# 题目匹配逻辑改造：从ID匹配迁移到Title匹配

## 修改背景
原有的逻辑是根据 `question_id` 进行答案匹配。当 `data.json` 中的题目顺序发生变化（增删改）时，如果仅仅依赖 ID 且 ID 生成机制不稳定（或数据重新导入），可能会导致答案错位。
用户要求改为根据 `question_title` 进行匹配，以保证在题目顺序调整或增删时，答案能正确对应到原题目。

## 修改内容

### 1. 后端逻辑修改

#### 1.1 Repository 层
- **QuestionRepository**: 增加 `findByTitle` 方法。
- **AnswerRepository**: 增加 `findByQuestion_TitleAndUser` 方法，允许通过题目名称和用户查找答案。

#### 1.2 Controller 层
- **QuizController**:
    - `saveAnswer`: 增加 `questionTitle` 参数。如果传入 `questionTitle`，优先根据 Title 查找题目；兼容原有的 `questionId` 逻辑。
    - `getAnswerByTitle`: 新增接口（或修改原接口），支持通过 `title` 获取答案。

#### 1.3 数据同步逻辑 (QuizLoginDemoApplication)
- 修改 `CommandLineRunner` 的数据同步逻辑。
- 在从 `data.json` 加载数据时，优先使用 `title` 在数据库中查找现有题目。
- 如果找到同名题目，则更新其 `sort_order`, `question_number` 等字段，**保留原有 ID**。
- 这样可以确保即使 JSON 文件中题目顺序变了，数据库中的 ID 不变，关联的 Answer 也就不会丢失或错位。

### 2. 前端逻辑修改

#### 2.1 quiz.html
- 在渲染题目列表时，增加 `data-question-title` 属性。
- `selectQuestion`: 获取当前点击题目的 Title。
- `loadAnswer`: 调用后端 API 时传入 `title` 而不是（或作为补充）ID。
- `saveAnswer`: 提交答案时传入 `questionTitle`。

### 3. 测试验证

#### 3.1 测试脚本
- 创建脚本 `ai_script/test_question_reorder_persistence.sh`。
- 创建测试类 `src/test/java/com/example/quiz/QuestionReorderTest.java`。

#### 3.2 测试过程
1. **Sync V1**: 加载初始数据 `data_v1.json` (Question A 在前)。
2. **Save Answer**: 为 Question A 保存答案。
3. **Sync V2**: 加载新数据 `data_v2.json` (Question A 被移动到后面，Question B 插队)。
4. **Verify**:
    - 验证 Question A 的 ID 是否保持不变（通过 Title 匹配逻辑）。
    - 验证 Question A 的 Answer 是否依然存在且内容正确。
    - 验证 Question A 的 `questionNumber` 是否已更新（如从 1.1 变为 1.2）。

#### 3.3 测试结果
- 测试通过。证明系统能正确处理题目重排场景，答案不丢失、不错位。
