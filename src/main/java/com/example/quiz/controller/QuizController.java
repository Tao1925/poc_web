package com.example.quiz.controller;

import com.example.quiz.model.*;
import com.example.quiz.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class QuizController {
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private AnswerRepository answerRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/quiz")
    public String quizPage(@RequestParam String username, Model model) {
        // 获取所有章节和题目
        List<Chapter> chapters = chapterRepository.findAllOrderBySortOrder();
        List<Question> questions = questionRepository.findAllOrderByChapterAndSortOrder();
        
        // 获取用户信息
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            model.addAttribute("user", user);
            model.addAttribute("username", username);
        }
        
        model.addAttribute("chapters", chapters);
        model.addAttribute("questions", questions);
        
        return "quiz";
    }
    
    @GetMapping("/quiz/question/{questionId}")
    @ResponseBody
    public ResponseEntity<Question> getQuestion(@PathVariable Long questionId, @RequestParam String username) {
        Optional<Question> questionOptional = questionRepository.findById(questionId);
        if (questionOptional.isPresent()) {
            return ResponseEntity.ok(questionOptional.get());
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/quiz/answer/{questionId}")
    @ResponseBody
    public ResponseEntity<Answer> getAnswer(@PathVariable Long questionId, @RequestParam String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            Optional<Answer> answerOptional = answerRepository.findByQuestionIdAndUserId(questionId, userOptional.get().getId());
            if (answerOptional.isPresent()) {
                return ResponseEntity.ok(answerOptional.get());
            }
        }
        return ResponseEntity.ok(new Answer()); // 返回空答案
    }
    
    @PostMapping("/quiz/save")
    @ResponseBody
    public ResponseEntity<String> saveAnswer(@RequestParam Long questionId, 
                                           @RequestParam String content, 
                                           @RequestParam String username) {
        try {
            Optional<User> userOptional = userRepository.findByUsername(username);
            Optional<Question> questionOptional = questionRepository.findById(questionId);
            
            if (userOptional.isPresent() && questionOptional.isPresent()) {
                User user = userOptional.get();
                Question question = questionOptional.get();
                
                // 查找现有答案
                Optional<Answer> existingAnswerOptional = answerRepository.findByQuestionAndUser(question, user);
                
                Answer answer;
                if (existingAnswerOptional.isPresent()) {
                    // 更新现有答案
                    answer = existingAnswerOptional.get();
                    answer.setContent(content);
                    answer.setUpdatedAt(LocalDateTime.now());
                } else {
                    // 创建新答案
                    answer = new Answer(content, question, user);
                }
                
                answerRepository.save(answer);
                return ResponseEntity.ok("保存成功");
            }
            
            return ResponseEntity.badRequest().body("用户或题目不存在");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("保存失败: " + e.getMessage());
        }
    }
}
