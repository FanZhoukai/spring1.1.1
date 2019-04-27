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

package org.springframework.context.support;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * ApplicationContext的抽象实现类。
 * 从包含bean定义的xml文件中拉取配置信息，利用XmlBeanDefinitionParser实现
 *
 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionParser
 */
public abstract class AbstractXmlApplicationContext extends AbstractApplicationContext {

	/**
	 * 当前上下文的beanFactory
	 * beanFactory用于创建bean，是IoC容器内全局的bean工厂
	 * TODO 为什么ApplicationContext本身继承BeanFactory，又需要持有一个BeanFactory对象？
	 */
	private ConfigurableListableBeanFactory beanFactory;

	/**
	 * Create a new AbstractXmlApplicationContext with no parent.
	 */
	public AbstractXmlApplicationContext() {
	}

	/**
	 * Create a new AbstractXmlApplicationContext with the given parent context.
	 * @param parent the parent context
	 */
	public AbstractXmlApplicationContext(ApplicationContext parent) {
		super(parent);
	}

	/**
	 * 刷新（构造）beanFactory对象
	 * 该方法会在所有初始化工作之前被调用
	 */
	protected final void refreshBeanFactory() throws BeansException {
		try {
			// 创建bean factory对象，默认是DefaultListableBeanFactory类型
			// 内部只是new了一个对象，并记录了父类bean factory对象、添加BeanFactory为忽略依赖类型，并没有其他操作
			// 此处可以看出，ApplicationContext确实是以BeanFactory为根基的
			DefaultListableBeanFactory beanFactory = createBeanFactory();

			// 根据bean factory对象，创建对应的xml bean定义读取工具，同时设置xml解析工具对象
			// 读取到的bean定义会被注册到给定的bean factory中
			XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
			beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

			// 初始化bean定义读取器（默认无实现）
			initBeanDefinitionReader(beanDefinitionReader);

			// 加载bean定义
			loadBeanDefinitions(beanDefinitionReader);
			this.beanFactory = beanFactory;
		}
		catch (IOException ex) {
			throw new ApplicationContextException("I/O error parsing XML document for application context [" + getDisplayName() + "]", ex);
		}
	}

	/**
	 * 获取beanFactory对象
	 */
	public final ConfigurableListableBeanFactory getBeanFactory() {
		return beanFactory;
	}

	/**
	 * 创建beanFactory对象
	 *
	 * 默认创建DefaultListableBeanFactory类型对象。
	 * 使用当前context对象的父对象，作为父bean factory。
	 * 可被子类覆写。
	 *
	 * @return 当前context对象的bean factory
	 */
	protected DefaultListableBeanFactory createBeanFactory() {
		return new DefaultListableBeanFactory(getInternalParentBeanFactory());
	}

	/**
	 * 初始化bean定义解析器，用于为context对象加载bean定义。
	 * 默认实现为空，可被子类重写，如用于屏蔽掉xml验证（setValidating），或使用一个不同的解析器实现类（setParserClass）。
	 *
	 * 这样做是为了便于子类的扩展，有点类似于简化版的前置处理器。子类覆写一个方法，即定义了一个前置处理器。
	 *
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader#setValidating
	 * @see org.springframework.beans.factory.xml.XmlBeanDefinitionReader#setParserClass
	 */
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader) {
	}

	/**
	 * 使用给定的读取器，读取bean定义信息，并注册进reader中的bean factory对象中
	 *
	 * bean factory的生命周期由调用该方法的refreshBeanFactory()方法管理，
	 * 因此该方法只负责加载、注册bean定义信息，其他的不需要管。
	 *
	 * @see #refreshBeanFactory
	 */
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
		// 获取配置文件路径位置
		String[] configLocations = getConfigLocations();
		if (configLocations != null) {
			// 获取资源路径解析器
			ResourcePatternResolver resourcePatternResolver = getResourcePatternResolver();
			for (int i = 0; i < configLocations.length; i++) {
				// 利用路径解析器，将路径解析为Resource实例对象
				Resource[] configResources = resourcePatternResolver.getResources(configLocations[i]);
				for (int j = 0; j < configResources.length; j++) {
					// 给reader中的beanFactory注册读取到的bean定义信息
					reader.loadBeanDefinitions(configResources[j]);
				}
			}
		}
	}

	/**
	 * 创建ResourcePatternResolver对象，用于解析并匹配路径，转为Resource实例对象。
	 * 默认类型是PathMatchingResourcePatternResolver，支持ant样式的路径。
	 * @see org.springframework.core.io.support.PathMatchingResourcePatternResolver
	 */
	protected ResourcePatternResolver getResourcePatternResolver() {
		return new PathMatchingResourcePatternResolver(this);
	}

	/**
	 * Return an array of resource locations, referring to the XML bean
	 * definition files that this context should be built with.
	 * @return an array of resource locations, or null if none
	 */
	protected abstract String[] getConfigLocations();

}
