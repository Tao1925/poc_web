package com.example.quiz;

import com.example.quiz.model.User;
import com.example.quiz.repository.UserRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 集成测试类 - 测试完整的Web应用功能
 */
@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.data-sync.enabled=false"
})
@Transactional
class QuizLoginDemoApplicationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 确保测试用户存在
        User adminUser = new User("admin", "123456");
        userRepository.save(adminUser);
    }

    @Test
    void testLoginPageAccess() throws Exception {
        // 测试登录页面访问
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(containsString("用户登录")))
                .andExpect(content().string(containsString("用户名")))
                .andExpect(content().string(containsString("密码")));
    }

    @Test
    void testLoginSuccess() throws Exception {
        // 测试登录成功场景
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "admin")
                .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/grading?username=admin"));
    }

    @Test
    void testLoginFailure_WrongPassword() throws Exception {
        // 测试密码错误场景
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "admin")
                .param("password", "wrongpassword"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(containsString("密码错误")));
    }

    @Test
    void testLoginFailure_UserNotFound() throws Exception {
        // 测试用户不存在场景
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "nonexistent")
                .param("password", "123456"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(content().string(containsString("用户名不存在")));
    }

    @Test
    void testWelcomePage() throws Exception {
        // 测试欢迎页面访问
        mockMvc.perform(get("/welcome")
                .param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("welcome"))
                .andExpect(content().string(containsString("登录成功")))
                .andExpect(content().string(containsString("admin")));
    }

    @Test
    void testUserTableInitialization() {
        // 测试用户表初始化
        // 验证admin用户是否存在
        assertTrue(userRepository.findByUsername("admin").isPresent());
        
        User adminUser = userRepository.findByUsername("admin").get();
        assertEquals("admin", adminUser.getUsername());
        assertEquals("123456", adminUser.getPassword());
    }

    @Test
    void testCompleteLoginFlow() throws Exception {
        // 测试完整的登录流程
        
        // 1. 访问登录页面
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));

        // 2. 提交登录表单
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "admin")
                .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/grading?username=admin"));

        // 3. 访问欢迎页面
        mockMvc.perform(get("/welcome")
                .param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("welcome"))
                .andExpect(content().string(containsString("欢迎回来")));
    }
}
