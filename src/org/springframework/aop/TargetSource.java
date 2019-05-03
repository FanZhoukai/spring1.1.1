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
 * TargetSource用于获取当前AOP调用的目标对象。
 * 如果没有环绕advice来结束拦截器链，将使用反射来调用目标对象。
 *
 * 静态TargetSource总会返回相同的目标对象，运行AOP框架内的优化。
 * 动态TargetSource支持池化，热插拔(hot swapping)等等特性。
 *
 * 解释下：
 * 池化：是指把目标对象放在一个池中，见CommonsPoolTargetSource；
 * 热插拔：是指能动态无感知切换目标对象的能力，见HotSwappableTargetSource
 *
 *
 * 应用开发者通常不需要直接使用TargetSource，这是AOP框架的内部接口。
 */
public interface TargetSource {

	/**
	 * Return the type of targets returned by this TargetSource.
	 * Can return null, although certain usages of a TargetSource
	 * might just work with a predetermined target class.
	 */
	Class getTargetClass();
	
	/**
	 * Will all calls to getTarget() return the same object?
	 * In that case, there will be no need to invoke releaseTarget(),
	 * and the AOP framework can cache the return value of getTarget().
	 * @return whether the target is immutable.
	 */
	boolean isStatic();

	/**
	 * 获取一个目标实例。
	 * 在AOP框架调用目标对象的方法前，该方法会立即被调用。
	 * <p>
	 * Return a target instance. Invoked immediately before the
	 * AOP framework calls the "target" of an AOP method invocation.
	 *
	 * @return 目标对象，包含joinpoint
	 * @throws Exception 如果目标对象无法被解析
	 */
	Object getTarget() throws Exception;
	
	/**
	 * Release the given target object obtained from the getTarget() method.
	 * @param target object obtained from a call to getTarget()
	 * @throws Exception if the object can't be released
	 * @see #getTarget
	 */
	void releaseTarget(Object target) throws Exception;

}
