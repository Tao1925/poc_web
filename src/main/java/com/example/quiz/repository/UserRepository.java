package com.example.quiz.repository;

import com.example.quiz.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return Optional<User> 用户对象（如果存在）
     */
    Optional<User> findByUsername(String username);
}
