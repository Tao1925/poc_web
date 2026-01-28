package com.example.quiz.controller;

import com.example.quiz.model.User;
import com.example.quiz.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class LoginController {
    
    @Autowired
    private UserRepository userRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);
    
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
    
    @PostMapping("/login")
    public String login(@RequestParam String username, 
                       @RequestParam String password, 
                       Model model) {
        
        // 查找用户
        Optional<User> userOptional = userRepository.findByUsername(username);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // 验证密码（这里简单比较，实际项目中应该使用加密）
            if (password.equals(user.getPassword())) {
                LOGGER.info("Login success. username={}", username);
                // 登录成功，根据用户类型跳转不同页面
                if ("admin".equals(username)) {
                    // admin用户跳转到判题界面
                    return "redirect:/grading?username=" + username;
                } else {
                    // 普通用户跳转到答题页面
                    return "redirect:/quiz?username=" + username;
                }
            } else {
                LOGGER.warn("Login failed: invalid password. username={}", username);
                // 密码错误
                model.addAttribute("error", "密码错误");
                return "login";
            }
        } else {
            LOGGER.warn("Login failed: user not found. username={}", username);
            // 用户不存在
            model.addAttribute("error", "用户名不存在");
            return "login";
        }
    }
    
    @GetMapping("/welcome")
    public String welcomePage(@RequestParam String username, Model model) {
        model.addAttribute("username", username);
        return "welcome";
    }
}
