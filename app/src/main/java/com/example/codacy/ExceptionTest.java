package com.example.codacy;

import java.util.ArrayList;
import java.util.List;

public class ExceptionTest {

    // 1. 命名规范问题
    public void SomeMethod() {  // 方法命名不符合Java命名规范，应该是someMethod
        int somevar = 10;  // 变量命名不规范，应该是someVar
    }

    // 2. 冗余代码
    public int calculateSum(int a, int b) {
        int sum = a + b;
        int sumAgain = a + b;  // 重复代码
        return sum;
    }


    // 3. 潜在错误（空指针异常）
    public void printStringLength(String str) {
        System.out.println(str.length());  // 如果str是null，这里会抛出NullPointerException
    }

    // 4. 复杂度过高
    public int complexFunction(int x) {
        if (x > 0) {
            return x;
        } else if (x == 0) {
            return 0;
        } else if (x < 0) {
            return x;
        } else {
            return 0;
        }  // 过多的判断，复杂度过高
    }

    // 5. 未使用的变量
    public void calculate() {
        int unusedVariable = 5;  // 该变量未被使用
        System.out.println("Hello World");
    }

    // 6. 安全问题（SQL注入）
    public void getUserData(String username) {
        String query = "SELECT * FROM users WHERE username = '" + username + "'";  // SQL注入漏洞
        // 这里应该使用预编译语句（PreparedStatement）来防止SQL注入
    }

    // 7. 缺少注释
    public void printMessage() {
        System.out.println("Hello, Codacy!");  // 没有注释
    }

    // 8. 过度使用硬编码常量
    public void displayMessage() {
        System.out.println("Welcome to our system!");  // 硬编码的字符串，应该定义为常量
    }

    // 9. 不必要的对象创建
    public void unnecessaryObjectCreation() {
        String str = new String("Hello!");  // 不必要的对象创建，直接使用字面量即可
        System.out.println(str);
    }

    // 10. 不必要的类型转换
    public void unnecessaryTypeCasting() {
        Object obj = "Test";
        String str = (String) obj;  // 强制类型转换是不必要的，obj本来就是String类型
    }

    // 11. 使用非线程安全的集合类
    public void nonThreadSafeCollection() {
        List<String> list = new ArrayList<>();  // 使用ArrayList，但没有考虑线程安全性
        list.add("Test");
    }

    // 12. 过多的嵌套
    public void deepNesting() {
        if (true) {
            if (false) {
                if (true) {
                    if (false) {
                        // 太多嵌套，代码变得难以维护
                        System.out.println("Too deep!");
                    }
                }
            }
        }
    }

    // 13. 代码重复
    public void printMessage1() {
        System.out.println("Message 1");
    }

    public void printMessage2() {
        System.out.println("Message 2");  // 这段代码与上面的方法重复
    }

    // 14. 不必要的静态方法
    public static void unnecessaryStaticMethod() {
        CodacyTest test = new CodacyTest();
        test.printMessage();
    }

    // 15. 错误的异常处理
    public void wrongExceptionHandling() {
        try {
            int result = 10 / 0;  // 除零错误
        } catch (Exception e) {
            e.printStackTrace();  // 捕获所有异常，不应该这样做，应该捕获特定异常
        }
    }

    // 16. 魔法数字
    public void magicNumber() {
        int salary = 5000;  // 魔法数字，应该使用常量代替
    }
}