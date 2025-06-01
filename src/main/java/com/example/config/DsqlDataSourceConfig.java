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

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.SQLExceptionOverride;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.task.SimpleAsyncTaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.dsql.DsqlUtilities;
import software.amazon.awssdk.services.dsql.model.GenerateAuthTokenRequest;
import software.amazon.awssdk.services.sso.auth.SsoProfileCredentialsProviderFactory;

@Configuration(proxyBeanMethods = false)
@Profile("!testcontainers")
@ImportRuntimeHints(DsqlDataSourceConfig.RuntimeHints.class)
public class DsqlDataSourceConfig {

	private final Logger logger = LoggerFactory.getLogger(DsqlDataSourceConfig.class);

	private final Duration tokenTtl = Duration.ofMinutes(60);

	@Bean
	@ConfigurationProperties("spring.datasource")
	DataSourceProperties dsqlDataSourceProperties() {
		return new DataSourceProperties();
	}

	@Bean
	Supplier<String> dsqlTokenSupplier(DataSourceProperties dsqlDataSourceProperties,
			AwsRegionProvider awsRegionProvider, AwsCredentialsProvider credentialsProvider) {
		Region region = awsRegionProvider.getRegion();
		DsqlUtilities utilities = DsqlUtilities.builder()
			.region(region)
			.credentialsProvider(credentialsProvider)
			.build();
		String username = dsqlDataSourceProperties.getUsername();
		String hostname = dsqlDataSourceProperties.getUrl().split("/")[2];
		return () -> {
			Consumer<GenerateAuthTokenRequest.Builder> request = builder -> builder.hostname(hostname)
				.region(region)
				.expiresIn(tokenTtl);
			return "admin".equals(username) ? utilities.generateDbConnectAdminAuthToken(request)
					: utilities.generateDbConnectAuthToken(request);
		};
	}

	@Bean
	@ConfigurationProperties("spring.datasource.hikari")
	HikariDataSource dsqlDataSource(DataSourceProperties dsqlDataSourceProperties, Supplier<String> dsqlTokenSupplier) {
		HikariDataSource dataSource = dsqlDataSourceProperties.initializeDataSourceBuilder()
			.type(HikariDataSource.class)
			.build();
		String token = dsqlTokenSupplier.get();
		if (StringUtils.hasText(dataSource.getPassword())) {
			logger.warn("Overriding existing password for the datasource with DSQL token.");
		}
		dataSource.setPassword(token);
		dataSource.setExceptionOverrideClassName(DsqlExceptionOverride.class.getName());
		return dataSource;
	}

	@Bean
	DsqlSQLExceptionTranslator dsqlSQLExceptionTranslator() {
		return new DsqlSQLExceptionTranslator();
	}

	@Bean
	JdbcTransactionManager transactionManager(DataSource dataSource,
			DsqlSQLExceptionTranslator dsqlSQLExceptionTranslator) {
		JdbcTransactionManager jdbcTransactionManager = new JdbcTransactionManager(dataSource);
		jdbcTransactionManager.setExceptionTranslator(dsqlSQLExceptionTranslator);
		return jdbcTransactionManager;
	}

	@Bean
	SimpleAsyncTaskScheduler taskScheduler(SimpleAsyncTaskSchedulerBuilder builder) {
		return builder.build();
	}

	@Bean
	InitializingBean tokenRefresher(DataSource dataSource, Supplier<String> dsqlTokenSupplier,
			SimpleAsyncTaskScheduler taskScheduler) throws Exception {
		HikariDataSource hikariDataSource = dataSource.unwrap(HikariDataSource.class);
		Duration interval = tokenTtl.dividedBy(2);
		return () -> taskScheduler.scheduleWithFixedDelay(() -> {
			try {
				String token = dsqlTokenSupplier.get();
				hikariDataSource.getHikariConfigMXBean().setPassword(token);
				hikariDataSource.getHikariPoolMXBean().softEvictConnections();
			}
			catch (RuntimeException e) {
				logger.error("Failed to refresh DSQL token", e);
			}
		}, Instant.now().plusSeconds(interval.toSeconds()), interval);
	}

	// https://catalog.workshops.aws/aurora-dsql/en-US/04-programming-with-aurora-dsql/02-handling-concurrency-conflicts
	private static final String DSQL_OPTIMISTIC_CONCURRENCY_ERROR_STATE = "40001";

	static class DsqlSQLExceptionTranslator implements SQLExceptionTranslator {

		SQLStateSQLExceptionTranslator delegate = new SQLStateSQLExceptionTranslator();

		@Override
		public DataAccessException translate(String task, String sql, SQLException ex) {
			if (DSQL_OPTIMISTIC_CONCURRENCY_ERROR_STATE.equals(ex.getSQLState())) {
				throw new OptimisticLockingFailureException(ex.getMessage(), ex);
			}
			return delegate.translate(task, sql, ex);
		}

	}

	public static class DsqlExceptionOverride implements SQLExceptionOverride {

		@java.lang.Override
		public Override adjudicate(SQLException ex) {
			if (DSQL_OPTIMISTIC_CONCURRENCY_ERROR_STATE.equals(ex.getSQLState())) {
				return Override.DO_NOT_EVICT;
			}
			return Override.CONTINUE_EVICT;
		}

	}

	static class RuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(org.springframework.aot.hint.RuntimeHints hints, ClassLoader classLoader) {
			try {
				hints.reflection()
					.registerConstructor(DsqlExceptionOverride.class.getDeclaredConstructors()[0],
							ExecutableMode.INVOKE)
					.registerConstructor(Class.forName("org.postgresql.ssl.DefaultJavaSSLFactory").getConstructors()[0],
							ExecutableMode.INVOKE)
					.registerConstructor(SsoProfileCredentialsProviderFactory.class.getConstructors()[0],
							ExecutableMode.INVOKE);
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}

	}

}
