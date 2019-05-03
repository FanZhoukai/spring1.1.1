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

/**
 * 为已配置的AOP代理提供委托接口，允许创建实际的代理对象。
 * <p>
 * 就像在DefaultAopProxyFactory一样，jdk动态代理和cglib动态代理都可以实现开箱即用。
 */
public interface AopProxy {
	/**
	 * 创建一个新的代理对象，代理给定的接口
	 * 使用线程上下文类加载器(thread context class loader)
	 */
	public abstract Object getProxy();

	/**
	 * 创建一个新的代理对象，代理给定的接口
	 * 使用自定义的类加载器
	 */
	public abstract Object getProxy(ClassLoader cl);
}