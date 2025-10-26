package com.example.quiz.controller;

import com.example.quiz.model.*;
import com.example.quiz.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

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
}

