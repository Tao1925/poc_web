# JSON序列化循环引用问题解决方案

## 问题描述

在 `quiz.html` 中执行 `loadQuestion` 时出现JSON解析错误：

```
加载题目失败: SyntaxError: Unexpected token '}', ...""chapter":}]}}]}}]}}"... is not valid JSON
```

## 问题原因分析

**根本原因：JPA实体类之间的循环引用导致JSON序列化失败**

### 循环引用关系图

```
Question (题目)
    ↓ @ManyToOne
Chapter (章节)
    ↓ @OneToMany  
List<Question> (题目列表)
    ↓ 回到 Question (形成循环)

Question (题目)
    ↓ @OneToMany
List<Answer> (答案列表)
    ↓ @ManyToOne
Question (题目) (形成循环)

Answer (答案)
    ↓ @ManyToOne
Question (题目)
    ↓ @OneToMany
List<Answer> (答案列表) (形成循环)
```

### 具体问题

1. **Question实体** 包含 `Chapter` 对象
2. **Chapter实体** 包含 `List<Question>` 对象
3. **Question实体** 包含 `List<Answer>` 对象
4. **Answer实体** 包含 `Question` 和 `User` 对象

当Spring Boot尝试将Question对象序列化为JSON时，Jackson会陷入无限循环，最终导致JSON格式错误。

## 解决方案

### 方案1：使用@JsonIgnore注解（已实现）

在实体类的关联字段上添加 `@JsonIgnore` 注解：

```java
// Question.java
@OneToMany(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
@JsonIgnore
private List<Answer> answers;

// Chapter.java  
@OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
@JsonIgnore
private List<Question> questions;

// Answer.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "question_id", nullable = false)
@JsonIgnore
private Question question;

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
@JsonIgnore
private User user;
```

### 方案2：使用DTO模式（推荐，已实现）

创建数据传输对象（DTO）来避免循环引用：

```java
// QuestionDTO.java
public class QuestionDTO {
    private Long id;
    private String title;
    private String description;
    private String questionNumber;
    private Integer sortOrder;
    private Long chapterId;
    private String chapterTitle;
    // 只包含必要的字段，避免循环引用
}
```

修改Controller使用DTO：

```java
@GetMapping("/quiz/question/{questionId}")
@ResponseBody
public ResponseEntity<QuestionDTO> getQuestion(@PathVariable Long questionId, @RequestParam String username) {
    Optional<Question> questionOptional = questionRepository.findById(questionId);
    if (questionOptional.isPresent()) {
        Question question = questionOptional.get();
        QuestionDTO questionDTO = new QuestionDTO(
            question.getId(),
            question.getTitle(),
            question.getDescription(),
            question.getQuestionNumber(),
            question.getSortOrder(),
            question.getChapter().getId(),
            question.getChapter().getTitle()
        );
        return ResponseEntity.ok(questionDTO);
    }
    return ResponseEntity.notFound().build();
}
```

## 测试验证

创建了专门的测试类 `QuizControllerJsonTest` 来验证修复效果：

```java
@Test
void testGetQuestionJsonSerialization() throws Exception {
    // 测试题目JSON序列化是否正常
    mockMvc.perform(get("/quiz/question/" + questionId)
            .param("username", "testuser"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.id").value(questionId))
            .andExpect(jsonPath("$.title").value("测试题目"))
            .andExpect(jsonPath("$.description").value("测试描述"))
            .andExpect(jsonPath("$.questionNumber").value("1.1"))
            .andExpect(jsonPath("$.sortOrder").value(1))
            .andExpect(jsonPath("$.chapterId").exists())
            .andExpect(jsonPath("$.chapterTitle").value("测试章节"));
}
```

**测试结果：** ✅ 通过

## 修复效果

### 修复前
- JSON序列化时出现循环引用
- 前端JavaScript解析JSON失败
- 错误信息：`SyntaxError: Unexpected token '}'`

### 修复后
- JSON序列化正常
- 前端可以正确解析JSON数据
- 题目加载功能正常工作

## 最佳实践建议

1. **优先使用DTO模式**：避免在实体类上添加@JsonIgnore注解
2. **明确API边界**：API只返回前端需要的数据
3. **避免过度序列化**：不要序列化整个对象图
4. **使用投影查询**：对于复杂查询，考虑使用JPA投影

## 相关文件

- `src/main/java/com/example/quiz/dto/QuestionDTO.java` - DTO类
- `src/main/java/com/example/quiz/controller/QuizController.java` - 修改后的控制器
- `src/main/java/com/example/quiz/model/*.java` - 添加@JsonIgnore的实体类
- `src/test/java/com/example/quiz/controller/QuizControllerJsonTest.java` - 测试类

## 总结

通过使用DTO模式和@JsonIgnore注解的组合方案，成功解决了JPA实体类循环引用导致的JSON序列化问题。现在前端可以正常加载题目数据，整个答题系统功能正常。
