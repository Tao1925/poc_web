package com.example.quiz.controller;

import com.example.quiz.model.*;
import com.example.quiz.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.List;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
public class GradingController {
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private AnswerRepository answerRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/grading")
    public String gradingPage(@RequestParam String username, Model model) {
        // 验证是否为admin用户
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (!userOptional.isPresent() || !"admin".equals(username)) {
            return "redirect:/login";
        }
        
        // 获取所有章节和题目
        List<Chapter> chapters = chapterRepository.findAllOrderBySortOrder();
        List<Question> questions = questionRepository.findAllOrderByChapterAndSortOrder();
        
        model.addAttribute("user", userOptional.get());
        model.addAttribute("username", username);
        model.addAttribute("chapters", chapters);
        model.addAttribute("questions", questions);
        
        return "grading";
    }
    
    @GetMapping("/grading/question/{questionId}/answers")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getQuestionAnswers(@PathVariable Long questionId) {
        Optional<Question> questionOptional = questionRepository.findById(questionId);
        if (!questionOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Question question = questionOptional.get();
        List<Answer> answers = answerRepository.findByQuestion(question);
        
        // 过滤掉admin用户的答案，只显示学生的答案
        List<Map<String, Object>> result = answers.stream()
            .filter(answer -> !"admin".equals(answer.getUser().getUsername()))
            .filter(answer -> answer.getContent() != null && !answer.getContent().trim().isEmpty())
            .map(answer -> {
                Map<String, Object> map = new HashMap<>();
                map.put("answerId", answer.getId());
                map.put("username", answer.getUser().getUsername());
                map.put("content", answer.getContent());
                map.put("score", answer.getScore());
                map.put("remark", answer.getRemark());  // 添加备注字段
                map.put("totalScore", question.getTotalScore());
                map.put("createdAt", answer.getCreatedAt());
                map.put("updatedAt", answer.getUpdatedAt());
                return map;
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/grading/updateScore")
    @ResponseBody
    public ResponseEntity<String> updateScore(@RequestParam Long answerId, 
                                             @RequestParam Double score,
                                             @RequestParam(required = false) String remark) {
        try {
            Optional<Answer> answerOptional = answerRepository.findById(answerId);
            if (!answerOptional.isPresent()) {
                return ResponseEntity.badRequest().body("答案不存在");
            }
            
            Answer answer = answerOptional.get();
            Double totalScore = answer.getQuestion().getTotalScore();
            
            // 验证分数不能超过总分
            if (totalScore != null && score > totalScore) {
                return ResponseEntity.badRequest().body("得分不能超过总分: " + totalScore);
            }
            
            answer.setScore(score);
            answer.setRemark(remark);  // 更新备注
            answerRepository.save(answer);
            
            return ResponseEntity.ok("评分成功");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("评分失败: " + e.getMessage());
        }
    }

    @GetMapping("/grading/export")
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> export(@RequestParam String username) {
        Optional<User> u = userRepository.findByUsername(username);
        if (!u.isPresent() || !"admin".equals(username)) {
            return ResponseEntity.status(403).body(null);
        }
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyMMdd");
        String fname = "poc_answer_" + LocalDate.now().format(df) + ".zip";
        StreamingResponseBody body = (OutputStream os) -> {
            try (ZipOutputStream zos = new ZipOutputStream(os)) {
                List<User> users = userRepository.findAll().stream()
                        .filter(user -> user.getUsername() != null && !"admin".equals(user.getUsername()))
                        .collect(Collectors.toList());
                List<Question> questions = questionRepository.findAllOrderByChapterAndSortOrder();
                for (User user : users) {
                    String safe = sanitizeFilename(user.getUsername()) + ".html";
                    zos.putNextEntry(new ZipEntry(safe));
                    StringBuilder sb = new StringBuilder();
                    sb.append("<!DOCTYPE html><html lang=\"zh-CN\"><head><meta charset=\"UTF-8\"><title>")
                      .append(escapeHtml(user.getUsername()))
                      .append("</title></head><body><h1>")
                      .append(escapeHtml(user.getUsername()))
                      .append("</h1>");
                    for (Question q : questions) {
                        Optional<Answer> ansOpt = answerRepository.findByQuestionIdAndUserId(q.getId(), user.getId());
                        if (ansOpt.isPresent()) {
                            Answer a = ansOpt.get();
                            String c = a.getContent();
                            if (c != null && !c.trim().isEmpty()) {
                                sb.append("<section>")
                                  .append("<h2>")
                                  .append(escapeHtml(q.getQuestionNumber()))
                                  .append("</h2>")
                                  .append("<div>")
                                  .append(escapeHtml(q.getDescription()))
                                  .append("</div>")
                                  .append("<div>")
                                  .append(c)
                                  .append("</div>")
                                  .append("</section>");
                            }
                        }
                    }
                    sb.append("</body></html>");
                    byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
                    zos.write(bytes);
                    zos.closeEntry();
                }
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fname, StandardCharsets.UTF_8))
                .body(body);
    }

    private String sanitizeFilename(String s) {
        StringBuilder r = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (Character.isLetterOrDigit(ch) || ch == '-' || ch == '_' || ch == '.') {
                r.append(ch);
            } else {
                r.append('_');
            }
        }
        return r.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        StringBuilder r = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '&': r.append("&amp;"); break;
                case '<': r.append("&lt;"); break;
                case '>': r.append("&gt;"); break;
                case '"': r.append("&quot;"); break;
                case '\'': r.append("&#39;"); break;
                default: r.append(c);
            }
        }
        return r.toString();
    }
}
