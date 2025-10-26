# POC Web - Java 答题系统

基于 Spring Boot 的 Java 知识答题系统，支持多用户答题、数据持久化保存。

## 快速开始

```bash
mvn spring-boot:run
```

访问：http://localhost:8080/

测试账号：
- `admin` / `123456` - 管理员（判题界面）
- `student1` / `student123` - 学生（答题界面）
- `teacher` / `teacher123` - 教师（答题界面）

## 功能特性

- ✅ **数据持久化**：数据保存在本地文件，重启不丢失
- ✅ **自动去重**：使用 MERGE 语句，避免数据重复
- ✅ **多用户支持**：不同用户数据独立存储
- ✅ **自动初始化**：启动时自动创建表和数据
- ✅ **H2 控制台**：可视化数据库管理
- ✅ **图片上传**：本地文件选择，支持 Windows/Mac
- ✅ **后台判题**：admin 用户可查看和评分学生答案
- ✅ **评分备注**：admin 可为每个答案添加评分备注

## 数据存储

- **位置**: `./data/pocdb.mv.db`
- **类型**: H2 文件数据库
- **持久化**: ✅ 是

## 验证脚本

```bash
# 验证数据持久化
./ai_script/verify_persistence.sh

# 测试图片上传功能（前端检查）
./ai_script/test_image_upload.sh

# 测试图片保存功能（后端检查）
./ai_script/test_image_save.sh

# 生成判题系统测试数据
./ai_script/generate_test_data.sh

# 测试判题系统功能
./ai_script/test_grading_system.sh

# 测试备注功能
./ai_script/test_remark_function.sh
```

## 重要说明

### 首次使用或数据库为空时：
```bash
rm -rf ./data
mvn spring-boot:run
```

### 升级后重建数据库（修复字段类型）：
如果遇到图片上传失败（HTTP 500），需要重建数据库：
```bash
pkill -f spring-boot
rm -rf ./data
mvn spring-boot:run
```

## 文档

- `ai_process_md/图片上传功能优化_20251025_2222.md` - 图片上传功能实现
- `ai_process_md/修复图片上传500错误_20251025_2232.md` - HTTP 500 错误修复
- `ai_process_md/后台判题系统开发_20251026_1123.md` - 后台判题系统开发
- `ai_process_md/增加备注字段功能_20251026_1135.md` - 增加备注字段功能

## 测试状态

✅ 所有功能已测试通过：
- 数据自动加载（用户、章节、题目）
- 登录功能正常（admin跳转判题，学生跳转答题）
- 答题保存功能正常
- 数据持久化保存
- 重启后数据不丢失
- 图片上传功能（本地文件选择）
- 后台判题功能（查看答案、评分、备注）

## 项目规范

- 一次性脚本 → `ai_script/` 文件夹
- 说明文档 → `ai_process_md/` 文件夹