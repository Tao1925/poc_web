# POC Web - Java 答题系统

基于 Spring Boot 的 Java 知识答题系统，支持多用户答题、数据持久化保存。

## 快速开始

```bash
mvn spring-boot:run
```

访问：http://localhost:8080/

测试账号：
- `admin` / `123456`
- `student1` / `student123`
- `teacher` / `teacher123`

## 功能特性

- ✅ **数据持久化**：数据保存在本地文件，重启不丢失
- ✅ **自动去重**：使用 MERGE 语句，避免数据重复
- ✅ **多用户支持**：不同用户数据独立存储
- ✅ **自动初始化**：启动时自动创建表和数据
- ✅ **H2 控制台**：可视化数据库管理

## 数据存储

- **位置**: `./data/pocdb.mv.db`
- **类型**: H2 文件数据库
- **持久化**: ✅ 是

## 验证脚本

```bash
./ai_script/verify_persistence.sh
```

## 重要说明

首次使用或数据库为空时：
```bash
rm -rf ./data
mvn spring-boot:run
```

## 文档

详见：`ai_process_md/数据库持久化完整解决方案_20251025_2152.md`

## 测试状态

✅ 所有功能已测试通过：
- 数据自动加载（用户、章节、题目）
- 登录功能正常
- 答题保存功能正常
- 数据持久化保存
- 重启后数据不丢失

## 项目规范

- 一次性脚本 → `ai_script/` 文件夹
- 说明文档 → `ai_process_md/` 文件夹