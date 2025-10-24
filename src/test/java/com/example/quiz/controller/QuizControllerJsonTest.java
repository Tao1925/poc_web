package com.example.quiz.controller;

import com.example.quiz.dto.QuestionDTO;
import com.example.quiz.model.*;
import com.example.quiz.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * QuizController JSON序列化测试
 */
@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class QuizControllerJsonTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 创建测试数据
        Chapter chapter = new Chapter("测试章节", "测试描述", 1);
        chapterRepository.save(chapter);
        
        Question question = new Question("测试题目", "测试描述", "1.1", 1, chapter);
        questionRepository.save(question);
        
        User user = new User("testuser", "password");
        userRepository.save(user);
    }

    @Test
    void testGetQuestionJsonSerialization() throws Exception {
        // 获取实际创建的题目ID
        List<Question> questions = questionRepository.findAll();
        assertFalse(questions.isEmpty());
        Long questionId = questions.get(0).getId();
        
        // 测试题目JSON序列化是否正常
        mockMvc.perform(get("/quiz/question/" + questionId)
                .param("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.id").value(questionId))
                .andExpect(jsonPath("$.title").value("测试题目"))
                .andExpect(jsonPath("$.description").value("测试描述"))
                .andExpect(jsonPath("$.questionNumber").value("1.1"))
                .andExpect(jsonPath("$.sortOrder").value(1))
                .andExpect(jsonPath("$.chapterId").exists())
                .andExpect(jsonPath("$.chapterTitle").value("测试章节"));
    }

    @Test
    void testGetQuestionNotFound() throws Exception {
        // 测试不存在的题目
        mockMvc.perform(get("/quiz/question/999")
                .param("username", "testuser"))
                .andExpect(status().isNotFound());
    }
}
