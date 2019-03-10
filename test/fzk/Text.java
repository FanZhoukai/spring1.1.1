
import junit.framework.TestCase;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 测试类
 *
 * 注意，阅读源码时注释了一些与主干流程无关的代码，都会在上一行备注TODO，并在结束的位置备注end。
 * 当以后测更多功能时，若想要恢复到原始代码，全文搜索TODO fzk即可看到。
 *
 * author:fanzhoukai
 */
public class Text extends TestCase {

    /**
     * 初始化IoC容器，并根据名称获取bean
     */
    public void testIocHelloWorld() {
        ApplicationContext context = new ClassPathXmlApplicationContext("beans.xml");
        HelloService helloService = (HelloService) context.getBean("helloService");
        helloService.sayHello();
    }
}
