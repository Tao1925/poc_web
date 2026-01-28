package com.example.quiz.controller;

import com.example.quiz.dto.AnswerDTO;
import com.example.quiz.dto.QuestionDTO;
import com.example.quiz.model.*;
import com.example.quiz.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final String DEBUG_LOG_PATH = java.nio.file.Paths.get("data", "debug.log").toString();
    private static final ObjectMapper DEBUG_MAPPER = new ObjectMapper();
    private static final Logger LOGGER = LoggerFactory.getLogger(QuizController.class);

    private void debugLog(String hypothesisId, String location, String message, Map<String, Object> data) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("sessionId", "debug-session");
            payload.put("runId", "pre-fix");
            payload.put("hypothesisId", hypothesisId);
            payload.put("location", location);
            payload.put("message", message);
            payload.put("data", data);
            payload.put("timestamp", System.currentTimeMillis());
            String json = DEBUG_MAPPER.writeValueAsString(payload);
            Path logPath = Paths.get(DEBUG_LOG_PATH);
            if (logPath.getParent() != null) {
                Files.createDirectories(logPath.getParent());
            }
            Files.write(logPath, (json + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }
    
    @GetMapping("/quiz")
    public String quizPage(@RequestParam String username, Model model) {
        // #region agent log
        debugLog("H4", "QuizController.quizPage:entry", "quizPageLoaded", new HashMap<>(Map.of(
                "username", username
        )));
        // #endregion
        // 获取所有章节和题目
        List<Chapter> chapters = chapterRepository.findAllOrderBySortOrder();
        List<Question> questions = questionRepository.findAllOrderByChapterAndSortOrder();

        // #region agent log
        Map<String, List<Long>> duplicateTitleIds = questions.stream()
                .filter(q -> q.getTitle() != null && !q.getTitle().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(
                        Question::getTitle,
                        java.util.stream.Collectors.mapping(Question::getId, java.util.stream.Collectors.toList())
                ));
        Map<String, List<Long>> duplicatesOnly = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : duplicateTitleIds.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicatesOnly.put(entry.getKey(), entry.getValue());
            }
        }
        if (!duplicatesOnly.isEmpty()) {
            debugLog("H1", "QuizController.quizPage:duplicates", "duplicateQuestionTitles", new HashMap<>(Map.of(
                    "duplicateTitlesCount", duplicatesOnly.size(),
                    "sampleTitles", duplicatesOnly.keySet().stream().limit(5).toList()
            )));
        }
        // #endregion
        
        // 获取用户信息
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            model.addAttribute("user", user);
            model.addAttribute("username", username);
        } else {
            LOGGER.warn("Quiz page requested with unknown user: {}", username);
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
        LOGGER.warn("Question not found. questionId={}, username={}", questionId, username);
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/quiz/answer/{questionId}")
    @ResponseBody
    public ResponseEntity<AnswerDTO> getAnswer(@PathVariable Long questionId, @RequestParam String username) {
        // #region agent log
        debugLog("H2", "QuizController.getAnswer:entry", "getAnswerById", new HashMap<>(Map.of(
                "questionId", questionId,
                "username", username
        )));
        // #endregion
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            Optional<Answer> answerOptional = answerRepository.findByQuestionIdAndUserId(questionId, userOptional.get().getId());
            if (answerOptional.isPresent()) {
                Answer answer = answerOptional.get();
                // #region agent log
                debugLog("H2", "QuizController.getAnswer:found", "answerFoundById", new HashMap<>(Map.of(
                        "questionId", questionId,
                        "answerId", answer.getId(),
                        "answerQuestionId", answer.getQuestion().getId()
                )));
                // #endregion
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
        } else {
            LOGGER.warn("Answer query with unknown user. questionId={}, username={}", questionId, username);
        }
        // 返回空答案DTO
        return ResponseEntity.ok(new AnswerDTO());
    }

    @GetMapping("/quiz/answer")
    @ResponseBody
    public ResponseEntity<AnswerDTO> getAnswerByTitle(@RequestParam String title, @RequestParam String username) {
        // #region agent log
        debugLog("H1", "QuizController.getAnswerByTitle:entry", "getAnswerByTitle", new HashMap<>(Map.of(
                "title", title,
                "username", username
        )));
        // #endregion
        // #region agent log
        List<Question> questionsByTitle = questionRepository.findAllByTitle(title);
        if (questionsByTitle.size() > 1) {
            debugLog("H1", "QuizController.getAnswerByTitle:duplicateTitle", "multipleQuestionsSameTitle", new HashMap<>(Map.of(
                    "title", title,
                    "questionIds", questionsByTitle.stream().map(Question::getId).toList()
            )));
        }
        // #endregion
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            Optional<Answer> answerOptional = answerRepository.findByQuestion_TitleAndUser(title, userOptional.get());
            if (answerOptional.isPresent()) {
                Answer answer = answerOptional.get();
                // #region agent log
                debugLog("H1", "QuizController.getAnswerByTitle:found", "answerFoundByTitle", new HashMap<>(Map.of(
                        "title", title,
                        "answerId", answer.getId(),
                        "answerQuestionId", answer.getQuestion().getId()
                )));
                // #endregion
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
        } else {
            LOGGER.warn("Answer query by title with unknown user. title={}, username={}", title, username);
        }
        return ResponseEntity.ok(new AnswerDTO());
    }
    
    @PostMapping("/quiz/save")
    @ResponseBody
    public ResponseEntity<String> saveAnswer(@RequestParam(required = false) Long questionId,
                                           @RequestParam(required = false) String questionTitle,
                                           @RequestParam String content, 
                                           @RequestParam String username) {
        try {
            // #region agent log
            debugLog("H3", "QuizController.saveAnswer:entry", "saveAnswerStart", new HashMap<>(Map.of(
                    "questionId", questionId,
                    "questionTitle", questionTitle,
                    "username", username,
                    "contentLen", content != null ? content.length() : 0
            )));
            // #endregion
            // 验证用户是否存在
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (!userOptional.isPresent()) {
                LOGGER.warn("Save answer failed: unknown user. questionId={}, questionTitle={}, username={}",
                        questionId, questionTitle, username);
                return ResponseEntity.badRequest().body("用户不存在: " + username);
            }

            if (questionId == null && questionTitle == null) {
                LOGGER.warn("Save answer failed: missing question identifiers. username={}", username);
                return ResponseEntity.badRequest().body("必须提供题目ID或题目名称");
            }
            
            // 验证题目是否存在
            Optional<Question> questionOptional;
            if (questionTitle != null && !questionTitle.isEmpty()) {
                questionOptional = questionRepository.findByTitle(questionTitle);
            } else {
                if (questionId == null) {
                    return ResponseEntity.badRequest().body("必须提供题目ID或题目名称");
                }
                questionOptional = questionRepository.findById(questionId);
            }

            if (!questionOptional.isPresent()) {
                LOGGER.warn("Save answer failed: question not found. questionId={}, questionTitle={}, username={}",
                        questionId, questionTitle, username);
                return ResponseEntity.badRequest().body("题目不存在: " + (questionTitle != null ? questionTitle : questionId));
            }
            
            User user = userOptional.get();
            Question question = questionOptional.get();
            // #region agent log
            debugLog("H1", "QuizController.saveAnswer:resolved", "resolvedQuestion", new HashMap<>(Map.of(
                    "resolvedQuestionId", question.getId(),
                    "resolvedQuestionTitle", question.getTitle(),
                    "inputQuestionId", questionId,
                    "inputQuestionTitle", questionTitle
            )));
            // #endregion
            
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
            LOGGER.info("Answer saved. answerId={}, questionId={}, questionTitle={}, username={}, isUpdate={}, contentLen={}",
                    answer.getId(), question.getId(), question.getTitle(), username, isUpdate,
                    content != null ? content.length() : 0);
            // #region agent log
            debugLog("H3", "QuizController.saveAnswer:done", "saveAnswerDone", new HashMap<>(Map.of(
                    "savedAnswerId", answer.getId(),
                    "savedQuestionId", answer.getQuestion().getId(),
                    "isUpdate", isUpdate
            )));
            // #endregion
            
            String message = isUpdate ? "答案更新成功" : "答案保存成功";
            return ResponseEntity.ok(message);
            
        } catch (Exception e) {
            LOGGER.error("Save answer error. questionId={}, questionTitle={}, username={}",
                    questionId, questionTitle, username, e);
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
