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

package org.springframework.core;

/**
 * 实现此接口的类应该是可被排序的，比如在一个集合中。
 * order可以被理解为优先级，order值越低，优先级越高。
 */
public interface Ordered {

  /**
   * 获取当前对象的order值，值越大排序时越靠后。
   * 通常从0或1开始，最大为Integer.MAX_VALUE。
   *
   * order值相同的对象，优先级随机。
   * 值越高说明优先级越低，有点类似于servlet启动时的load-on-startup参数。
   */
	public int getOrder();
}
