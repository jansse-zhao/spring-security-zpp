/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.web;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Defines a filter chain which is capable of being matched against an
 * {@code HttpServletRequest}. in order to decide whether it applies to that request.
 * <p>
 * Used to configure a {@code FilterChainProxy}.
 * <p>
 *  TODO-ADMIN 核心组件security过滤器链
 *  当前框架只有一个默认实现{@link DefaultSecurityFilterChain}
 *
 * @author Luke Taylor
 * @since 3.1
 */
public interface SecurityFilterChain {


	// 要能根据request判断是否匹配某个过滤器
	boolean matches(HttpServletRequest request);

	// 要能获取一个过滤器列表
	List<Filter> getFilters();

}
