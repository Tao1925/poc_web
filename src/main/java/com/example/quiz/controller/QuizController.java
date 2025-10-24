package com.example.quiz.controller;

import com.example.quiz.dto.AnswerDTO;
import com.example.quiz.dto.QuestionDTO;
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
    public ResponseEntity<QuestionDTO> getQuestion(@PathVariable Long questionId, @RequestParam String username) {
        Optional<Question> questionOptional = questionRepository.findById(questionId);
        if (questionOptional.isPresent()) {
            Question question = questionOptional.get();
            QuestionDTO questionDTO = new QuestionDTO(
                question.getId(),
                question.getTitle(),
                question.getDescription(),
                question.getQuestionNumber(),
                question.getSortOrder(),
                question.getChapter().getId(),
                question.getChapter().getTitle()
            );
            return ResponseEntity.ok(questionDTO);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/quiz/answer/{questionId}")
    @ResponseBody
    public ResponseEntity<AnswerDTO> getAnswer(@PathVariable Long questionId, @RequestParam String username) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            Optional<Answer> answerOptional = answerRepository.findByQuestionIdAndUserId(questionId, userOptional.get().getId());
            if (answerOptional.isPresent()) {
                Answer answer = answerOptional.get();
                AnswerDTO answerDTO = new AnswerDTO(
                    answer.getId(),
                    answer.getContent(),
                    answer.getCreatedAt(),
                    answer.getUpdatedAt(),
                    answer.getQuestion().getId(),
                    answer.getUser().getId(),
                    username
                );
                return ResponseEntity.ok(answerDTO);
            }
        }
        // 返回空答案DTO
        return ResponseEntity.ok(new AnswerDTO());
    }
    
    @PostMapping("/quiz/save")
    @ResponseBody
    public ResponseEntity<String> saveAnswer(@RequestParam Long questionId, 
                                           @RequestParam String content, 
                                           @RequestParam String username) {
        try {
            // 验证用户是否存在
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (!userOptional.isPresent()) {
                return ResponseEntity.badRequest().body("用户不存在: " + username);
            }
            
            // 验证题目是否存在
            Optional<Question> questionOptional = questionRepository.findById(questionId);
            if (!questionOptional.isPresent()) {
                return ResponseEntity.badRequest().body("题目不存在: " + questionId);
            }
            
            User user = userOptional.get();
            Question question = questionOptional.get();
            
            // 查找现有答案
            Optional<Answer> existingAnswerOptional = answerRepository.findByQuestionAndUser(question, user);
            
            Answer answer;
            boolean isUpdate = existingAnswerOptional.isPresent();
            
            if (isUpdate) {
                // 更新现有答案
                answer = existingAnswerOptional.get();
                answer.setContent(content);
                answer.setUpdatedAt(LocalDateTime.now());
            } else {
                // 创建新答案
                answer = new Answer(content, question, user);
            }
            
            answerRepository.save(answer);
            
            String message = isUpdate ? "答案更新成功" : "答案保存成功";
            return ResponseEntity.ok(message);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("保存失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/quiz/stats/{username}")
    @ResponseBody
    public ResponseEntity<String> getUserStats(@PathVariable String username) {
        try {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (!userOptional.isPresent()) {
                return ResponseEntity.badRequest().body("用户不存在: " + username);
            }
            
            User user = userOptional.get();
            List<Answer> answers = answerRepository.findByUser(user);
            
            int totalAnswers = answers.size();
            int answeredQuestions = (int) answers.stream()
                .filter(answer -> answer.getContent() != null && !answer.getContent().trim().isEmpty())
                .count();
            
            String stats = String.format("用户 %s 的答题统计：总共答题 %d 道，有效答案 %d 道", 
                username, totalAnswers, answeredQuestions);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("获取统计信息失败: " + e.getMessage());
        }
    }
}
