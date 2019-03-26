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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;

/**
 * SPI interface to be implemented by most if not all listable bean factories.
 * In addition to ConfigurableBeanFactory, it provides means to evaluate bean
 * definitions and to pre-instantiate singletons.
 *
 * <p>Allows for framework-internal plug'n'play, e.g. in AbstractApplicationContext.
 *
 * @author Juergen Hoeller
 * @since 03.11.2003
 * @see org.springframework.context.support.AbstractApplicationContext#getBeanFactory
 */
public interface ConfigurableListableBeanFactory
		extends ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory {

	/**
	 * Return the registered BeanDefinition for the given bean, allowing access
	 * to its property values and constructor argument value (which can be
	 * modified during bean factory post-processing).
	 * <p>A returned BeanDefinition object should not be a copy but the original
	 * definition object as registered in the factory. This means that it should
	 * be castable to a more specific implementation type, if necessary.
	 * @param beanName name of the bean
	 * @return the registered BeanDefinition
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException
	 * if there is no bean with the given name
	 * @throws BeansException in case of errors
	 */
	BeanDefinition getBeanDefinition(String beanName) throws BeansException;

	/**
	 * 初始化全部单例对象（懒加载除外），同时考虑FactoryBean的情况。
	 * 如果需要的话，通常在bean factory设置结束后被调用。
	 * 由于这是一个启动方法（startup method），如果失败，必须销毁所有已创建的单例对象，以避免挂起资源。
	 * 换句话说，该方法结束后，所有的单例实例应该要么全部被创建，要么全部不被创建。
	 */
	void preInstantiateSingletons() throws BeansException;

}
