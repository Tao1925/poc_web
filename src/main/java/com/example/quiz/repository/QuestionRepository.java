package com.example.quiz.repository;

import com.example.quiz.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    @Query("SELECT q FROM Question q WHERE q.chapter.id = :chapterId ORDER BY q.sortOrder ASC")
    List<Question> findByChapterIdOrderBySortOrder(@Param("chapterId") Long chapterId);
    
    @Query("SELECT q FROM Question q ORDER BY q.chapter.sortOrder ASC, q.sortOrder ASC")
    List<Question> findAllOrderByChapterAndSortOrder();
}
