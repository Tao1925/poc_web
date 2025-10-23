package com.example.quiz.repository;

import com.example.quiz.model.Answer;
import com.example.quiz.model.Question;
import com.example.quiz.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    
    Optional<Answer> findByQuestionAndUser(Question question, User user);
    
    @Query("SELECT a FROM Answer a WHERE a.question.id = :questionId AND a.user.id = :userId")
    Optional<Answer> findByQuestionIdAndUserId(@Param("questionId") Long questionId, @Param("userId") Long userId);
}
