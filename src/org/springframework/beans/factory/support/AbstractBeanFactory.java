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

import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanIsAbstractException;
import org.springframework.beans.factory.BeanIsNotAFactoryException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanCircularReferenceException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * Abstract superclass for BeanFactory implementations.
 * Implements the ConfigurableBeanFactory SPI interface.
 *
 * <p>This class provides singleton/prototype determination, singleton cache,
 * aliases, FactoryBean handling, and bean definition merging for child bean
 * definitions. It also allows for management of a bean factory hierarchy,
 * implementing the HierarchicalBeanFactory interface.
 *
 * <p>The main template methods to be implemented by subclasses are
 * getBeanDefinition and createBean, retrieving a bean definition for
 * a given bean name respectively creating a bean instance for a given
 * bean definition.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 15 April 2001
 * @see #getBeanDefinition
 * @see #createBean
 * @see #destroyBean
 */
public abstract class AbstractBeanFactory implements ConfigurableBeanFactory {

	/**
	 * 用于区别一个factory bean和一个利用bean factory创建的普通bean。
	 * 例如，名称为"myEjb"的bean是一个工厂bean，那么使用"&myEjb"就会得到这个工厂bean本身，而不是这个工厂产生的实例。
	 */
	public static final String FACTORY_BEAN_PREFIX = "&";

    /**
     * 实例化bean时，注册进单例缓存的临时标记对象。
     * 表明当前bean正处于创建之中（in creation），用于监测循环依赖问题
     */
	private static final Object CURRENTLY_IN_CREATION = new Object();


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 父bean factory, 用于支持bean的继承关系 */
	private BeanFactory parentBeanFactory;

	/** Custom PropertyEditors to apply to the beans of this factory */
	private Map customEditors = new HashMap();

	/** 在依赖检查或注入时，需要被忽略的依赖类型 */
	private final Set ignoreDependencyTypes = new HashSet();

	/** BeanPostProcessors to apply in createBean */
	private final List beanPostProcessors = new ArrayList();

	/** bean正式名称的别名map，key:别名; value:正式名称 */
	private final Map aliasMap = Collections.synchronizedMap(new HashMap());

    /**
     * 单例bean的缓存map，结构：Map<String, Object>：bean名称:bean实例
     */
	private final Map singletonCache = Collections.synchronizedMap(new HashMap());


	/**
	 * 根据指定的父bean factory对象，创建一个AbstractBeanFactory对象
	 */
	public AbstractBeanFactory() {
		ignoreDependencyType(BeanFactory.class);
	}

