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

/**
 * 基于AdvisedSupport创建AOP代理对象的核心接口
 * <p>
 * 代理对象应该遵守以下约定：
 * 1. 应实现所有配置中指定要代理的接口；
 * 2. 应实现Advised接口；
 * 3. 应实现equals()方法，用于比较代理接口、advice和目标对象；
 * 4. 如果目标对象和所有advisor都是可被序列化的，代理对象也应该可被序列化；
 * 5. 如果目标对象和所有advisor都线程安全，代理对象也应线程安全。
 * <p>
 * 代理可能允许，也可能不允许改变advice。
 * 如果不允许改变advice（如由于配置冻结的原因），在尝试更改advice时应抛出AopConfigException。
 */
public interface AopProxyFactory {

	/**
	 * 根据AdvisedSupport配置对象，创建一个AOP代理对象
	 *
	 * @param advisedSupport AOP配置
	 * @return AOP代理对象
	 */
	AopProxy createAopProxy(AdvisedSupport advisedSupport) throws AopConfigException;

}
