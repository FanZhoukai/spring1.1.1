package fzk.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.MethodBeforeAdvice;

import java.lang.reflect.Method;

/**
 * 测试环绕Advice
 * <p>
 * 其中定义横切逻辑，将被织入业务代码中
 * <p>
 * author:fanzhoukai
 * 2019/5/1 13:54
 */
public class MyAroundAdvice implements MethodInterceptor {

	/**
	 * 环绕Advice
	 */
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		try {
			// 前置Advice
			System.out.println("before...");
			Object[] args = methodInvocation.getArguments();
			Object target = methodInvocation.getThis();
			methodInvocation.getMethod().invoke(target, args);
		} catch (Exception e) {
			e.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			System.out.println("after...");
		}
		return null;
	}
}
