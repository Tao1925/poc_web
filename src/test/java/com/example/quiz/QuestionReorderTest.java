package com.example.quiz;

import com.example.quiz.model.Answer;
import com.example.quiz.model.Question;
import com.example.quiz.model.User;
import com.example.quiz.repository.AnswerRepository;
import com.example.quiz.repository.QuestionRepository;
import com.example.quiz.repository.UserRepository;
import com.example.quiz.repository.ChapterRepository;
import com.example.quiz.model.Chapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class QuestionReorderTest {

    @Autowired
    private CommandLineRunner syncDataFromJson;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Test
    @Transactional
    public void testReorderPersistence() throws Exception {
        // Step 1: Sync V1
        System.setProperty("app.data-sync.location", "classpath:data_v1.json");
        syncDataFromJson.run();

        // Verify V1 state
        Optional<Question> qA = questionRepository.findByTitle("Question A");
        assertTrue(qA.isPresent(), "Question A should be present");
        assertEquals("1.1", qA.get().getQuestionNumber());

        System.out.println("Users in DB:");
        userRepository.findAll().forEach(u -> System.out.println("User: " + u.getUsername()));

        Optional<User> user = userRepository.findByUsername("user1");
        assertTrue(user.isPresent(), "User user1 should be present");

        // Save Answer for Question A
        Answer answer = new Answer("Answer A", qA.get(), user.get());
        answerRepository.save(answer);

        // Step 2: Sync V2 (Reorder)
        System.setProperty("app.data-sync.location", "classpath:data_v2.json");
        syncDataFromJson.run();

        // Verify V2 state
        Optional<Question> qA_v2 = questionRepository.findByTitle("Question A");
        assertTrue(qA_v2.isPresent());
        // Question A should now be 1.2
        assertEquals("1.2", qA_v2.get().getQuestionNumber());
        // ID should remain same (because we updated existing)
        assertEquals(qA.get().getId(), qA_v2.get().getId());

        // Verify Answer is still attached
        Optional<Answer> answer_v2 = answerRepository.findByQuestion_TitleAndUser("Question A", user.get());
        assertTrue(answer_v2.isPresent());
        assertEquals("Answer A", answer_v2.get().getContent());
        
        // Verify Question C
        Optional<Question> qC = questionRepository.findByTitle("Question C");
        assertTrue(qC.isPresent());
        assertEquals("1.1", qC.get().getQuestionNumber());
        
        // Verify Chapter Questions Order (The fix validation)
        List<Chapter> chapters = chapterRepository.findAllOrderBySortOrder();
        assertFalse(chapters.isEmpty());
        Chapter chapter = chapters.get(0);
        List<Question> questions = chapter.getQuestions();
        assertEquals(3, questions.size());
        
        // In data_v2.json:
        // Question C is 1.1 (Sort Order 1)
        // Question A is 1.2 (Sort Order 2)
        // Question B is 1.3 (Sort Order 3)
        assertEquals("Question C", questions.get(0).getTitle());
        assertEquals("Question A", questions.get(1).getTitle());
        assertEquals("Question B", questions.get(2).getTitle());
        
        // Clean up
        System.clearProperty("app.data-sync.location");
    }
}
