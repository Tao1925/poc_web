package com.example.quiz.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 2000)
    private String description;
    
    @Column(name = "question_number")
    private String questionNumber;
    
    @Column(name = "sort_order")
    private Integer sortOrder;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;
    
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Answer> answers;
    
    // 默认构造函数
    public Question() {}
    
    // 带参数的构造函数
    public Question(String title, String description, String questionNumber, Integer sortOrder, Chapter chapter) {
        this.title = title;
        this.description = description;
        this.questionNumber = questionNumber;
        this.sortOrder = sortOrder;
        this.chapter = chapter;
    }
    
    // Getter 和 Setter 方法
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getQuestionNumber() {
        return questionNumber;
    }
    
    public void setQuestionNumber(String questionNumber) {
        this.questionNumber = questionNumber;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    public Chapter getChapter() {
        return chapter;
    }
    
    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }
    
    public List<Answer> getAnswers() {
        return answers;
    }
    
    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }
    
    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", questionNumber='" + questionNumber + '\'' +
                ", sortOrder=" + sortOrder +
                '}';
    }
}
