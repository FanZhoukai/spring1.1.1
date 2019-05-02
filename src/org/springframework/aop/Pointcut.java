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


/**
 * spring核心Pointcut（切入点）的抽象。
 *
 * 一个Pointcut由类过滤器和方法匹配器组成。
 * 类过滤器、方法匹配器，和Pointcut本身，都可以互相组合，来构建组合的Pointcut
 */
public interface Pointcut {

	ClassFilter getClassFilter();
	
	MethodMatcher getMethodMatcher();
	
	// could add getFieldMatcher() without breaking most existing code
	
	Pointcut TRUE = TruePointcut.INSTANCE;

}
