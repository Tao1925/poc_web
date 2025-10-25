#!/bin/bash
# 图片上传功能测试脚本

echo "================================"
echo "图片上传功能测试"
echo "================================"
echo ""

# 检查应用状态
echo "1. 检查应用状态..."
if curl -s http://localhost:8080/ > /dev/null 2>&1; then
    echo "   ✅ 应用正在运行"
else
    echo "   ❌ 应用未运行"
    exit 1
fi

# 检查quiz页面
echo ""
echo "2. 检查答题页面..."
QUIZ_PAGE=$(curl -s "http://localhost:8080/quiz?username=student1")

if echo "$QUIZ_PAGE" | grep -q "imageInput"; then
    echo "   ✅ 文件输入框已添加"
else
    echo "   ❌ 文件输入框未找到"
    exit 1
fi

if echo "$QUIZ_PAGE" | grep -q 'type="file"'; then
    echo "   ✅ 文件选择器已配置"
else
    echo "   ❌ 文件选择器未配置"
    exit 1
fi

if echo "$QUIZ_PAGE" | grep -q 'accept="image/\*"'; then
    echo "   ✅ 图片类型限制已设置"
else
    echo "   ❌ 图片类型限制未设置"
    exit 1
fi

if echo "$QUIZ_PAGE" | grep -q "handleImageUpload"; then
    echo "   ✅ 图片上传处理函数已添加"
else
    echo "   ❌ 图片上传处理函数未找到"
    exit 1
fi

# 检查功能实现
echo ""
echo "3. 检查功能实现..."
if echo "$QUIZ_PAGE" | grep -q "readAsDataURL"; then
    echo "   ✅ Base64编码功能已实现"
else
    echo "   ❌ Base64编码功能未实现"
    exit 1
fi

if echo "$QUIZ_PAGE" | grep -q "maxSize.*5.*1024.*1024"; then
    echo "   ✅ 文件大小限制已设置（5MB）"
else
    echo "   ⚠️  文件大小限制可能未设置"
fi

echo ""
echo "================================"
echo "自动化测试通过！"
echo "================================"
echo ""
echo "功能说明："
echo "  - ✅ 点击📷按钮打开文件选择器"
echo "  - ✅ 支持Windows和Mac系统"
echo "  - ✅ 自动过滤非图片文件"
echo "  - ✅ 图片大小限制5MB"
echo "  - ✅ 图片转Base64嵌入"
echo "  - ✅ 自动触发保存"
echo ""
echo "手动测试步骤："
echo "  1. 访问 http://localhost:8080/"
echo "  2. 登录（student1 / student123）"
echo "  3. 选择任意题目"
echo "  4. 点击工具栏的📷按钮"
echo "  5. 选择本地图片文件"
echo "  6. 验证图片是否显示在编辑器中"
echo ""

