package com.example.quiz.controller;

import com.example.quiz.model.*;
import com.example.quiz.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

/**
 * 答题内容持久化存储测试
 */
@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.data-sync.enabled=false"
})
@Transactional
class AnswerPersistenceTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private User testUser1;
    private User testUser2;
    private Question testQuestion;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 创建测试用户
        testUser1 = new User("user1", "password1");
        testUser2 = new User("user2", "password2");
        userRepository.save(testUser1);
        userRepository.save(testUser2);
        
        // 创建测试章节和题目
        Chapter chapter = new Chapter("测试章节", "测试描述", 1);
        chapterRepository.save(chapter);
        
        testQuestion = new Question("测试题目", "测试描述", "1.1", 1, chapter);
        questionRepository.save(testQuestion);
    }

    @Test
    void testSaveAnswer() throws Exception {
        String answerContent = "<p>这是我的测试答案</p>";
        
        // 用户1保存答案
        mockMvc.perform(post("/quiz/save")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("questionId", testQuestion.getId().toString())
                .param("content", answerContent)
                .param("username", "user1"))
                .andExpect(status().isOk())
                .andExpect(content().string("答案保存成功"));
        
        // 验证答案已保存
        List<Answer> answers = answerRepository.findByUser(testUser1);
        assertEquals(1, answers.size());
        assertEquals(answerContent, answers.get(0).getContent());
        assertEquals(testQuestion.getId(), answers.get(0).getQuestion().getId());
    }

    @Test
    void testUpdateAnswer() throws Exception {
        String initialContent = "<p>初始答案</p>";
        String updatedContent = "<p>更新后的答案</p>";
        
        // 先保存初始答案
        mockMvc.perform(post("/quiz/save")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("questionId", testQuestion.getId().toString())
                .param("content", initialContent)
                .param("username", "user1"))
                .andExpect(status().isOk());
        
        // 更新答案
        mockMvc.perform(post("/quiz/save")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("questionId", testQuestion.getId().toString())
                .param("content", updatedContent)
                .param("username", "user1"))
                .andExpect(status().isOk())
                .andExpect(content().string("答案更新成功"));
        
        // 验证答案已更新
        List<Answer> answers = answerRepository.findByUser(testUser1);
        assertEquals(1, answers.size());
        assertEquals(updatedContent, answers.get(0).getContent());
    }

    @Test
    void testLoadAnswer() throws Exception {
        String answerContent = "<p>用户1的答案</p>";
        
        // 保存答案
        Answer answer = new Answer(answerContent, testQuestion, testUser1);
        answerRepository.save(answer);
        
        // 加载答案
        mockMvc.perform(get("/quiz/answer/" + testQuestion.getId())
                .param("username", "user1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").value(answerContent))
                .andExpect(jsonPath("$.questionId").value(testQuestion.getId()))
                .andExpect(jsonPath("$.userId").value(testUser1.getId()))
                .andExpect(jsonPath("$.username").value("user1"));
    }

    @Test
    void testLoadEmptyAnswer() throws Exception {
        // 加载不存在的答案
        mockMvc.perform(get("/quiz/answer/" + testQuestion.getId())
                .param("username", "user1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void testMultiUserAnswers() throws Exception {
        String user1Answer = "<p>用户1的答案</p>";
        String user2Answer = "<p>用户2的答案</p>";
        
        // 用户1保存答案
        mockMvc.perform(post("/quiz/save")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("questionId", testQuestion.getId().toString())
                .param("content", user1Answer)
                .param("username", "user1"))
                .andExpect(status().isOk());
        
        // 用户2保存答案
        mockMvc.perform(post("/quiz/save")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("questionId", testQuestion.getId().toString())
                .param("content", user2Answer)
                .param("username", "user2"))
                .andExpect(status().isOk());
        
        // 验证两个用户的答案都保存了
        List<Answer> user1Answers = answerRepository.findByUser(testUser1);
        List<Answer> user2Answers = answerRepository.findByUser(testUser2);
        
        assertEquals(1, user1Answers.size());
        assertEquals(1, user2Answers.size());
        assertEquals(user1Answer, user1Answers.get(0).getContent());
        assertEquals(user2Answer, user2Answers.get(0).getContent());
        
        // 验证用户1只能看到自己的答案
        mockMvc.perform(get("/quiz/answer/" + testQuestion.getId())
                .param("username", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(user1Answer));
        
        // 验证用户2只能看到自己的答案
        mockMvc.perform(get("/quiz/answer/" + testQuestion.getId())
                .param("username", "user2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(user2Answer));
    }

    @Test
    void testUserStats() throws Exception {
        // 创建多个答案
        Answer answer1 = new Answer("<p>答案1</p>", testQuestion, testUser1);
        Answer answer2 = new Answer("<p>答案2</p>", testQuestion, testUser1);
        answerRepository.save(answer1);
        answerRepository.save(answer2);
        
        // 获取用户统计
        mockMvc.perform(get("/quiz/stats/user1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("用户 user1 的答题统计")))
                .andExpect(content().string(containsString("总共答题 2 道")))
                .andExpect(content().string(containsString("有效答案 2 道")));
    }

    @Test
    void testSaveAnswerWithInvalidUser() throws Exception {
        mockMvc.perform(post("/quiz/save")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("questionId", testQuestion.getId().toString())
                .param("content", "<p>测试答案</p>")
                .param("username", "nonexistent"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("用户不存在: nonexistent"));
    }

    @Test
    void testSaveAnswerWithInvalidQuestion() throws Exception {
        mockMvc.perform(post("/quiz/save")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("questionId", "999")
                .param("content", "<p>测试答案</p>")
                .param("username", "user1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("题目不存在: 999"));
    }

    @Test
    void testAnswerPersistenceAfterRestart() throws Exception {
        String answerContent = "<p>持久化测试答案</p>";
        
        // 保存答案
        mockMvc.perform(post("/quiz/save")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("questionId", testQuestion.getId().toString())
                .param("content", answerContent)
                .param("username", "user1"))
                .andExpect(status().isOk());
        
        // 模拟应用重启 - 重新查询数据库
        List<Answer> answers = answerRepository.findByUser(testUser1);
        assertEquals(1, answers.size());
        assertEquals(answerContent, answers.get(0).getContent());
        
        // 验证可以重新加载答案
        mockMvc.perform(get("/quiz/answer/" + testQuestion.getId())
                .param("username", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(answerContent));
    }
}
