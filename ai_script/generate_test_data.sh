#!/bin/bash
# 生成测试数据脚本 - 为判题系统创建学生答案

echo "========================================="
echo "生成测试数据"
echo "========================================="
echo ""

# 检查应用状态
echo "1. 检查应用状态..."
if ! curl -s http://localhost:8080/ > /dev/null 2>&1; then
    echo "   ❌ 应用未运行，请先启动应用"
    exit 1
fi
echo "   ✅ 应用正在运行"

echo ""
echo "2. 生成 student1 的测试答案..."

# student1 - 题目1的答案
curl -s -X POST "http://localhost:8080/quiz/save" \
    -d "questionId=1" \
    -d "content=<div><h3>Java基本数据类型</h3><p>Java中有8种基本数据类型：</p><ul><li>整型：byte(1字节)、short(2字节)、int(4字节)、long(8字节)</li><li>浮点型：float(4字节)、double(8字节)</li><li>字符型：char(2字节)</li><li>布尔型：boolean(1位)</li></ul><p>使用场景：整型用于计数，浮点型用于科学计算，char用于字符处理，boolean用于逻辑判断。</p></div>" \
    -d "username=student1" > /dev/null
echo "   ✅ 题目1答案已提交"

# student1 - 题目2的答案
curl -s -X POST "http://localhost:8080/quiz/save" \
    -d "questionId=2" \
    -d "content=<div><h3>Java控制结构</h3><p><strong>if-else语句：</strong></p><pre>if (condition) {
    // 代码块
} else {
    // 代码块
}</pre><p><strong>switch语句：</strong></p><pre>switch (variable) {
    case value1:
        break;
    default:
        break;
}</pre><p><strong>循环语句：</strong>for、while、do-while</p></div>" \
    -d "username=student1" > /dev/null
echo "   ✅ 题目2答案已提交"

# student1 - 题目4的答案
curl -s -X POST "http://localhost:8080/quiz/save" \
    -d "questionId=4" \
    -d "content=<div><h3>学生类设计</h3><pre>public class Student {
    private String name;
    private int age;
    private String studentId;
    
    public Student(String name, int age, String studentId) {
        this.name = name;
        this.age = age;
        this.studentId = studentId;
    }
    
    // getter和setter方法
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    // ... 其他getter/setter
}</pre></div>" \
    -d "username=student1" > /dev/null
echo "   ✅ 题目4答案已提交"

echo ""
echo "3. 生成 teacher 的测试答案..."

# teacher - 题目1的答案
curl -s -X POST "http://localhost:8080/quiz/save" \
    -d "questionId=1" \
    -d "content=<div><p>Java的8种基本数据类型分为四类：</p><ol><li>整数类型：byte(-128~127)、short(-32768~32767)、int、long</li><li>浮点类型：float(单精度)、double(双精度)</li><li>字符类型：char(Unicode字符)</li><li>布尔类型：boolean(true/false)</li></ol><p>在实际开发中，int是最常用的整数类型，double用于浮点运算，boolean用于条件判断。</p></div>" \
    -d "username=teacher" > /dev/null
echo "   ✅ 题目1答案已提交"

# teacher - 题目2的答案  
curl -s -X POST "http://localhost:8080/quiz/save" \
    -d "questionId=2" \
    -d "content=<div><h3>控制结构详解</h3><p>1. <strong>if-else</strong>：条件分支</p><p>2. <strong>switch</strong>：多分支选择，支持int、String等类型</p><p>3. <strong>for循环</strong>：已知循环次数时使用</p><p>4. <strong>while循环</strong>：条件循环</p><p>5. <strong>do-while循环</strong>：至少执行一次的循环</p><p>示例代码：</p><pre>for (int i = 0; i < 10; i++) {
    System.out.println(i);
}</pre></div>" \
    -d "username=teacher" > /dev/null
echo "   ✅ 题目2答案已提交"

# teacher - 题目3的答案
curl -s -X POST "http://localhost:8080/quiz/save" \
    -d "questionId=3" \
    -d "content=<div><h3>数组操作</h3><p><strong>声明和初始化：</strong></p><pre>int[] arr = new int[5];
int[] arr2 = {1, 2, 3, 4, 5};</pre><p><strong>排序示例：</strong></p><pre>import java.util.Arrays;

public class ArraySort {
    public static void main(String[] args) {
        int[] numbers = {5, 2, 8, 1, 9};
        Arrays.sort(numbers);
        System.out.println(Arrays.toString(numbers));
    }
}</pre></div>" \
    -d "username=teacher" > /dev/null
echo "   ✅ 题目3答案已提交"

# teacher - 题目4的答案
curl -s -X POST "http://localhost:8080/quiz/save" \
    -d "questionId=4" \
    -d "content=<div><h3>Student类完整实现</h3><pre>public class Student {
    private String name;
    private int age;
    private String studentId;
    
    // 无参构造
    public Student() {}
    
    // 有参构造
    public Student(String name, int age, String studentId) {
        this.name = name;
        this.age = age;
        this.studentId = studentId;
    }
    
    // Getter和Setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    @Override
    public String toString() {
        return \"Student{name='\" + name + \"', age=\" + age + \", studentId='\" + studentId + \"'}\";
    }
}</pre></div>" \
    -d "username=teacher" > /dev/null
echo "   ✅ 题目4答案已提交"

echo ""
echo "========================================="
echo "✅ 测试数据生成完成"
echo "========================================="
echo ""
echo "生成的数据："
echo "  • student1: 3道题的答案（题目1, 2, 4）"
echo "  • teacher:  4道题的答案（题目1, 2, 3, 4）"
echo ""
echo "验证方式："
echo "  1. 使用 admin/123456 登录"
echo "  2. 系统会自动跳转到判题界面"
echo "  3. 选择题目查看学生答案"
echo "  4. 为学生答案打分"
echo ""

