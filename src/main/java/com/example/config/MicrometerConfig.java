/*
 * Copyright (C) 2025 Toshiaki Maki <makingx@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.config;

import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MicrometerConfig {

	@Bean
	public MeterFilter meterFilter() {
		return MeterFilter.deny(id -> {
			String uri = id.getTag("uri");
			if (uri == null) {
				uri = id.getTag("url.path");
			}
			return uri != null && (uri.equals("/readyz") || uri.equals("/livez") || uri.startsWith("/actuator")
					|| uri.startsWith("/_static"));
		});
	}

}