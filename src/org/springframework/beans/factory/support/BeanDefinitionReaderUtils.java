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

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues;

/**
 * 读取bean定义工具类
 *
 * @see PropertiesBeanDefinitionReader
 * @see org.springframework.beans.factory.xml.DefaultXmlBeanDefinitionParser
 */
public class BeanDefinitionReaderUtils {

	/**
	 * 根据给定的类名、父定义、构造方法参数、属性值，
	 * 创建一个根定义（RootBeanDefinition）或子定义（ChildBeanDefinition）对象。
	 * 根据parent是否存在，来判断创建父/子定义对象。
	 *
	 * @param className   bean类名
	 * @param parent      父bean名称
	 * @param cargs       构造方法参数
	 * @param pvs         属性名称(property values)
	 * @param classLoader 用于加载bean类对象的类加载器，若只想根据名称注册bean的class对象，可为空
	 * @return bean定义
	 */
	public static AbstractBeanDefinition createBeanDefinition(
			String className, String parent, ConstructorArgumentValues cargs,
			MutablePropertyValues pvs, ClassLoader classLoader)
			throws ClassNotFoundException {

		Class beanClass = null;
		if (className != null && classLoader != null) {
			beanClass = Class.forName(className, true, classLoader);
		}

		if (parent == null) {
			if (beanClass != null) {
				return new RootBeanDefinition(beanClass, cargs, pvs);
			} else {
				return new RootBeanDefinition(className, cargs, pvs);
			}
		} else {
			if (beanClass != null) {
				return new ChildBeanDefinition(parent, beanClass, cargs, pvs);
			} else {
				return new ChildBeanDefinition(parent, className, cargs, pvs);
			}
		}
	}

}
