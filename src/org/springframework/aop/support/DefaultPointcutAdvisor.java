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

package org.springframework.aop.support;

import java.io.Serializable;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.core.Ordered;

/**
 * pointcut驱动的Advisor实现类。
 * 是最通用的Advisor，基本所有需求都可以通过它来实现。
 * 支持任意类型的pointcut和advice，除introductions外。
 *
 * 通常情况下，不需要继承这个类，也不需要自己实现Advisor。
 * （意思是说，向其中注入pointcut和advise就可以直接拿来用了）
 *
 * Convenient pointcut-driven advisor implementation.
 *
 * <p>This is the most commonly used Advisor implementation. It can be
 * used with any pointcut and advice type, except for introductions.
 * There is normally no need to subclass this class, or to
 * implement custom Advisors.
 *
 * @author Rod Johnson
 */
public class DefaultPointcutAdvisor implements PointcutAdvisor, Ordered, Serializable {

	private int order = Integer.MAX_VALUE;

	private Pointcut pointcut;
	
	private Advice advice;
	
	/**
	 * Create an empty DefaultPointcutAdvisor.
	 * Advice and Pointcut must be set before use using
	 * setter methods.
	 */
	public DefaultPointcutAdvisor() {
	}
	
	/**
	 * Create a DefaultPointcutAdvisor that matches all methods.
	 * Pointcut.TRUE will be used as pointcut.
	 * @param advice advice to use
	 */
	public DefaultPointcutAdvisor(Advice advice) {
		this(Pointcut.TRUE, advice);
	}
	
	/**
	 * Create a DefaultPointcutAdvisor, specifying pointcut
	 * and advice
	 * @param pointcut pointcut targeting the advice
	 * @param advice advice to run when pointcut matches
	 */
	public DefaultPointcutAdvisor(Pointcut pointcut, Advice advice) {
		this.pointcut = pointcut;
		this.advice = advice;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public int getOrder() {
		return order;
	}

	public void setAdvice(Advice advice) {
		this.advice = advice;
	}

	public Advice getAdvice() {
		return advice;
	}

	public Pointcut getPointcut() {
		return pointcut;
	}
	
	public void setPointcut(Pointcut pointcut) {
		this.pointcut = pointcut;
	}

	public boolean isPerInstance() {
		throw new UnsupportedOperationException("perInstance property of Advisor is not yet supported in Spring");
	}

	public boolean equals(Object o) {
		if (!(o instanceof DefaultPointcutAdvisor)) {
			return false;
		}
		DefaultPointcutAdvisor other = (DefaultPointcutAdvisor) o;
		return other.advice.equals(this.advice) && other.pointcut.equals(this.pointcut);
	}
	
	public String toString() {
		return "DefaultPointcutAdvisor: pointcut=" + pointcut + "; " +
				"advice=" + advice;
	}

}
