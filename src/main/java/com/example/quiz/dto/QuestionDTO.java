package com.example.quiz.dto;

/**
 * Question数据传输对象，用于JSON序列化
 * 避免循环引用问题
 */
public class QuestionDTO {
    private Long id;
    private String title;
    private String description;
    private String questionNumber;
    private Integer sortOrder;
    private Long chapterId;
    private String chapterTitle;
    
    // 默认构造函数
    public QuestionDTO() {}
    
    // 带参数的构造函数
    public QuestionDTO(Long id, String title, String description, String questionNumber, 
                      Integer sortOrder, Long chapterId, String chapterTitle) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.questionNumber = questionNumber;
        this.sortOrder = sortOrder;
        this.chapterId = chapterId;
        this.chapterTitle = chapterTitle;
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
    
    public Long getChapterId() {
        return chapterId;
    }
    
    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }
    
    public String getChapterTitle() {
        return chapterTitle;
    }
    
    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }
}
