package org.eclipse.rdf4j.spring.operationlog;

import org.eclipse.rdf4j.spring.operationlog.log.OperationLog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "rdf4j.spring.operationlog", name = "enabled")
@EnableConfigurationProperties(OperationLogProperties.class)
public class OperationLogConfig {
	@Bean
	OperationLog getOperationLog() {
		return new OperationLog();
	}
}
