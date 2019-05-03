/*
 * Copyright 2002-2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.framework.autoproxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyConfig;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

/**
 * 使用AOP代理封装一组bean的后置处理器实现类。
 * 这些代理会在调用bean本身之前，委托给指定的拦截器。
 *
 * 本类区分了“公共”拦截器和“特殊”拦截器，前者用于它创建的所有代理，后者用于每个bean实例。
 * 可以使用interceptorNames属性来设置公共拦截器，除此之外没必要设置任何公共拦截器。
 *
 *
 * BeanPostProcessor implementation that wraps a group of beans with AOP proxies
 * that delegate to the given interceptors before invoking the bean itself.
 *
 * <p>This class distinguishes between "common" interceptors: shared for all proxies it
 * creates, and "specific" interceptors: unique per bean instance. There need not
 * be any common interceptors. If there are, they are set using the interceptorNames
 * property. As with ProxyFactoryBean, interceptors names in the current factory
 * are used rather than bean references to allow correct handling of prototype
 * advisors and interceptors: for example, to support stateful mixins.
 * Any advice type is supported for "interceptorNames" entries.
 *
 * <p>Such autoproxying is particularly useful if there's a large number of beans that need
 * to be wrapped with similar proxies, i.e. delegating to the same interceptors.
 * Instead of x repetitive proxy definitions for x target beans, you can register
 * one single such post processor with the bean factory to achieve the same effect.
 *
 * <p>Subclasses can apply any strategy to decide if a bean is to be proxied,
 * e.g. by type, by name, by definition details, etc. They can also return
 * additional interceptors that should just be applied to the specific bean
 * instance. The default concrete implementation is BeanNameAutoProxyCreator,
 * identifying the beans to be proxied via a list of bean names.
 *
 * <p>Any number of TargetSourceCreator implementations can be used with any subclass,
 * to create a custom target source - for example, to pool prototype objects.
 * Autoproxying will occur even if there is no advice if a TargetSourceCreator specifies
 * a custom TargetSource. If there are no TargetSourceCreators set, or if none matches,
 * a SingletonTargetSource will be used by default to wrap the bean to be autoproxied.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @since October 13, 2003
 * @see #setInterceptorNames
 * @see BeanNameAutoProxyCreator
 */
