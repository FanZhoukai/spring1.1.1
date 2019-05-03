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

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Convenience superclass for configuration used in creating proxies,
 * to ensure that all proxy creators have consistent properties.
 *
 * <p>Note that it is no longer possible to configure subclasses to expose
 * the MethodInvocation. Interceptors should normally manage their own
 * ThreadLocals if they need to make resources available to advised objects.
 * If it's absolutely necessary to expose the MethodInvocation, use an
 * interceptor to do so.
 *
 * @author Rod Johnson
 */
public class ProxyConfig implements Serializable {
	
	/*
	 * Note that some of the instance variables in this class and AdvisedSupport
	 * are protected, rather than private, as is usually preferred in Spring
	 * (following "Expert One-on-One J2EE Design and Development", Chapter 4).
	 * This allows direct field access in the AopProxy implementations, which
	 * produces a 10-20% reduction in AOP performance overhead compared with
	 * method access. - RJ, December 10, 2003.
	 */
	
	/**
	 * Transient to optimize serialization:
	 * AdvisedSupport resets it.
	 */
	protected transient Log logger = LogFactory.getLog(getClass());

	private boolean proxyTargetClass;
	
	private boolean optimize;
	
	private boolean opaque;

	/**
	 * Should proxies obtained from this configuration expose
	 * the AOP proxy for the AopContext class to retrieve for targets?
	 * The default is false, as enabling this property may impair performance.
	 */
	protected boolean exposeProxy;

	/**
	 * Is this config frozen: that is, should it be impossible
	 * to change advice. Default is not frozen.
	 */
	private boolean frozen;
	
	/** 用于创建AOP代理对象实例的工厂（即织入器） */
	private transient AopProxyFactory aopProxyFactory = new DefaultAopProxyFactory();

	
	/**
	 * Set whether to proxy the target class directly as well as any interfaces.
	 * We can set this to true to force CGLIB proxying. Default is false.
	 */
	public void setProxyTargetClass(boolean proxyTargetClass) {
		this.proxyTargetClass = proxyTargetClass;
	}

	/**
	 * 是否直接代理目标类，和所有接口
	 * Return whether to proxy the target class directly as well as any interfaces.
	 */
	public boolean getProxyTargetClass() {
		return this.proxyTargetClass;
	}

	/**
	 * 设置代理是否应该执行侵略性优化。
	 * “侵略性优化”的确切含义在不同的代理之间会有所不同，但是通常会有一些权衡。
	 * 例如，优化通常意味着在创建代理之后advice更改不会生效。（即为了创建代理的速度，放弃了后续修改advice的可能）
	 * 由于这个原因，optimize默认是false。
	 *
	 * 如果其他设置禁止优化，则optimize=true可以被忽略：
	 * 例如，如果将exposeProxy设置为true，而这与optimize不兼容，则可以忽略true。
	 * 例如，cglib增强的动态代理可以进行优化，覆盖没有通知链的方法。对于没有advice的方法，这可以产生2.5倍的性能提升。
	 * 警告:当使用CGLIB(也将proxyTargetClass设置为true)时，将该值设置为true可以获得较大的性能收益，因此这对于性能关键的代理来说是一个很好的设置。
	 * 然而，启用此功能将意味着在从该工厂获得代理之后不能更改advice。
	 *
	 * @param optimize 是否启用侵入式优化——优化后无法更改advice。默认为false
	 */
	public void setOptimize(boolean optimize) {
		this.optimize = optimize;
	}

	/**
	 * 是否启用侵入式优化——优化后无法更改advice
	 */
	public boolean getOptimize() {
		return this.optimize;
	}

	/**
	 * Set whether the proxy should be exposed by the AOP framework as a
	 * ThreadLocal for retrieval via the AopContext class. This is useful
	 * if an advised object needs to call another advised method on itself.
	 * (If it uses <code>this</code>, the invocation will not be advised).
	 * <p>Default is false, for optimal performance.
	 */
	public void setExposeProxy(boolean exposeProxy) {
		this.exposeProxy = exposeProxy;
	}
	
	/**
	 * Return whether the AOP proxy will expose the AOP proxy for
	 * each invocation.
	 */
	public boolean getExposeProxy() {
		return this.exposeProxy;
	}

	/**
	 * Set whether this config should be frozen.
	 * <p>When a config is frozen, no advice changes can be made. This is
	 * useful for optimization, and useful when we don't want callers to
	 * be able to manipulate configuration after casting to Advised.
	 */
	public void setFrozen(boolean frozen) {
		this.frozen = frozen;
	}

	/**
	 * Return whether the config is frozen, and no advice changes can be made.
	 */
	public boolean isFrozen() {
		return frozen;
	}

	/**
	 * Customize the AopProxyFactory, allowing different strategies
	 * to be dropped in without changing the core framework.
	 * Default is DefaultAopProxyFactory, using dynamic proxies or CGLIB.
	 * <p>For example, an AopProxyFactory could return an AopProxy using
	 * dynamic proxies, CGLIB or code generation strategy.
	 */
	public void setAopProxyFactory(AopProxyFactory apf) {
		this.aopProxyFactory = apf;
	}

	/**
	 * 获取当前ProxyConfig对象使用的织入器
	 */
	public AopProxyFactory getAopProxyFactory() {
		return this.aopProxyFactory;
	}

	/**
	 * @return whether proxies created by this configuration
	 * should be prevented from being cast to Advised
	 */
	public boolean getOpaque() {
		return opaque;
	}
	
	/**
	 * @param opaque Set whether proxies created by this configuration
	 * should be prevented from being cast to Advised to
	 * query proxy status. Default is false, meaning that
	 * any AOP proxy can be cast to Advised.
	 */
	public void setOpaque(boolean opaque) {
		this.opaque = opaque;
	}

	/**
	 * Copy configuration from the other config object.
	 * @param other object to copy configuration from
	 */
	public void copyFrom(ProxyConfig other) {
		this.proxyTargetClass = other.proxyTargetClass;
		this.optimize = other.getOptimize();
		this.exposeProxy = other.exposeProxy;
		this.frozen = other.frozen;
		this.opaque = other.opaque;
		this.aopProxyFactory = other.aopProxyFactory;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("proxyTargetClass=" + this.proxyTargetClass + "; ");
		sb.append("optimize=" + this.optimize + "; ");
		sb.append("exposeProxy=" + this.exposeProxy + "; ");
		sb.append("opaque=" + this.opaque + "; ");
		sb.append("frozen=" + this.frozen + "; ");
		sb.append("aopProxyFactory=" + this.aopProxyFactory + "; ");
		return sb.toString();
	}
}
