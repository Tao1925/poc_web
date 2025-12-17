# 修复题目重排后前端显示顺序错乱问题

## 问题描述
用户在 `data.json` 中交换了题目的顺序（例如将原来的 1.2 放到 1.1 前面）。
数据同步逻辑正确更新了 `sort_order` 和 `question_number`。
但是前端页面显示时，仍然按照旧的顺序（或 ID 顺序）显示，导致变成了 "1.2 题目A", "1.1 题目B" 这种顺序错乱的情况。

## 原因分析
前端通过 `th:each="question : ${chapter.questions}"` 遍历题目。
该列表来自 `Chapter` 实体的 `questions` 关联字段。
原有的 `Chapter.java` 中，`@OneToMany` 关联没有指定排序规则。
因此，Hibernate 加载关联集合时，默认可能按照主键 (ID) 排序。
当题目顺序变更时，ID 没有变（为了保持答案关联），只有 `sort_order` 变了。
如果按 ID 排序，就会导致前端显示的顺序与 JSON 中定义的顺序不一致。

## 解决方案
在 `Chapter` 实体的 `questions` 字段上添加 `@OrderBy` 注解，强制按 `sortOrder` 升序排序。

```java
    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC") // 新增
    @JsonIgnore
    private List<Question> questions;
```

## 验证
更新了 `QuestionReorderTest.java`，增加对 `chapter.getQuestions()` 返回列表顺序的断言。
测试通过，确认返回的题目列表是严格按照 `sort_order` 排序的。