public abstract class AbstractAutoProxyCreator extends ProxyConfig
		implements BeanPostProcessor, BeanFactoryAware, Ordered {

	/**
	 * Convenience constant for subclasses: Return value for "do not proxy".
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	protected static final Object[] DO_NOT_PROXY = null;

	/**
	 * Convenience constant for subclasses: Return value for
	 * "proxy without additional interceptors, just the common ones".
	 * @see #getAdvicesAndAdvisorsForBean
	 */
	protected static final Object[] PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS = new Object[0];


	protected final Log logger = LogFactory.getLog(getClass());

	/** Default value is same as non-ordered */
	private int order = Integer.MAX_VALUE;

	/** Default is global AdvisorAdapterRegistry */
	private AdvisorAdapterRegistry advisorAdapterRegistry = GlobalAdvisorAdapterRegistry.getInstance();

	/**
	 * Names of common interceptors. We must use bean name rather than object references
	 * to handle prototype advisors/interceptors.
	 * Default is the empty array: no common interceptors.
	 */
	private String[] interceptorNames = new String[0];

	// 是否优先执行common拦截器
	private boolean applyCommonInterceptorsFirst = true;

	private List customTargetSourceCreators = Collections.EMPTY_LIST;

	private BeanFactory owningBeanFactory;


	/**
	 * Set the ordering which will apply to this class's implementation
	 * of Ordered, used when applying multiple BeanPostProcessors.
	 * Default value is Integer.MAX_VALUE, meaning that it's non-ordered.
	 * @param order ordering value
	 */
	public final void setOrder(int order) {
	  this.order = order;
	}

	public final int getOrder() {
	  return order;
	}

	/**
	 * Specify the AdvisorAdapterRegistry to use.
	 * Default is the global AdvisorAdapterRegistry.
	 * @see org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry
	 */
	public void setAdvisorAdapterRegistry(AdvisorAdapterRegistry advisorAdapterRegistry) {
		this.advisorAdapterRegistry = advisorAdapterRegistry;
	}

	/**
	 * Set custom TargetSourceCreators to be applied in this order.
	 * If the list is empty, or they all return null, a SingletonTargetSource
	 * will be created.
	 * <p>TargetSourceCreators can only be invoked if this post processor is used
	 * in a BeanFactory, and its BeanFactoryAware callback is used.
	 * @param targetSourceCreators list of TargetSourceCreator.
	 * Ordering is significant: The TargetSource returned from the first matching
	 * TargetSourceCreator (that is, the first that returns non-null) will be used.
	 */
	public void setCustomTargetSourceCreators(List targetSourceCreators) {
		this.customTargetSourceCreators = targetSourceCreators;
	}

	/**
	 * Set the common interceptors. These must be bean names
	 * in the current factory. They can be of any advice or#
	 * advisor type Spring supports. If this property isn't
	 * set, there will be zero common interceptors. This is
	 * perfectly valid, if "specific" interceptors such as
	 * matching Advisors are all we want.
	 */
	public void setInterceptorNames(String[] interceptorNames) {
		this.interceptorNames = interceptorNames;
	}

	/**
	 * Set whether the common interceptors should be applied before
	 * bean-specific ones. Default is true; else, bean-specific
	 * interceptors will get applied first.
	 */
	public void setApplyCommonInterceptorsFirst(boolean applyCommonInterceptorsFirst) {
		this.applyCommonInterceptorsFirst = applyCommonInterceptorsFirst;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.owningBeanFactory = beanFactory;
	}

	/**
	 * Return the owning BeanFactory
	 * May be null, as this object doesn't need to belong to a bean factory.
	 */
	protected BeanFactory getBeanFactory() {
		return this.owningBeanFactory;
	}

	/**
	 * 初始化前的后置处理器
	 * 啥都不干，因为用户可能自定义了初始化方法，不要影响到它们的执行
	 */
	public Object postProcessBeforeInitialization(Object bean, String beanName) {
		return bean;
	}

	/**
	 * 如果bean是一个要被代理的子类，则使用配置的拦截器，创建一个代理对象
	 */
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		// 检查特殊case：我们不想代理那些本身就用于自动代理的部分基础组件
		// （Advisor, MethodInterceptor, AbstractAutoProxyCreator的实现类），以免栈溢出异常
		if (isInfrastructureClass(bean, beanName) || shouldSkip(bean, beanName)) {
			logger.debug("Did not attempt to autoproxy infrastructure class [" + bean.getClass().getName() + "]");
			return bean;
		}

		// 获取自定义的TargetSource
		TargetSource targetSource = getCustomTargetSource(bean, beanName);

		// 获取需要被代理的advice(横切逻辑)和advisor(切面)
		// 其中specific是指，对一个bean独有的advice和advisor，与下面的commonInterceptors相对应
		Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean, beanName, targetSource);

		// 如果有advice，或者TargetSourceCreator想要做些自己的事（比如池化），进行代理
		// proxy if we have advice or if a TargetSourceCreator wants to do some
		// fancy stuff such as pooling
		if (specificInterceptors != DO_NOT_PROXY || targetSource != null) {

			if (targetSource == null) {
				// 默认单例TargetSource
				targetSource = new SingletonTargetSource(bean);
			}

			// handle prototypes correctly
			Advisor[] commonInterceptors = resolveInterceptorNames();

			// 合并所有拦截器（common）进allInterceptors列表中
			List allInterceptors = new ArrayList();
			if (specificInterceptors != null) {
				allInterceptors.addAll(Arrays.asList(specificInterceptors));
				if (commonInterceptors != null) {
					if (this.applyCommonInterceptorsFirst) {
						allInterceptors.addAll(0, Arrays.asList(commonInterceptors));
					}
					else {
						allInterceptors.addAll(Arrays.asList(commonInterceptors));
					}
				}
			}

			// 构造ProxyFactory，并复制配置信息（从ProxyConfig继承而来）
			ProxyFactory proxyFactory = new ProxyFactory();
			proxyFactory.copyFrom(this);

			// TODO fzk 这个if不知道是干啥的
			if (!getProxyTargetClass()) {
				// Must allow for introductions; can't just set interfaces to
				// the target's interfaces only.
				Class[] targetsInterfaces = AopUtils.getAllInterfaces(bean);
				for (int i = 0; i < targetsInterfaces.length; i++) {
					proxyFactory.addInterface(targetsInterfaces[i]);
				}
			}

			/* 配置proxyFactory(织入器) */
			// 注册全部拦截器
			for (Iterator it = allInterceptors.iterator(); it.hasNext();) {
				Advisor advisor = this.advisorAdapterRegistry.wrap(it.next());
				proxyFactory.addAdvisor(advisor);
			}
			// 设置targetSource
			proxyFactory.setTargetSource(targetSource);
			// 自定义ProxyFactory
			customizeProxyFactory(bean, proxyFactory);

			/* 创建代理对象 */
			return proxyFactory.getProxy();
		}
		else {
			return bean;
		}
	}

	/**
	 * 子类可以选择实现该方法，如改变暴露出的接口
	 *
	 * @param bean 将要被自动代理的bean
	 * @param pf   将要用于创建代理对象的织入器ProxyFactory。该方法结束后，将立即创建代理对象
	 */
	protected void customizeProxyFactory(Object bean, ProxyFactory pf) {
		// This implementation does nothing
	}

	/**
	 * 解析拦截器bean名称
	 */
	private Advisor[] resolveInterceptorNames() {
		Advisor[] advisors = new Advisor[this.interceptorNames.length];
		for (int i = 0; i < this.interceptorNames.length; i++) {
			Object next = this.owningBeanFactory.getBean(this.interceptorNames[i]);
			advisors[i] = this.advisorAdapterRegistry.wrap(next);
		}
		return advisors;
	}

	protected boolean isInfrastructureClass(Object bean, String beanName) {
		return Advisor.class.isAssignableFrom(bean.getClass()) ||
				MethodInterceptor.class.isAssignableFrom(bean.getClass()) ||
				AbstractAutoProxyCreator.class.isAssignableFrom(bean.getClass());
	}

	/**
	 * 子类应重写该方法。如果一个bean不需要被这个后置处理器自动代理的话，返回true。
	 * 可用于避免循环引用。
	 *
	 * @param bean     新的bean实例
	 * @param beanName bean名称
	 */
	protected boolean shouldSkip(Object bean, String beanName) {
		return false;
	}

	/**
	 * 为bean实例创建target source。
	 * <p>
	 * 如果设置了TargetSourceCreator（customTargetSourceCreators属性中设置），直接使用。
	 * 子类可以重写该方法，来采用不同的机制。
	 *
	 * @return a TargetSource for this bean
	 * @see #setCustomTargetSourceCreators 设置自定义的TargetSourceCreator
	 */
	protected TargetSource getCustomTargetSource(Object bean, String beanName) {
		// we can't create fancy target sources for singletons
		if (this.owningBeanFactory != null && this.owningBeanFactory.containsBean(beanName) &&
				!this.owningBeanFactory.isSingleton(beanName)) {
			logger.info("Checking for custom TargetSource for bean with beanName '" + beanName + "'");
			for (int i = 0; i < this.customTargetSourceCreators.size(); i++) {
				TargetSourceCreator tsc = (TargetSourceCreator) this.customTargetSourceCreators.get(i);
				TargetSource ts = tsc.getTargetSource(bean, beanName, this.owningBeanFactory);
				if (ts != null) {
					// found a match
					if (logger.isInfoEnabled()) {
						logger.info("TargetSourceCreator [" + tsc + " found custom TargetSource for bean with beanName '" +
												beanName + "'");
					}
					return ts;
				}
			}
		}

		// no custom TargetSource found
		return null;
	}

	/**
	 * 返回给定的bean是否需要被代理，要应用哪些附加的advice和advisor。
	 *
	 * 以前版本中，该方法叫"getInterceptorAndAdvisorForBean"，在spring1.1的术语解释环节中被重命名。
	 * AOP alliance中的"拦截器"，只是Advice的一种特殊形式，因此，我们更倾向于使用Advice这种通用说法。
	 * 第三个参数customTargetSource是spring1.1新增的，将其添加进该方法的所有实现中。
	 *
	 * @param bean the new bean instance 新的bean实例
	 * @param beanName the name of the bean bean名称
	 * @param customTargetSource 由getTargetSource()方法产生。只有当自定义target source在使用中时，才会为空。
	 * @return 指定bean需要应用的附加拦截器组成的数组；若没有附加的拦截器，返回空数组；如果根本没有代理，返回null
	 *
	 * 参考常量：DO_NOT_PROXY, PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
	 *
	 * @see #postProcessAfterInitialization
	 * @see #DO_NOT_PROXY
	 * @see #PROXY_WITHOUT_ADDITIONAL_INTERCEPTORS
	 */
	protected abstract Object[] getAdvicesAndAdvisorsForBean(
			Object bean, String beanName, TargetSource customTargetSource) throws BeansException;

}
