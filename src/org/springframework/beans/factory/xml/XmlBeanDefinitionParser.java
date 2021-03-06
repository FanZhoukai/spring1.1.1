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

package org.springframework.beans.factory.xml;

import org.w3c.dom.Document;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.core.io.Resource;

/**
 * Strategy interface for parsing XML bean definitions.
 * Used by XmlBeanDefinitionReader for actually parsing a DOM document.
 *
 * <p>Instantiated per document to parse: Implementations can hold state in
 * instance variables during the execution of the registerBeanDefinitions
 * method, for example global settings that are defined for all bean
 * definitions in the document.
 *
 * @author Juergen Hoeller
 * @since 18.12.2003
 * @see XmlBeanDefinitionReader#setParserClass
 */
public interface XmlBeanDefinitionParser {

	/**
	 * 转化DOM中的bean定义，并注册进指定的beanFactory中。
	 * beanFactory包含在参数reader中。
	 */
	int registerBeanDefinitions(BeanDefinitionReader reader, Document doc, Resource resource)
			throws BeansException;

}
