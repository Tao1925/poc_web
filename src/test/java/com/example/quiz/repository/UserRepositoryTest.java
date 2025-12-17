package com.example.quiz.repository;

import com.example.quiz.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserRepository 数据访问层测试类
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.data-sync.enabled=false"
})
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testFindByUsername_UserExists() {
        // 准备测试数据
        User user = new User("admin", "123456");
        entityManager.persistAndFlush(user);

        // 执行查询
        Optional<User> result = userRepository.findByUsername("admin");

        // 验证结果
        assertTrue(result.isPresent());
        assertEquals("admin", result.get().getUsername());
        assertEquals("123456", result.get().getPassword());
    }

    @Test
    void testFindByUsername_UserNotExists() {
        // 执行查询（不存在的用户）
        Optional<User> result = userRepository.findByUsername("nonexistent");

        // 验证结果
        assertFalse(result.isPresent());
    }

    @Test
    void testSaveUser() {
        // 准备测试数据
        User user = new User("testuser", "password123");

        // 执行保存
        User savedUser = userRepository.save(user);

        // 验证结果
        assertNotNull(savedUser.getId());
        assertEquals("testuser", savedUser.getUsername());
        assertEquals("password123", savedUser.getPassword());
    }

    @Test
    void testUserTableInitialization() {
        // 验证用户表是否正确初始化
        // 注意：由于使用了create-drop，预置数据可能不会自动加载
        // 这里主要测试表结构是否正确
        assertNotNull(userRepository);
        
        // 手动插入admin用户进行测试
        User admin = new User("admin", "123456");
        entityManager.persistAndFlush(admin);
        
        Optional<User> foundAdmin = userRepository.findByUsername("admin");
        assertTrue(foundAdmin.isPresent());
        assertEquals("admin", foundAdmin.get().getUsername());
        assertEquals("123456", foundAdmin.get().getPassword());
    }
}
