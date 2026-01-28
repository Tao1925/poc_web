package com.example.quiz.controller;

import com.example.quiz.model.*;
import com.example.quiz.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.*;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(GradingController.class);
    
    @GetMapping("/grading")
    public String gradingPage(@RequestParam String username, Model model) {
        // 验证是否为admin用户
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (!userOptional.isPresent() || !"admin".equals(username)) {
            LOGGER.warn("Unauthorized grading page access. username={}", username);
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
            LOGGER.warn("Grading answers requested for missing question. questionId={}", questionId);
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
                LOGGER.warn("Update score failed: answer not found. answerId={}", answerId);
                return ResponseEntity.badRequest().body("答案不存在");
            }
            
            Answer answer = answerOptional.get();
            Double totalScore = answer.getQuestion().getTotalScore();
            
            // 验证分数不能超过总分
            if (totalScore != null && score > totalScore) {
                LOGGER.warn("Update score failed: score exceeds total. answerId={}, score={}, totalScore={}",
                        answerId, score, totalScore);
                return ResponseEntity.badRequest().body("得分不能超过总分: " + totalScore);
            }
            
            answer.setScore(score);
            answer.setRemark(remark);  // 更新备注
            answerRepository.save(answer);
            LOGGER.info("Score updated. answerId={}, score={}, totalScore={}, remarkLen={}",
                    answerId, score, totalScore, remark != null ? remark.length() : 0);
            
            return ResponseEntity.ok("评分成功");
        } catch (Exception e) {
            LOGGER.error("Update score error. answerId={}, score={}", answerId, score, e);
            return ResponseEntity.internalServerError().body("评分失败: " + e.getMessage());
        }
    }

    @GetMapping("/grading/export")
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> export(@RequestParam String username) {
        Optional<User> u = userRepository.findByUsername(username);
        if (!u.isPresent() || !"admin".equals(username)) {
            LOGGER.warn("Unauthorized export request. username={}", username);
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
                LOGGER.info("Export answers start. users={}, questions={}", users.size(), questions.size());
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
                LOGGER.info("Export answers done. users={}", users.size());
            } catch (Exception e) {
                LOGGER.error("Export answers error", e);
                throw e;
            }
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fname, StandardCharsets.UTF_8))
                .body(body);
    }

    @GetMapping("/grading/exportScores")
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> exportScores(@RequestParam String username) {
        Optional<User> u = userRepository.findByUsername(username);
        if (!u.isPresent() || !"admin".equals(username)) {
            LOGGER.warn("Unauthorized exportScores request. username={}", username);
            return ResponseEntity.status(403).body(null);
        }

        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyMMdd");
        String fname = "poc_score_" + LocalDate.now().format(df) + ".csv";

        StreamingResponseBody body = (OutputStream os) -> {
            try {
                // Write BOM for Excel compatibility
                os.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});

                StringBuilder sb = new StringBuilder();

                // 1. Get Users (exclude admin, sort by ID to match data.json order)
                List<User> users = userRepository.findAll().stream()
                        .filter(user -> user.getUsername() != null && !"admin".equals(user.getUsername()))
                        .sorted(Comparator.comparing(User::getId))
                        .collect(Collectors.toList());

                // 2. Get Questions
                List<Question> questions = questionRepository.findAllOrderByChapterAndSortOrder();
                LOGGER.info("Export scores start. users={}, questions={}", users.size(), questions.size());

                // 3. Header Row: empty, total, user1, user2...
                sb.append(",total");
                for (User user : users) {
                    sb.append(",").append(escapeCsv(user.getUsername()));
                }
                sb.append("\n");

                // 4. Data Rows
                for (Question q : questions) {
                    sb.append(escapeCsv(q.getTitle()));
                    sb.append(",").append(q.getTotalScore() != null ? (int) q.getTotalScore().doubleValue() : 0);

                    for (User user : users) {
                        sb.append(",");
                        Optional<Answer> ansOpt = answerRepository.findByQuestionIdAndUserId(q.getId(), user.getId());
                        int score = 0;
                        if (ansOpt.isPresent() && ansOpt.get().getScore() != null) {
                            score = (int) ansOpt.get().getScore().doubleValue();
                        }
                        sb.append(score);
                    }
                    sb.append("\n");
                }

                os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                LOGGER.info("Export scores done. users={}", users.size());
            } catch (Exception e) {
                LOGGER.error("Export scores error", e);
                throw e;
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(fname, StandardCharsets.UTF_8))
                .body(body);
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
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
