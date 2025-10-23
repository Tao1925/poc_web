package com.example.quiz.controller;

import com.example.quiz.model.User;
import com.example.quiz.repository.UserRepository;
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
                // 登录成功，重定向到答题页面
                return "redirect:/quiz?username=" + username;
            } else {
                // 密码错误
                model.addAttribute("error", "密码错误");
                return "login";
            }
        } else {
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
