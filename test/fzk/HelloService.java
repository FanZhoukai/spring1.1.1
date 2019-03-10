package fzk;

/**
 * 测试service
 * IoC容器会自动创建该类的对象
 * <p>
 * author:fanzhoukai
 */
public class HelloService {
    /**
     * 普通bean属性，用于测试属性赋值
     */
    private String name;

    public void sayHello() {
        System.out.println("Hello, my name is " + name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
