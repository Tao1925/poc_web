package com.example.quiz.controller;

import com.example.quiz.model.User;
import com.example.quiz.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * LoginController 单元测试类
 */
@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Model model;

    @InjectMocks
    private LoginController loginController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("admin", "123456");
        testUser.setId(1L);
    }

    @Test
    void testLoginPage() {
        // 测试登录页面访问
        String result = loginController.loginPage();
        assertEquals("login", result);
    }

    @Test
    void testLoginSuccess() {
        // 准备测试数据
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

        // 执行登录
        String result = loginController.login("admin", "123456", model);

        // 验证结果
        assertEquals("redirect:/quiz?username=admin", result);
        verify(userRepository).findByUsername("admin");
        verify(model, never()).addAttribute(anyString(), any());
    }

    @Test
    void testLoginFailure_WrongPassword() {
        // 准备测试数据
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testUser));

        // 执行登录（错误密码）
        String result = loginController.login("admin", "wrongpassword", model);

        // 验证结果
        assertEquals("login", result);
        verify(userRepository).findByUsername("admin");
        verify(model).addAttribute("error", "密码错误");
    }

    @Test
    void testLoginFailure_UserNotFound() {
        // 准备测试数据
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // 执行登录（不存在的用户）
        String result = loginController.login("nonexistent", "123456", model);

        // 验证结果
        assertEquals("login", result);
        verify(userRepository).findByUsername("nonexistent");
        verify(model).addAttribute("error", "用户名不存在");
    }

    @Test
    void testWelcomePage() {
        // 执行欢迎页面访问
        String result = loginController.welcomePage("admin", model);

        // 验证结果
        assertEquals("welcome", result);
        verify(model).addAttribute("username", "admin");
    }
}
