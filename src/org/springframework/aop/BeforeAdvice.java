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
 * 所有before advice的超级接口（before advice是指AOP在指定方法前执行的横切逻辑）
 *
 * spring只支持方法级别的before advice，尽管这不可能改变，但未来如果需要的话，这个API的设计允许字段级别的advice
 *
 * @see org.springframework.aop.MethodBeforeAdvice
 */
public interface BeforeAdvice extends Advice {

}