	/**
	 * 根据指定的父bean factory对象，创建一个AbstractBeanFactory对象
	 * @param parentBeanFactory 父bean factory，可为空
	 */
	public AbstractBeanFactory(BeanFactory parentBeanFactory) {
		this();
		this.parentBeanFactory = parentBeanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of BeanFactory
	//---------------------------------------------------------------------

    /**
     * 根据bean名称获取bean实例
     * 若找不到，则检查父beanFactory
     */
	public Object getBean(String name) throws BeansException {
		return getBean(name, (Object[]) null);
	}
		
    /**
     * 根据bean名称获取bean实例
     * 若找不到，则检查父beanFactory
     *
     * @param args 若bean是prototype作用域的，静态工厂方法需要的参数。除此之外此参数必须为null
     *
     * TODO: We could consider supporting this for constructor args also, but it's really a
     * corner case required for AspectJ integration.
     */
	public Object getBean(String name, Object[] args) throws BeansException {
		String beanName = transformedBeanName(name);

		// 检查缓存的单例map（为了获取手动注册的单例bean）
		Object sharedInstance = this.singletonCache.get(beanName);
		// 【设计模式】双重检查的单例模式，第一重检查
		if (sharedInstance != null) {
			// 若bean正处于创建过程中，抛出异常
			if (sharedInstance == CURRENTLY_IN_CREATION) {
				throw new BeanCurrentlyInCreationException(beanName, "Requested bean is already currently in creation");
			}
			// 直接返回bean实例，顺便处理factory bean的情况
			return getObjectForSharedInstance(name, sharedInstance);
		}

		// 缓存map中不存在，首先检查bean定义是否存在
        RootBeanDefinition mergedBeanDefinition = null;
        try {
            mergedBeanDefinition = getMergedBeanDefinition(beanName, false);
        }
        catch (NoSuchBeanDefinitionException ex) {
            // 找不到此bean定义，那就让父beanFactory创建一个bean
            if (this.parentBeanFactory != null) {
                return this.parentBeanFactory.getBean(name);
            }
            throw ex;
        }

        // bean定义必须是非抽象的
        if (mergedBeanDefinition.isAbstract()) {
            throw new BeanIsAbstractException(name);
        }

        // 检查args参数的可用性。仅适用于prototype作用域的、通过工厂方法构造的bean。
        if (args != null) {
            if (mergedBeanDefinition.isSingleton()) {
                throw new BeanDefinitionStoreException("Cannot specify arguments in the getBean() method when referring to a singleton bean definition");
            } else if (mergedBeanDefinition.getFactoryMethodName() == null) {
                throw new BeanDefinitionStoreException("Can only specify arguments in the getBean() method in conjunction with a factory method");
            }
        }

        // 创建bean实例
        if (mergedBeanDefinition.isSingleton()) {
            // 【设计模式】双重检查的单例模式实例
            synchronized (this.singletonCache) {
                // 双重检查的单例模式，锁内第二重检查
                sharedInstance = this.singletonCache.get(beanName);
                if (sharedInstance == null) {
                	// 将临时对象放入缓存，以解决循环依赖问题
                    this.singletonCache.put(beanName, CURRENTLY_IN_CREATION);
                    try {
                    	// 创建bean实例，放入缓存map中，替代原有的临时对象
                        sharedInstance = createBean(beanName, mergedBeanDefinition, args);
                        this.singletonCache.put(beanName, sharedInstance);
                    }
                    catch (BeansException ex) {
                        this.singletonCache.remove(beanName);
                        throw ex;
                    }
                }
            }
            return getObjectForSharedInstance(name, sharedInstance);
        }
        else {
            // prototype
            return createBean(name, mergedBeanDefinition, args);
        }
	}

	public Object getBean(String name, Class requiredType) throws BeansException {
		Object bean = getBean(name);
		if (!requiredType.isAssignableFrom(bean.getClass())) {
			throw new BeanNotOfRequiredTypeException(name, requiredType, bean);
		}
		return bean;
	}

	public boolean containsBean(String name) {
		String beanName = transformedBeanName(name);
		if (this.singletonCache.containsKey(beanName)) {
			return true;
		}
		if (containsBeanDefinition(beanName)) {
			return true;
		}
		else {
			// not found -> check parent
			if (this.parentBeanFactory != null) {
				return this.parentBeanFactory.containsBean(beanName);
			}
			else {
				return false;
			}
		}
	}

	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);
		try {
			Class beanClass = null;
			boolean singleton = true;
			Object beanInstance = this.singletonCache.get(beanName);
			if (beanInstance != null) {
				beanClass = beanInstance.getClass();
				singleton = true;
			}
			else {
				RootBeanDefinition bd = getMergedBeanDefinition(beanName, false);
				if (bd.hasBeanClass()) {
					beanClass = bd.getBeanClass();
				}
				singleton = bd.isSingleton();
			}
			// in case of FactoryBean, return singleton status of created object if not a dereference
			if (beanClass != null && FactoryBean.class.isAssignableFrom(beanClass) && !isFactoryDereference(name)) {
				FactoryBean factoryBean = (FactoryBean) getBean(FACTORY_BEAN_PREFIX + beanName);
				return factoryBean.isSingleton();
			}
			else {
				return singleton;
			}
		}
		catch (NoSuchBeanDefinitionException ex) {
			// not found -> check parent
			if (this.parentBeanFactory != null) {
				return this.parentBeanFactory.isSingleton(beanName);
			}
			throw ex;
		}
	}

	public String[] getAliases(String name) throws NoSuchBeanDefinitionException {
		String beanName = transformedBeanName(name);
		// check if bean actually exists in this bean factory
		if (this.singletonCache.containsKey(beanName) || containsBeanDefinition(beanName)) {
			// if found, gather aliases
			List aliases = new ArrayList();
			synchronized (this.aliasMap) {
				for (Iterator it = this.aliasMap.entrySet().iterator(); it.hasNext();) {
					Map.Entry entry = (Map.Entry) it.next();
					if (entry.getValue().equals(beanName)) {
						aliases.add(entry.getKey());
					}
				}
			}
			return (String[]) aliases.toArray(new String[aliases.size()]);
		}
		else {
			// not found -> check parent
			if (this.parentBeanFactory != null) {
				return this.parentBeanFactory.getAliases(beanName);
			}
			throw new NoSuchBeanDefinitionException(beanName, toString());
		}
	}


	//---------------------------------------------------------------------
	// Implementation of HierarchicalBeanFactory
	//---------------------------------------------------------------------

	public BeanFactory getParentBeanFactory() {
		return parentBeanFactory;
	}


	//---------------------------------------------------------------------
	// Implementation of ConfigurableBeanFactory
	//---------------------------------------------------------------------

	public void setParentBeanFactory(BeanFactory parentBeanFactory) {
		this.parentBeanFactory = parentBeanFactory;
	}

	public void registerCustomEditor(Class requiredType, PropertyEditor propertyEditor) {
		this.customEditors.put(requiredType, propertyEditor);
	}

	/**
	 * Return the map of custom editors, with Classes as keys
	 * and PropertyEditors as values.
	 */
	public Map getCustomEditors() {
		return customEditors;
	}

	/**
	 * 添加需要忽略的依赖类型
	 */
	public void ignoreDependencyType(Class type) {
		this.ignoreDependencyTypes.add(type);
	}

	/**
	 * Return the set of classes that will get ignored for autowiring.
	 */
	public Set getIgnoredDependencyTypes() {
		return ignoreDependencyTypes;
	}

	public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
		this.beanPostProcessors.add(beanPostProcessor);
	}

	/**
	 * Return the list of BeanPostProcessors that will get applied
	 * to beans created with this factory.
	 */
	public List getBeanPostProcessors() {
		return beanPostProcessors;
	}

	/**
	 * 根据bean名称，创建别名
	 */
	public void registerAlias(String beanName, String alias) throws BeanDefinitionStoreException {
		synchronized (this.aliasMap) {
			Object registeredName = this.aliasMap.get(alias);
			if (registeredName != null) {
				throw new BeanDefinitionStoreException("Cannot register alias '" + alias + "' for bean name '" + beanName +
						"': it's already registered for bean name '" + registeredName + "'");
			}
			this.aliasMap.put(alias, beanName);
		}
	}

	public void registerSingleton(String beanName, Object singletonObject) throws BeanDefinitionStoreException {
		synchronized (this.singletonCache) {
			Object oldObject = this.singletonCache.get(beanName);
			if (oldObject != null) {
				throw new BeanDefinitionStoreException("Could not register object [" + singletonObject +
						"] under bean name '" + beanName + "': there's already object [" + oldObject + " bound");
			}
			addSingleton(beanName, singletonObject);
		}
	}

	/**
	 * Add the given singleton object to the singleton cache of this factory.
	 * <p>To be called for eager registration of singletons, e.g. to be able to
	 * resolve circular references.
	 * @param beanName the name of the bean
	 * @param singletonObject the singleton object
	 */
	protected void addSingleton(String beanName, Object singletonObject) {
		this.singletonCache.put(beanName, singletonObject);
	}

	/**
	 * Remove the bean with the given name from the singleton cache of this factory.
	 * <p>To be able to clean up eager registration of a singleton if creation failed.
	 * @param beanName the name of the bean
	 */
	protected void removeSingleton(String beanName) {
		this.singletonCache.remove(beanName);
	}

	public void destroySingletons() {
		if (logger.isInfoEnabled()) {
			logger.info("Destroying singletons in factory {" + this + "}");
		}
		synchronized (this.singletonCache) {
			Set singletonCacheKeys = new HashSet(this.singletonCache.keySet());
			for (Iterator it = singletonCacheKeys.iterator(); it.hasNext();) {
				destroySingleton((String) it.next());
			}
		}
	}

	/**
	 * Destroy the given bean. Delegates to destroyBean if a corresponding
	 * singleton instance is found.
	 * @param beanName name of the bean
	 * @see #destroyBean
	 */
	protected final void destroySingleton(String beanName) {
		Object singletonInstance = this.singletonCache.remove(beanName);
		if (singletonInstance != null) {
			destroyBean(beanName, singletonInstance);
		}
	}


	//---------------------------------------------------------------------
	// Implementation methods
	//---------------------------------------------------------------------

    /**
     * 去除特殊前缀、别名转换为正式名
     */
	protected String transformedBeanName(String name) throws NoSuchBeanDefinitionException {
		if (name == null) {
			throw new NoSuchBeanDefinitionException(name, "Cannot get bean with null name");
		}
		if (name.startsWith(FACTORY_BEAN_PREFIX)) {
			name = name.substring(FACTORY_BEAN_PREFIX.length());
		}
		// 处理别名，若有，则返回对应正式名
		String canonicalName = (String) this.aliasMap.get(name);
		return canonicalName != null ? canonicalName : name;
	}

    /**
     * 判断是否是一个factory bean名称（即以特殊前缀开头）
     */
	protected boolean isFactoryDereference(String name) {
		return name.startsWith(FACTORY_BEAN_PREFIX);
	}

	/**
	 * Initialize the given BeanWrapper with the custom editors registered
	 * with this factory.
	 * @param bw the BeanWrapper to initialize
	 */
	protected void initBeanWrapper(BeanWrapper bw) {
		for (Iterator it = this.customEditors.keySet().iterator(); it.hasNext();) {
			Class clazz = (Class) it.next();
			bw.registerCustomEditor(clazz, (PropertyEditor) this.customEditors.get(clazz));
		}
	}

	/**
	 * Return the names of beans in the singleton cache that match the given
	 * object type (including subclasses). Will <i>not</i> consider FactoryBeans
	 * as the type of their created objects is not known before instantiation.
	 * <p>Does not consider any hierarchy this factory may participate in.
	 * @param type class or interface to match, or null for all bean names
	 * @return the names of beans in the singleton cache that match the given
	 * object type (including subclasses), or an empty array if none
	 */
	public String[] getSingletonNames(Class type) {
		synchronized (this.singletonCache) {
			Set keys = this.singletonCache.keySet();
			Set matches = new HashSet();
			Iterator itr = keys.iterator();
			while (itr.hasNext()) {
				String name = (String) itr.next();
				Object singletonObject = this.singletonCache.get(name);
				if (type == null || type.isAssignableFrom(singletonObject.getClass())) {
					matches.add(name);
				}
			}
			return (String[]) matches.toArray(new String[matches.size()]);
		}
	}

    /**
     * 根据bean名称和实例，获取bean实例。
     * 处理了factory bean的情况：若实例是factory bean，且调用者想要一个由factory bean创建的bean实例，
     * 则根据factory bean创建一个实例。
     * 除此之外的情况，直接返回对应的bean实例。
     *
     * @param name bean名称，可能包含factoryBean的特殊前缀
     * @param beanInstance 单例bean实例
     * @return
     */
	protected Object getObjectForSharedInstance(String name, Object beanInstance) {
		String beanName = transformedBeanName(name);

        // 如果name符合factory bean，实例却不属于FactoryBean类型，则抛出异常
		if (isFactoryDereference(name) && !(beanInstance instanceof FactoryBean)) {
			throw new BeanIsNotAFactoryException(beanName, beanInstance);
		}

        // 现在，bean实例可能是普通的bean，或是一个factory bean。
        // 若实例是factory bean，且是普通的bean名称，则用它创建一个bean实例；
        // 若实例是factory bean，但带有特殊前缀，则调用者只想要一个factory的实例；
        // 若实例不是factory bean，则直接返回实例即可。
		if (beanInstance instanceof FactoryBean) {
			if (!isFactoryDereference(name)) {
                // 由factory bean创建一个bean实例
				FactoryBean factory = (FactoryBean) beanInstance;
				try {
					beanInstance = factory.getObject();
				} catch (Exception ex) {
					throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
				}
				// 创建不成功，说明有循环引用的情况
				if (beanInstance == null) {
					throw new FactoryBeanCircularReferenceException(beanName, "FactoryBean returned null object: not fully initialized due to circular bean reference");
				}
			} else {
			    // 调用者只是想要一个factory bean本身，而不是由它创建的bean实例
			}
		}
		return beanInstance;
	}

	/**
	 * 获取bean的根定义（RootBeanDefinition）
	 * 如果当前bean定义是子定义的话，递归找其父bean定义，直到找到其根节点。
	 * 因此，最后的结果应递归包含所有parent属性指代的bean定义信息。
	 *
	 * @param includingAncestors
	 */
	public RootBeanDefinition getMergedBeanDefinition(String beanName, boolean includingAncestors) throws BeansException {
		try {
			return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
		} catch (NoSuchBeanDefinitionException ex) {
			if (includingAncestors && getParentBeanFactory() instanceof AbstractBeanFactory) {
				return ((AbstractBeanFactory) getParentBeanFactory()).getMergedBeanDefinition(beanName, true);
			} else {
				throw ex;
			}
		}
	}

	/**
	 * Return a RootBeanDefinition, even by traversing parent if the parameter is a child definition.
	 * @return a merged RootBeanDefinition with overridden properties
	 */
	protected RootBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd) {
		if (bd instanceof RootBeanDefinition) {
			return (RootBeanDefinition) bd;
		}

		else if (bd instanceof ChildBeanDefinition) {
			ChildBeanDefinition cbd = (ChildBeanDefinition) bd;
			RootBeanDefinition pbd = null;
			if (!beanName.equals(cbd.getParentName())) {
				pbd = getMergedBeanDefinition(cbd.getParentName(), true);
			}
			else {
				if (getParentBeanFactory() instanceof AbstractBeanFactory) {
					pbd = ((AbstractBeanFactory) getParentBeanFactory()).getMergedBeanDefinition(cbd.getParentName(), true);
				}
				else {
					throw new NoSuchBeanDefinitionException(cbd.getParentName(),
							"Parent name '" + cbd.getParentName() + "' is equal to bean name '" + beanName +
							"' - cannot be resolved without an AbstractBeanFactory parent");
				}
			}

			// deep copy with overridden values
			RootBeanDefinition rbd = new RootBeanDefinition(pbd);
			rbd.overrideFrom(cbd);
			return rbd;
		}
		else {
			throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName,
					"Definition is neither a RootBeanDefinition nor a ChildBeanDefinition");
		}
	}

	//---------------------------------------------------------------------
	// Abstract methods to be implemented by concrete subclasses
	//---------------------------------------------------------------------

	/**
	 * Check if this bean factory contains a bean definition with the given name.
	 * Does not consider any hierarchy this factory may participate in.
	 * Invoked by containsBean when no cached singleton instance is found.
	 * @param beanName the name of the bean to look for
	 * @return if this bean factory contains a bean definition with the given name
	 * @see #containsBean
	 */
	public abstract boolean containsBeanDefinition(String beanName);

	/**
	 * Return the bean definition for the given bean name.
	 * Subclasses should normally implement caching, as this method is invoked
	 * by this class every time bean definition metadata is needed.
	 * @param beanName name of the bean to find a definition for
	 * @return the BeanDefinition for this prototype name. Must never return null.
	 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException
	 * if the bean definition cannot be resolved
	 * @throws BeansException in case of errors
	 * @see RootBeanDefinition
	 * @see ChildBeanDefinition
	 */
	protected abstract BeanDefinition getBeanDefinition(String beanName) throws BeansException;

	/**
	 * Create a bean instance for the given bean definition.
	 * The bean definition will already have been merged with the parent
	 * definition in case of a child definition.
	 * <p>All the other methods in this class invoke this method, although
	 * beans may be cached after being instantiated by this method. All bean
	 * instantiation within this class is performed by this method.
	 * @param beanName name of the bean
	 * @param mergedBeanDefinition the bean definition for the bean
	 * @param args arguments to use if creating a prototype using explicit arguments
	 * to a static factory method. This parameter must be null except in this case.
	 * @return a new instance of the bean
	 * @throws BeansException in case of errors
	 */
	protected abstract Object createBean(
			String beanName, RootBeanDefinition mergedBeanDefinition, Object[] args) throws BeansException;

	/**
	 * Destroy the given bean. Must destroy beans that depend on the given
	 * bean before the bean itself. Should not throw any exceptions.
	 * @param beanName name of the bean
	 * @param bean the bean instance to destroy
	 */
	protected abstract void destroyBean(String beanName, Object bean);

}
