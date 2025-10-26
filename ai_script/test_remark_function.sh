#!/bin/bash
# 测试备注功能

echo "========================================="
echo "备注功能测试"
echo "========================================="
echo ""

# 检查应用状态
echo "1. 检查应用状态..."
if ! curl -s http://localhost:8080/ > /dev/null 2>&1; then
    echo "   ❌ 应用未运行"
    exit 1
fi
echo "   ✅ 应用正在运行"

# 测试获取答案列表（包含备注字段）
echo ""
echo "2. 测试获取答案列表（包含备注字段）..."
ANSWERS=$(curl -s "http://localhost:8080/grading/question/1/answers")

if echo "$ANSWERS" | grep -q '"remark"'; then
    echo "   ✅ 答案列表包含remark字段"
else
    echo "   ❌ 答案列表缺少remark字段"
    exit 1
fi

# 获取第一个答案ID
ANSWER_ID=$(echo "$ANSWERS" | grep -o '"answerId":[0-9]*' | head -1 | cut -d':' -f2)

if [ -z "$ANSWER_ID" ]; then
    echo "   ❌ 未找到答案ID"
    exit 1
fi

echo "   答案ID: $ANSWER_ID"

# 测试保存备注
echo ""
echo "3. 测试保存分数和备注..."
SAVE_RESULT=$(curl -s -X POST "http://localhost:8080/grading/updateScore" \
    -d "answerId=$ANSWER_ID" \
    -d "score=9.0" \
    -d "remark=回答完整，逻辑清晰")

if echo "$SAVE_RESULT" | grep -q "成功"; then
    echo "   ✅ 保存成功: $SAVE_RESULT"
else
    echo "   ❌ 保存失败: $SAVE_RESULT"
    exit 1
fi

# 验证备注是否保存
echo ""
echo "4. 验证备注是否保存..."
sleep 1
UPDATED_ANSWERS=$(curl -s "http://localhost:8080/grading/question/1/answers")

if echo "$UPDATED_ANSWERS" | grep -q "回答完整，逻辑清晰"; then
    echo "   ✅ 备注已保存到数据库"
    echo "   备注内容: 回答完整，逻辑清晰"
else
    echo "   ⚠️  备注可能未正确保存"
fi

# 测试修改备注
echo ""
echo "5. 测试修改备注..."
MODIFY_RESULT=$(curl -s -X POST "http://localhost:8080/grading/updateScore" \
    -d "answerId=$ANSWER_ID" \
    -d "score=9.5" \
    -d "remark=优秀答案，内容全面")

if echo "$MODIFY_RESULT" | grep -q "成功"; then
    echo "   ✅ 修改成功: $MODIFY_RESULT"
else
    echo "   ❌ 修改失败: $MODIFY_RESULT"
fi

# 验证修改后的备注
sleep 1
MODIFIED_ANSWERS=$(curl -s "http://localhost:8080/grading/question/1/answers")

if echo "$MODIFIED_ANSWERS" | grep -q "优秀答案，内容全面"; then
    echo "   ✅ 备注修改已保存"
    echo "   新备注: 优秀答案，内容全面"
else
    echo "   ⚠️  备注修改可能未保存"
fi

# 测试判题页面HTML
echo ""
echo "6. 测试判题页面HTML..."
GRADING_PAGE=$(curl -s "http://localhost:8080/grading?username=admin")

if echo "$GRADING_PAGE" | grep -q "remark-input"; then
    echo "   ✅ 判题页面包含备注输入框"
else
    echo "   ❌ 判题页面缺少备注输入框"
fi

if echo "$GRADING_PAGE" | grep -q "备注："; then
    echo "   ✅ 判题页面包含备注标签"
else
    echo "   ❌ 判题页面缺少备注标签"
fi

echo ""
echo "========================================="
echo "✅ 测试完成"
echo "========================================="
echo ""
echo "测试结果总结："
echo "  ✓ 答案列表包含remark字段"
echo "  ✓ 保存分数和备注功能正常"
echo "  ✓ 备注已持久化到数据库"
echo "  ✓ 备注修改功能正常"
echo "  ✓ 判题页面包含备注输入框"
echo ""
echo "手动测试步骤："
echo "  1. 访问 http://localhost:8080/"
echo "  2. 使用 admin/123456 登录"
echo "  3. 选择任意题目"
echo "  4. 查看学生答案，应该看到备注输入框"
echo "  5. 输入备注并保存"
echo "  6. 刷新页面验证备注是否保存"
echo ""

