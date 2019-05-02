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

package org.springframework.aop.framework;

import org.aopalliance.intercept.Interceptor;

import org.springframework.aop.support.AopUtils;

/**
 * AOP代理工厂，编程式使用，而非通过bean factory使用。
 * 提供了一种简单的、利用代码的，获取、配置AOP代理方式。
 *
 * 啥叫代理工厂？就是织入器。利用该工厂，将横切逻辑织入到target对象中，并返回。核心方法就是getProxy，获取代理后对象。
 */
public class ProxyFactory extends AdvisedSupport {

	/**
	 * 创建一个ProxyFactory
	 */
	public ProxyFactory() {
	}

	/**
	 * 创建一个ProxyFactory
	 * 代理给定目标对象的所有接口
	 */
	public ProxyFactory(Object target) throws AopConfigException {
		if (target == null) {
			throw new AopConfigException("Can't proxy null object");
		}
		setInterfaces(AopUtils.getAllInterfaces(target));
		setTarget(target);
	}
	
	/**
	 * Create a new ProxyFactory.
	 * No target, only interfaces. Must add interceptors.
	 */
	public ProxyFactory(Class[] interfaces) {
		setInterfaces(interfaces);
	}

	/**
	 * 根据当前工厂中的配置信息，创建新的代理对象。
	 * 可以被重复调用。如果已经添加或移除了接口，效果可能会不同。可以添加或删除拦截器。
	 */
	public Object getProxy() {
		AopProxy proxy = createAopProxy();
		return proxy.getProxy();
	}

	/**
	 * Create a new proxy for the given interface and interceptor.
	 * <p>Convenience method for creating a proxy for a single interceptor,
	 * assuming that the interceptor handles all calls itself rather than
	 * delegating to a target, like in the case of remoting proxies.
	 * @param proxyInterface the interface that the proxy should implement
	 * @param interceptor the interceptor that the proxy should invoke
	 * @return the new proxy
	 */
	public static Object getProxy(Class proxyInterface, Interceptor interceptor) {
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.addInterface(proxyInterface);
		proxyFactory.addAdvice(interceptor);
		return proxyFactory.getProxy();
	}

}
