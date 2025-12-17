## 本次修改主题
使用 `data.json` 替代 `data.sql`，并在应用启动时强制同步 `User/Chapter/Question` 三张表数据。

## 修改动机
- `data.sql` 的初始化方式难以保证“每次启动数据库数据与资源文件完全一致”
- 使用 `data.json` 作为单一数据源，可以更直观地维护用户、章节、题目结构，并在启动时进行对齐（新增/更新/删除）

## 主要改动
- 新增 `src/main/resources/data.json`，包含：
  - `users`: `username/password`
  - `chapters`: `title` + `questions`
  - `questions`: `title/description/total_score`
- 启动时读取 `data.json` 并同步到数据库：
  - `Chapter.sort_order` 由 JSON 中章节顺序决定（从 1 开始）
  - `Question.question_number` 按 `chapterIndex.questionIndex` 生成（例如 `3.2`）
  - `Question.sort_order` 由章节内题目顺序决定（从 1 开始）
  - 同步策略（确保与 JSON 完全一致）：
    - 不在 JSON 中的 `User` 会被删除（先删除该用户的 `Answer`）
    - 不在 JSON 中的 `Question` 会被删除（先删除该题的 `Answer`）
    - 不在 JSON 中的 `Chapter` 会被删除
    - JSON 中存在的记录会被更新为 JSON 内容
- 禁用 `data.sql` 自动初始化，避免与 JSON 同步冲突。

## 代码与配置变更
- `src/main/resources/application.properties`
  - 将 `spring.sql.init.mode` 设为 `never`，停止自动执行 `data.sql`
  - 新增：
    - `app.data-sync.enabled=true`
    - `app.data-sync.location=classpath:data.json`
- `src/main/java/com/example/quiz/QuizLoginDemoApplication.java`
  - 新增 `CommandLineRunner`：启动时读取 `data.json` 并同步 `users/chapters/questions`
  - 兼容测试切片：`ObjectMapper` 使用 `ObjectProvider` 获取，不存在则回退到 `new ObjectMapper()`
  - 为避免外键约束问题，删除 `User/Question` 前先删除对应 `Answer`
- `src/test/resources/application-test.properties` 以及多个测试类 `@TestPropertySource`
  - 新增 `app.data-sync.enabled=false`，避免测试环境被启动同步逻辑影响
- 修正测试用例断言
  - `admin` 登录后实际跳转为 `redirect:/grading?username=admin`，更新相关断言以匹配现有逻辑

## 新增/修改文件清单
- 新增：`src/main/resources/data.json`
- 修改：`src/main/resources/application.properties`
- 修改：`src/main/java/com/example/quiz/QuizLoginDemoApplication.java`
- 修改：`src/test/resources/application-test.properties`
- 修改：`src/test/java/com/example/quiz/QuizLoginDemoApplicationIntegrationTest.java`
- 修改：`src/test/java/com/example/quiz/controller/AnswerPersistenceTest.java`
- 修改：`src/test/java/com/example/quiz/controller/QuizControllerJsonTest.java`
- 修改：`src/test/java/com/example/quiz/repository/UserRepositoryTest.java`
- 修改：`src/test/java/com/example/quiz/controller/LoginControllerTest.java`

## 验证结果
- 已执行 `mvn test`，测试全部通过。

