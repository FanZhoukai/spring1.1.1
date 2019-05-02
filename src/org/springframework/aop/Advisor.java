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

package org.springframework.aop;

import org.aopalliance.aop.Advice;

/**
 * AOP的基础接口。
 * 持有advice（横切逻辑），和一个过滤器（决定advice在哪些方法上执行）
 * 该接口不是给spring的使用者使用的，但考虑了支持多种advice的共同特征。
 *
 * spring AOP是基于around advice(通过方法拦截器实现)的，与AOP alliance的拦截器API兼容。
 * 支持多种advice，比如before、after，这些不需要使用拦截器去实现。
 */
public interface Advisor {
	
	/**
	 * 好像没啥用。。。
	 * Return whether this advice is associated with a particular instance
	 * (for example, creating a mixin) or is it shared with all instances of
	 * the advised class obtained from the same Spring bean factory.
	 * <b>Note that this method is not currently used by the framework</b>.
	 * Use singleton/prototype bean definitions or appropriate programmatic
	 * proxy creation to ensure that Advisors have the correct lifecycle model. 
	 */
	boolean isPerInstance();
	
	/**
	 * 获取该aspect的advice部分。
	 * advice可能是拦截器(around advice)、throw advice、before advice等等。
	 * spring支持用户自定义的advice，见org.springframework.aop.adapter包。
	 *
	 * 重申：AOP中的aspect = spring中的advisor，包括一个advice(横切逻辑)和n个pointcut(切入点)
	 *
	 * @return the advice that should apply if the pointcut matches
	 */
	Advice getAdvice();

}
