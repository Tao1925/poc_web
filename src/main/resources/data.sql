-- 插入测试用户数据
INSERT INTO users (username, password) VALUES ('admin', '123456');
INSERT INTO users (username, password) VALUES ('student1', 'student123');
INSERT INTO users (username, password) VALUES ('teacher', 'teacher123');

-- 章节数据
INSERT INTO chapters (title, description, sort_order) VALUES 
('第一章：Java基础', 'Java编程语言的基础知识，包括语法、数据类型、控制结构等', 1),
('第二章：面向对象编程', 'Java面向对象编程的核心概念，包括类、对象、继承、多态等', 2),
('第三章：集合框架', 'Java集合框架的使用，包括List、Set、Map等常用集合类型', 3),
('第四章：异常处理', 'Java异常处理机制，包括异常类型、抛出和捕获异常的方法', 4);

-- 题目数据
INSERT INTO questions (title, description, question_number, sort_order, chapter_id) VALUES 
('Java数据类型', '请详细说明Java中的基本数据类型有哪些，并举例说明每种类型的取值范围和使用场景。', '1.1', 1, 1),
('控制结构', '请解释Java中的if-else语句、switch语句和循环语句的使用方法，并给出相应的代码示例。', '1.2', 2, 1),
('数组操作', '请说明Java中数组的声明、初始化和常用操作方法，并编写一个数组排序的程序。', '1.3', 3, 1),
('类的定义', '请设计一个学生类(Student)，包含姓名、年龄、学号等属性，并提供相应的构造方法和getter/setter方法。', '2.1', 1, 2),
('继承关系', '请基于学生类创建一个研究生类(GraduateStudent)，并说明继承的优势和注意事项。', '2.2', 2, 2),
('多态应用', '请举例说明Java中多态的概念和应用场景，并编写相应的代码演示。', '2.3', 3, 2),
('List集合', '请比较ArrayList和LinkedList的特点，并说明在什么情况下应该选择哪种实现。', '3.1', 1, 3),
('Map集合', '请说明HashMap和TreeMap的区别，并编写代码演示它们的使用方法。', '3.2', 2, 3),
('Set集合', '请解释HashSet、LinkedHashSet和TreeSet的特点和适用场景。', '3.3', 3, 3),
('异常分类', '请说明Java中Checked异常和Unchecked异常的区别，并举例说明。', '4.1', 1, 4),
('异常处理', '请编写代码演示try-catch-finally语句的使用，并说明finally块的作用。', '4.2', 2, 4),
('自定义异常', '请创建一个自定义异常类，并演示如何抛出和捕获自定义异常。', '4.3', 3, 4);