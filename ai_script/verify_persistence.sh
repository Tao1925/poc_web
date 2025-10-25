#!/bin/bash
# 数据持久化验证脚本
# 用途：验证数据库文件是否正确创建和持久化

echo "================================"
echo "数据持久化验证脚本"
echo "================================"
echo ""

# 检查数据目录
echo "1. 检查数据目录..."
if [ -d "./data" ]; then
    echo "   ✅ data/ 目录存在"
else
    echo "   ⚠️  data/ 目录不存在，将在首次启动时创建"
fi

# 检查数据库文件
echo ""
echo "2. 检查数据库文件..."
if [ -f "./data/pocdb.mv.db" ]; then
    FILE_SIZE=$(ls -lh ./data/pocdb.mv.db | awk '{print $5}')
    echo "   ✅ 数据库文件存在"
    echo "   文件大小: $FILE_SIZE"
    echo "   文件路径: ./data/pocdb.mv.db"
else
    echo "   ⚠️  数据库文件不存在"
    echo "   提示：首次启动应用时会自动创建"
fi

# 检查配置
echo ""
echo "3. 检查配置文件..."
if grep -q "jdbc:h2:file:./data/pocdb" src/main/resources/application.properties; then
    echo "   ✅ 使用文件数据库配置"
else
    echo "   ❌ 配置错误：未使用文件数据库"
    exit 1
fi

if grep -q "spring.jpa.defer-datasource-initialization=true" src/main/resources/application.properties; then
    echo "   ✅ SQL 延迟初始化已启用"
else
    echo "   ❌ 配置缺失：spring.jpa.defer-datasource-initialization=true"
    exit 1
fi

# 检查应用状态
echo ""
echo "4. 检查应用状态..."
if curl -s http://localhost:8080/h2-console > /dev/null 2>&1; then
    echo "   ✅ 应用正在运行"
    
    # 验证数据
    echo ""
    echo "5. 验证数据..."
    RESPONSE=$(curl -s "http://localhost:8080/quiz/stats/student1")
    if [[ $RESPONSE == *"用户不存在"* ]]; then
        echo "   ⚠️  用户数据为空（可能是首次启动）"
    else
        echo "   ✅ 用户数据存在"
        echo "   响应: $RESPONSE"
    fi
else
    echo "   ⚠️  应用未运行"
    echo "   提示：请先启动应用 mvn spring-boot:run"
fi

echo ""
echo "================================"
echo "验证完成！"
echo "================================"
echo ""
echo "数据库配置："
echo "  - 类型: H2 文件数据库"
echo "  - 位置: ./data/pocdb.mv.db"
echo "  - 持久化: ✅ 是"
echo ""
echo "H2 控制台："
echo "  - URL: http://localhost:8080/h2-console"
echo "  - JDBC URL: jdbc:h2:file:./data/pocdb"
echo "  - 用户名: sa"
echo "  - 密码: (留空)"
echo ""

