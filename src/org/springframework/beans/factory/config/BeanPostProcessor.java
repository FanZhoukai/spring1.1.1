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

/**
 * 允许对新创建的bean实例自定义修改。
 * 比如检查标记接口，或把bean包装进代理中（AOP就是这么做的）
 *
 * ApplicationContext可以自动检测其中的后置处理器bean，并应用于其他所有bean的创建过程中。
 * 允许后置处理器可以手动注入普通bean工厂中。
 *
 * 常见用法：
 * 通常，通过标记接口来给属性赋值的后置处理器会实现postProcessBeforeInitialization；
 * 使用代理包装bean的后置处理器，通常需要实现postProcessAfterInitialization。
 *
 *
 * （由于后置处理器对应用于 “任何其他” bean上的，所以需要通过接口来分组，不同的组有不同的处理方式）
 *
 * @see ConfigurableBeanFactory#addBeanPostProcessor
 * @see BeanFactoryPostProcessor
 */
public interface BeanPostProcessor {

	/**
	 * Apply this BeanPostProcessor to the given new bean instance <i>before</i> any bean
	 * initialization callbacks (like InitializingBean's afterPropertiesSet or a custom
	 * init-method). The bean will already be populated with property values.
	 * The returned bean instance may be a wrapper around the original.
	 * @param bean the new bean instance
	 * @param beanName the beanName of the bean
	 * @return the bean instance to use, either the original or a wrapped one
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 */
	Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException;

	/**
	 * Apply this BeanPostProcessor to the given new bean instance <i>after</i> any bean
	 * initialization callbacks (like InitializingBean's afterPropertiesSet or a custom
	 * init-method). The bean will already be populated with property values.
	 * The returned bean instance may be a wrapper around the original.
	 * @param bean the new bean instance
	 * @param beanName the beanName of the bean
	 * @return the bean instance to use, either the original or a wrapped one
	 * @throws org.springframework.beans.BeansException in case of errors
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 */
	Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException;

}
