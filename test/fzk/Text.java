package fzk;

import junit.framework.TestCase;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 测试类
 *
 * 注：
 * 1. 阅读源码时删除了一些碍眼的log；
 * 2. 注释了一些与主干流程无关的代码，都会在上一行备注TODO，并在结束的位置备注end，
 *    当以后测更多功能时，若想要恢复到原始代码，全文搜索TODO fzk即可看到。
 * 3. 碰到的设计模式，使用"【设计模式】"标注了，全文搜索即可。
 *
 * author:fanzhoukai
 */
public class Text extends TestCase {

    /**
     * 1. 初始化IoC容器，并根据名称获取bean
     * 2. 测试后置处理器、后置处理器的优先级
     */
    public void testIocHelloWorld() {
        ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
        HelloService helloService = (HelloService) context.getBean("helloService");
        helloService.sayHello();
    }
}
