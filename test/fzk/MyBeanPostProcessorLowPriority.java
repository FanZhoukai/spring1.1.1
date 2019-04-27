package fzk;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

/**
 * 测试后置处理器（低优先级）
 *
 * author:fanzhoukai
 * 2019/4/27 20:49
 */
public class MyBeanPostProcessorLowPriority implements BeanPostProcessor, Ordered {
	/**
	 * 初始化前执行
	 */
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("bean post processor - before initialization - low priority");
		return bean;
	}

	/**
	 * 初始化后执行
	 */
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		System.out.println("bean post processor - after initialization - low priority");
		return bean;
	}

	/**
	 * 后置处理器优先级
	 * 值越小，优先级越高（越先被执行）
	 */
	public int getOrder() {
		return 2;
	}
}
