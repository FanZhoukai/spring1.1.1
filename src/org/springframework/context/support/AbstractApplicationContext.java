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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.propertyeditors.InputStreamEditor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.OrderComparator;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;

/**
 * Partial implementation of ApplicationContext. Doesn't mandate the type
 * of storage used for configuration, but implements common functionality.
 * Uses the Template Method design pattern, requiring concrete subclasses
 * to implement abstract methods.
 *
 * <p>In contrast to a plain bean factory, an ApplicationContext is supposed
 * to detect special beans defined in its bean factory: Therefore, this class
 * automatically registers BeanFactoryPostProcessors, BeanPostProcessors
 * and ApplicationListeners that are defined as beans in the context.
 *
 * <p>A MessageSource may be also supplied as a bean in the context, with
 * the name "messageSource". Else, message resolution is delegated to the
 * parent context.
 *
 * <p>Implements resource loading through extending DefaultResourceLoader.
 * Therefore, treats resource paths as class path resources. Only supports
 * full classpath resource names that include the package path, like
 * "mypackage/myresource.dat".
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since January 21, 2001
 * @see #refreshBeanFactory
 * @see #getBeanFactory
 * @see #MESSAGE_SOURCE_BEAN_NAME
 */
public abstract class AbstractApplicationContext extends DefaultResourceLoader
		implements ConfigurableApplicationContext {

	/**
	 * Name of the MessageSource bean in the factory.
	 * If none is supplied, message resolution is delegated to the parent.
	 * @see MessageSource
	 */
	public static final String MESSAGE_SOURCE_BEAN_NAME = "messageSource";

	/**
	 * ApplicationEventMulticaster类的bean名称
	 *
	 * @see SimpleApplicationEventMulticaster 默认实现类
	 */
	public static final String APPLICATION_EVENT_MULTICASTER_BEAN_NAME = "applicationEventMulticaster";

	static {
		// Eagerly load the ContextClosedEvent class to avoid weird classloader issues
		// on application shutdown in WebLogic 8.1. (Reported by Dustin Woods.)
		ContextClosedEvent.class.getName();
	}


	//---------------------------------------------------------------------
	// Instance data
	//---------------------------------------------------------------------

	/** Logger used by this class. Available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Parent context */
	private ApplicationContext parent;

	/** BeanFactoryPostProcessors to apply on refresh */
	private final List beanFactoryPostProcessors = new ArrayList();

	/** Display name */
	private String displayName = getClass().getName() + ";hashCode=" + hashCode();

	/** System time in milliseconds when this context started */
	private long startupTime;

	/** MessageSource we delegate our implementation of this interface to */
	private MessageSource messageSource;

	/** Helper class used in event publishing */
	private ApplicationEventMulticaster applicationEventMulticaster;


	//---------------------------------------------------------------------
	// Constructors
	//---------------------------------------------------------------------

	/**
	 * Create a new AbstractApplicationContext with no parent.
	 */
	public AbstractApplicationContext() {
	}

	/**
	 * Create a new AbstractApplicationContext with the given parent context.
	 * @param parent the parent context
	 */
	public AbstractApplicationContext(ApplicationContext parent) {
		this.parent = parent;
	}


	//---------------------------------------------------------------------
	// Implementation of ApplicationContext
	//---------------------------------------------------------------------

	/**
	 * Return the parent context, or null if there is no parent
	 * (that is, this context is the root of the context hierarchy).
	 */
	public ApplicationContext getParent() {
		return parent;
	}

	/**
	 * Set a friendly name for this context.
	 * Typically done during initialization of concrete context implementations.
	 */
	protected void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Return a friendly name for this context.
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Return the timestamp (ms) when this context was first loaded.
	 */
	public long getStartupDate() {
		return startupTime;
	}

	/**
	 * Publish the given event to all listeners.
	 * <p>Note: Listeners get initialized after the MessageSource, to be able
	 * to access it within listener implementations. Thus, MessageSource
	 * implementation cannot publish events.
	 * @param event event to publish (may be application-specific or a
	 * standard framework event)
	 */
	public void publishEvent(ApplicationEvent event) {
		if (logger.isDebugEnabled()) {
			logger.debug("Publishing event in context [" + getDisplayName() + "]: " + event.toString());
		}
		this.applicationEventMulticaster.multicastEvent(event);
		if (this.parent != null) {
			this.parent.publishEvent(event);
		}
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableApplicationContext
	//---------------------------------------------------------------------

	public void setParent(ApplicationContext parent) {
		this.parent = parent;
	}

	public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor beanFactoryPostProcessor) {
		this.beanFactoryPostProcessors.add(beanFactoryPostProcessor);
	}

	/**
	 * Return the list of BeanPostProcessors that will get applied
	 * to beans created with this factory.
	 */
	public List getBeanFactoryPostProcessors() {
		return beanFactoryPostProcessors;
	}

	/**
     * 加载或刷新配置文件
	 */
	public void refresh() throws BeansException {
		this.startupTime = System.currentTimeMillis();

		// 让子类完成刷新内部beanFactory的操作
		// 只是读取了bean定义信息，并没有初始化bean
		refreshBeanFactory();

		ConfigurableListableBeanFactory beanFactory = getBeanFactory();

		// TODO fzk start
		/*
		// 配置beanFactory上下文语义，即一些通用配置信息
		// 包括：自定义的属性设置、后置处理器、忽略依赖类型
		beanFactory.registerCustomEditor(Resource.class, new ResourceEditor(this));
		beanFactory.registerCustomEditor(InputStream.class, new InputStreamEditor(new ResourceEditor(this)));
		beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
		beanFactory.ignoreDependencyType(ResourceLoader.class);
		beanFactory.ignoreDependencyType(ApplicationContext.class);

		// allows post-processing of the bean factory in context subclasses
		postProcessBeanFactory(beanFactory);

		// 调用beanFactory的后置处理器（注意不是bean的，而是当前beanFactory对象的）
		for (Iterator it = getBeanFactoryPostProcessors().iterator(); it.hasNext();) {
			BeanFactoryPostProcessor factoryProcessor = (BeanFactoryPostProcessor) it.next();
			factoryProcessor.postProcessBeanFactory(beanFactory);
		}

		// 调用所有bean的BeanFactoryPostProcessor类型的后置处理器
		invokeBeanFactoryPostProcessors();
		*/
		// TODO fzk end

		// 注册bean后置处理器，拦截bean的创建过程
		registerBeanPostProcessors();

		// TODO fzk start
		/*
		// 为当前上下文初始化message source
		initMessageSource();

        // 【不理解】初始化事件监听器
		// initialize event multicaster for this context
		initApplicationEventMulticaster();

		// 初始化指定上下文子类的其他特殊bean
		// initialize other special beans in specific context subclasses
		onRefresh();

		// 检查并注册监听器bean
		refreshListeners();
		*/
		// TODO fzk end

		// 初始化单例实例
		// 这么晚才初始化，是为了让它们能访问到message source
		beanFactory.preInstantiateSingletons();

		// TODO fzk start
		/*
		// 最后一步：发布对应的事件
		publishEvent(new ContextRefreshedEvent(this));
		*/
        // TODO fzk end
	}

	/**
	 * Modify the application context's internal bean factory after its standard
	 * initialization. All bean definitions will have been loaded, but no beans
	 * will have been instantiated yet. This allows for registering special
	 * BeanPostProcessors etc in certain ApplicationContext implementations.
	 * @param beanFactory the bean factory used by the application context
	 * @throws org.springframework.beans.BeansException in case of errors
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
	}

	/**
	 * Instantiate and invoke all registered BeanFactoryPostProcessor beans,
	 * respecting explicit order if given.
	 * Must be called before singleton instantiation.
	 */
	private void invokeBeanFactoryPostProcessors() throws BeansException {
		String[] beanNames = getBeanDefinitionNames(BeanFactoryPostProcessor.class);
		BeanFactoryPostProcessor[] factoryProcessors = new BeanFactoryPostProcessor[beanNames.length];
		for (int i = 0; i < beanNames.length; i++) {
			factoryProcessors[i] = (BeanFactoryPostProcessor) getBean(beanNames[i]);
		}
		Arrays.sort(factoryProcessors, new OrderComparator());
		for (int i = 0; i < factoryProcessors.length; i++) {
			BeanFactoryPostProcessor factoryProcessor = factoryProcessors[i];
			factoryProcessor.postProcessBeanFactory(getBeanFactory());
		}
	}

	/**
	 * 实例化所有注册的后置处理器bean，并排序。
	 * 将在所有其他业务bean实例化之前被调用。
	 *
	 * 后置处理器也是bean，只是实现了BeanPostProcessor而已。
	 * ApplicationContext的机制保证了：后置处理器bean将在业务bean之前被注册进IoC容器中（该方法注册后置处理器bean，
	 * preInstantiateSingletons方法注册业务bean），因此才会保证，后置处理器应用于 “其他所有” bean的初始化进程。
	 */
	private void registerBeanPostProcessors() throws BeansException {
		// 获取所有后置处理器bean名称（根据类型获取）
		String[] beanNames = getBeanDefinitionNames(BeanPostProcessor.class);
		if (beanNames.length > 0) {
			// 实例化所有后置处理器bean，构造临时list
			List beanProcessors = new ArrayList();
			for (int i = 0; i < beanNames.length; i++) {
				beanProcessors.add(getBean(beanNames[i]));
			}
			// 根据order排序
			// order值越小，优先级越高
			// order值越小，排名越靠前，则越先被注册到后置处理器list中，在拦截bean实例时更先被执行
			// 注意，后置处理器和AOP都可以实现Ordered接口，但二者顺序不同：
			// 后置处理器    AOP
			// order=1      order=1
			// order=2      order=2
			// init         invoke
			// order=1      order=2
			// order=2      order=1
			//
			// 后置处理器是顺序执行的，无论初始化前还是初始化后，都按order从小到大顺序执行；
			// 而AOP采用一种同心圆的方式，order值越小，处于同心圆的更外圈，影响力越大。
			Collections.sort(beanProcessors, new OrderComparator());

			// 添加进beanFactory的后置处理器list中
			for (Iterator it = beanProcessors.iterator(); it.hasNext();) {
				getBeanFactory().addBeanPostProcessor((BeanPostProcessor) it.next());
			}
		}
	}

	/**
	 * Initialize the MessageSource.
	 * Use parent's if none defined in this context.
	 */
	private void initMessageSource() throws BeansException {
		try {
			this.messageSource = (MessageSource) getBean(MESSAGE_SOURCE_BEAN_NAME);
			// Set parent message source if applicable,
			// and if the message source is defined in this context, not in a parent.
			if (this.parent != null && (this.messageSource instanceof HierarchicalMessageSource) &&
			    containsBeanDefinition(MESSAGE_SOURCE_BEAN_NAME)) {
				((HierarchicalMessageSource) this.messageSource).setParentMessageSource(
				    getInternalParentMessageSource());
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			logger.info("No MessageSource found for context [" + getDisplayName() + "]: using empty default");
			// use empty message source to be able to accept getMessage calls
			this.messageSource = new StaticMessageSource();
		}
	}

	/**
     * 初始化applicationEventMulticaster
     * 默认类型是SimpleApplicationEventMulticaster
	 */
	private void initApplicationEventMulticaster() throws BeansException {
		try {
			this.applicationEventMulticaster =
					(ApplicationEventMulticaster) getBean(APPLICATION_EVENT_MULTICASTER_BEAN_NAME);
		}
		catch (NoSuchBeanDefinitionException ex) {
			logger.info("No ApplicationEventMulticaster found for context [" + getDisplayName() + "]: using default");
			this.applicationEventMulticaster = new SimpleApplicationEventMulticaster();
		}
	}

	/**
	 * Template method which can be overridden to add context-specific refresh work.
	 * Called on initialization of special beans, before instantiation of singletons.
	 * @throws BeansException in case of errors during refresh
	 * @see #refresh
	 */
	protected void onRefresh() throws BeansException {
		// for subclasses: do nothing by default
	}

	/**
	 * Add beans that implement ApplicationListener as listeners.
	 * Doesn't affect other listeners, which can be added without being beans.
	 */
	private void refreshListeners() throws BeansException {
		logger.info("Refreshing listeners");
		Collection listeners = getBeansOfType(ApplicationListener.class, true, false).values();
		logger.debug("Found " + listeners.size() + " listeners in bean factory");
		for (Iterator it = listeners.iterator(); it.hasNext();) {
			ApplicationListener listener = (ApplicationListener) it.next();
			addListener(listener);
			if (logger.isInfoEnabled()) {
				logger.info("Application listener [" + listener + "] added");
			}
		}
	}

	/**
	 * Subclasses can invoke this method to register a listener.
	 * Any beans in the context that are listeners are automatically added.
	 * @param listener the listener to register
	 */
	protected void addListener(ApplicationListener listener) {
		this.applicationEventMulticaster.addApplicationListener(listener);
	}

	/**
	 * Destroy the singletons in the bean factory of this application context.
	 */
	public void close() {
		logger.info("Closing application context [" + getDisplayName() + "]");

		// Destroy all cached singletons in this context,
		// invoking DisposableBean.destroy and/or "destroy-method".
		getBeanFactory().destroySingletons();

		// publish corresponding event
		publishEvent(new ContextClosedEvent(this));
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory
	//---------------------------------------------------------------------

    /**
     * 根据bean名称获取bean实例
     */
	public Object getBean(String name) throws BeansException {
		return getBeanFactory().getBean(name);
	}

	public Object getBean(String name, Class requiredType) throws BeansException {
		return getBeanFactory().getBean(name, requiredType);
	}

	public boolean containsBean(String name) {
		return getBeanFactory().containsBean(name);
	}

	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().isSingleton(name);
	}

	public String[] getAliases(String name) throws NoSuchBeanDefinitionException {
		return getBeanFactory().getAliases(name);
	}


	//---------------------------------------------------------------------
	// Implementation of ListableBeanFactory
	//---------------------------------------------------------------------

	public int getBeanDefinitionCount() {
		return getBeanFactory().getBeanDefinitionCount();
	}

	public String[] getBeanDefinitionNames() {
		return getBeanFactory().getBeanDefinitionNames();
	}

	/**
	 * 找到符合指定类型的所有bean名称
	 * 符合类型是指，属于指定类及其子类，或是指定接口的实现类
	 */
	public String[] getBeanDefinitionNames(Class type) {
		return getBeanFactory().getBeanDefinitionNames(type);
	}

	public boolean containsBeanDefinition(String name) {
		return getBeanFactory().containsBeanDefinition(name);
	}

	public Map getBeansOfType(Class type, boolean includePrototypes, boolean includeFactoryBeans)
			throws BeansException {
		return getBeanFactory().getBeansOfType(type, includePrototypes, includeFactoryBeans);
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory
	//---------------------------------------------------------------------

	public BeanFactory getParentBeanFactory() {
		return getParent();
	}

	/**
	 * 该方法的作用是，获取父对象的BeanFactory。
	 *
	 * 由于ApplicationContext继承了BeanFactory，因此对于一般的context对象，直接强转即可。
	 * 但由于ConfigurableApplicationContext类比较特殊，提供了getBeanFactory()方法，
	 * 即它的实现类内部需要维护了一个bean factory对象，因此需要手动调用一下这个方法。
	 *
	 * 转成易读的伪代码：
	 * if(parent instanceof 特殊类型) {
	 *     return ((特殊类型) parent).getBeanFactory(); // 强转为特殊类调用特殊方法
	 * } else {
	 *     return (BeanFactory) parent; // 将ApplicationContext退化为BeanFactory类型返回
	 * }
	 *
	 * @see org.springframework.context.ConfigurableApplicationContext#getBeanFactory
	 */
	protected BeanFactory getInternalParentBeanFactory() {
		return (getParent() instanceof ConfigurableApplicationContext) ?
				((ConfigurableApplicationContext) getParent()).getBeanFactory() : (BeanFactory) getParent();
	}


	//---------------------------------------------------------------------
	// Implementation of MessageSource
	//---------------------------------------------------------------------

	public String getMessage(String code, Object args[], String defaultMessage, Locale locale) {
		return this.messageSource.getMessage(code, args, defaultMessage, locale);
	}

	public String getMessage(String code, Object args[], Locale locale) throws NoSuchMessageException {
		return this.messageSource.getMessage(code, args, locale);
	}

	public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
		return this.messageSource.getMessage(resolvable, locale);
	}

	/**
	 * Return the internal message source of the parent context if it is an
	 * AbstractApplicationContext too; else, return the parent context itself.
	 */
	protected MessageSource getInternalParentMessageSource() {
		return (getParent() instanceof AbstractApplicationContext) ?
		    ((AbstractApplicationContext) getParent()).messageSource : getParent();
	}


	//---------------------------------------------------------------------
	// Abstract methods that must be implemented by subclasses
	//---------------------------------------------------------------------

	/**
	 * 子类必须实现此方法，用于实际刷新（构造）beanFactory对象
	 * 该方法会在所有初始化工作之前被调用
	 */
	protected abstract void refreshBeanFactory() throws BeansException;

	/**
	 * 子类必须在这里返回内部bean factory
	 * 该方法的实现尽量高效，以便于被重复调用，避免性能损耗
	 */
	public abstract ConfigurableListableBeanFactory getBeanFactory();


	/**
	 * Return information about this context.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer(getClass().getName());
		sb.append(": ");
		sb.append("displayName=[").append(this.displayName).append("]; ");
		sb.append("startup date=[").append(new Date(this.startupTime)).append("]; ");
		if (this.parent == null) {
			sb.append("root of ApplicationContext hierarchy");
		}
		else {
			sb.append("parent=[").append(this.parent).append(']');
		}
		return sb.toString();
	}

}
