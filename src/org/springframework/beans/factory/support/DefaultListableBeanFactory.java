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

package org.springframework.beans.factory.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.StringUtils;

/**
 * BeanFactory的具体实现类。
 * 可当做独立的bean factory使用，也可以用作自定义bean factory的父类。
 *
 * 该类是BeanFactory接口下相对功能较完整的实现类，可以说是spring IoC模块的根基，也是使用最广泛的实现类。
 * 如果业务需要扩展BeanFactory，基本上都是继承这个类。
 * 该类功能的实现是通过实现不同接口来完成的，主要的功能性接口如下：
 *
 * 1. AbstractAutowireCapableBeanFactory：提供了自动注入的功能；
 * 2. ConfigurableListableBeanFactory：提供了对预实例化单例对象的功能；
 * 3. ListableBeanFactory：提供快速查找、统计bean实例的功能；
 * 4. BeanDefinitionRegistry：提供了注册bean的功能。
 */
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
    implements ConfigurableListableBeanFactory, BeanDefinitionRegistry {

	/* 是否允许重复注册bean */
	private boolean allowBeanDefinitionOverriding = true;

	/** bean定义对象map，结构：Map<String,BeanDefinition> */
	private final Map beanDefinitionMap = new HashMap();

	/** bean名称列表，按注册顺序排序 */
	private final List beanDefinitionNames = new LinkedList();


	/**
	 * Create a new DefaultListableBeanFactory.
	 */
	public DefaultListableBeanFactory() {
		super();
	}

	/**
	 * 根据指定的父bean factory对象，创建一个DefaultListableBeanFactory对象
	 */
	public DefaultListableBeanFactory(BeanFactory parentBeanFactory) {
		super(parentBeanFactory);
	}

	/**
	 * Set if it should be allowed to override bean definitions by registering a
	 * different definition with the same name, automatically replacing the former.
	 * If not, an exception will be thrown. Default is true.
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory
	//---------------------------------------------------------------------

	public int getBeanDefinitionCount() {
		return this.beanDefinitionMap.size();
	}

	public String[] getBeanDefinitionNames() {
		return getBeanDefinitionNames(null);
	}

	/**
	 * 找到符合指定类型的所有bean名称
	 * 符合类型是指，属于指定类及其子类，或是指定接口的实现类
	 */
	public String[] getBeanDefinitionNames(Class type) {
		List matches = new ArrayList();
		Iterator it = this.beanDefinitionNames.iterator();
		while (it.hasNext()) {
			String beanName = (String) it.next();
			if (isBeanDefinitionTypeMatch(beanName, type)) {
				matches.add(beanName);
			}
		}
		return (String[]) matches.toArray(new String[matches.size()]);
	}

	/**
	 * 判断bean名称对应的bean类型，是否属于给定的类及其子类（或给定接口的实现类）
	 */
	protected boolean isBeanDefinitionTypeMatch(String beanName, Class type) {
		if (type == null) {
			return true;
		}
		RootBeanDefinition rbd = getMergedBeanDefinition(beanName, false);
		return (rbd.hasBeanClass() && type.isAssignableFrom(rbd.getBeanClass()));
	}

	/**
	 * 判断是否包含一个bean的定义信息
	 */
	public boolean containsBeanDefinition(String beanName) {
		return this.beanDefinitionMap.containsKey(beanName);
	}

	public Map getBeansOfType(Class type, boolean includePrototypes, boolean includeFactoryBeans)
			throws BeansException {

		String[] beanNames = getBeanDefinitionNames(type);
		Map result = new HashMap();
		for (int i = 0; i < beanNames.length; i++) {
			if (includePrototypes || isSingleton(beanNames[i])) {
				try {
					result.put(beanNames[i], getBean(beanNames[i]));
				}
				catch (BeanCurrentlyInCreationException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring match to currently created bean '" + beanNames[i] + "'");
					}
					// ignore
				}
				catch (BeanIsAbstractException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring match to abstract bean definition '" + beanNames[i] + "'");
					}
					// ignore
				}
			}
		}

		String[] singletonNames = getSingletonNames(type);
		for (int i = 0; i < singletonNames.length; i++) {
			if (!containsBeanDefinition(singletonNames[i])) {
				// directly registered singleton
				try {
					result.put(singletonNames[i], getBean(singletonNames[i]));
				}
				catch (BeanCurrentlyInCreationException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring match to currently created bean '" + singletonNames[i] + "'");
					}
					// ignore
				}
			}
		}

		if (includeFactoryBeans) {
			String[] factoryNames = getBeanDefinitionNames(FactoryBean.class);
			for (int i = 0; i < factoryNames.length; i++) {
				try {
					FactoryBean factory = (FactoryBean) getBean(FACTORY_BEAN_PREFIX + factoryNames[i]);
					Class objectType = factory.getObjectType();
					if ((objectType == null && factory.isSingleton()) ||
							((factory.isSingleton() || includePrototypes) &&
							objectType != null && type.isAssignableFrom(objectType))) {
						Object createdObject = getBean(factoryNames[i]);
						if (type.isInstance(createdObject)) {
							result.put(factoryNames[i], createdObject);
						}
					}
				}
				catch (BeanCurrentlyInCreationException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring match to currently created bean '" + factoryNames[i] + "'");
					}
					// ignore
				}
				catch (BeanIsAbstractException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring match to abstract bean definition '" + beanNames[i] + "'");
					}
					// ignore
				}
				catch (BeanCreationException ex) {
					// We're currently creating that FactoryBean.
					// Sensible to ignore it, as we are just looking for a certain type.
					if (logger.isDebugEnabled()) {
						logger.debug("Ignoring FactoryBean creation failure when looking for matching beans", ex);
					}
					// ignore
				}
			}
		}

		return result;
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableListableBeanFactory
	//---------------------------------------------------------------------

	/**
	 * 初始化全部单例对象（懒加载除外），同时考虑FactoryBean的情况。
	 * 如果失败，销毁所有已创建的单例对象，以避免挂起资源。
	 */
	public void preInstantiateSingletons() throws BeansException {
		try {
			for (Iterator it = this.beanDefinitionNames.iterator(); it.hasNext();) {
				String beanName = (String) it.next();
				// 包含bean定义信息，才允许继续创建
				if (containsBeanDefinition(beanName)) {
					// 获取融合后的bean定义对象。其中融合是指递归包含所有parent属性指代的bean定义信息
					RootBeanDefinition bd = getMergedBeanDefinition(beanName, false);
					// 满足以下条件才会在此步创建bean：有bean类型、非抽象、是单例、非懒加载
					if (bd.hasBeanClass() && !bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
						// 若属于FactoryBean，首先获取到这个工厂bean后，再获取这个bean
						// （因为获取bean时会识别出它是由工厂bean创建的，所以需要首先把工厂bean创建出来）
						if (FactoryBean.class.isAssignableFrom(bd.getBeanClass())) {
							FactoryBean factory = (FactoryBean) getBean(FACTORY_BEAN_PREFIX + beanName);
							// factory bean下，需要通过FactoryBean来判断这个bean是否单例
							if (factory.isSingleton()) {
								getBean(beanName);
							}
						} else {
							// 非FactoryBean，普通bean直接创建即可
							getBean(beanName);
						}
					}
				}
			}
		}
		catch (BeansException ex) {
			// 销毁所有已创建的单例对象，以避免挂起资源
			try {
				destroySingletons();
			}
			catch (Throwable ex2) {
				logger.error("preInstantiateSingletons failed but couldn't destroy already created singletons", ex2);
			}
			throw ex;
		}
	}


	//---------------------------------------------------------------------
	// Implementation of BeanDefinitionRegistry
	//---------------------------------------------------------------------

	/**
	 * 将bean实例注册进beanFactory中
	 * 当前对象就是beanFactory，即注册进当前对象的beanDefinitionMap中
	 */
	public void registerBeanDefinition(String name, BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
		// bean定义验证
		if (beanDefinition instanceof AbstractBeanDefinition) {
			try {
				((AbstractBeanDefinition) beanDefinition).validate();
			}
			catch (BeanDefinitionValidationException ex) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), name, "Validation of bean definition with name failed", ex);
			}
		}
		// 若已有重名的bean定义，验证是否允许覆盖
		Object oldBeanDefinition = this.beanDefinitionMap.get(name);
		if (oldBeanDefinition != null) {
			if (!this.allowBeanDefinitionOverriding) {
				throw new BeanDefinitionStoreException("Cannot register bean definition [" + beanDefinition + "] for bean '" + name + "': there's already [" + oldBeanDefinition + "] bound");
			}
		} else {
			this.beanDefinitionNames.add(name);
		}
		// 注册进map中
		this.beanDefinitionMap.put(name, beanDefinition);
	}


	//---------------------------------------------------------------------
	// Implementation of superclass abstract methods
	//---------------------------------------------------------------------

    /**
     * 根据bean名称获取bean定义
     * （直接从map中取）
     */
	public BeanDefinition getBeanDefinition(String beanName) throws BeansException {
		BeanDefinition bd = (BeanDefinition) this.beanDefinitionMap.get(beanName);
		if (bd == null) {
			throw new NoSuchBeanDefinitionException(beanName, toString());
		}
		return bd;
	}

	protected Map findMatchingBeans(Class requiredType) {
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(this, requiredType, true, true);
	}


	public String toString() {
		StringBuffer sb = new StringBuffer(getClass().getName());
		sb.append(" defining beans [" + StringUtils.arrayToDelimitedString(getBeanDefinitionNames(), ",") + "]");
		if (getParentBeanFactory() == null) {
			sb.append("; Root of BeanFactory hierarchy");
		}
		else {
			sb.append("; parent=<" + getParentBeanFactory() + ">");
		}
		return sb.toString();
	}

}
