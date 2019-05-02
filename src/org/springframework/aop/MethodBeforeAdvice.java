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

import java.lang.reflect.Method;

/**
 * 一个方法执行之前，Advice内的逻辑先执行。
 * 只有advice内的逻辑抛出异常，才会阻止方法的正常调用。
 */
public interface MethodBeforeAdvice extends BeforeAdvice {

	/**
	 * 在给定方法调用之前，进行回调
	 * @param m 将要被调用的方法
	 * @param args 方法m的参数
	 * @param target 方法调用的目标，可能为空
	 * @throws Throwable 如果横切逻辑想要终止方法调用，且方法签名允许抛出异常，则会抛出异常。否则会被包装为一个runtime exception抛出
	 */
	void before(Method m, Object[] args, Object target) throws Throwable;

}
