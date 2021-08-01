package org.eclipse.rdf4j.spring.uuidsource.simple;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "rdf4j.spring.uuidsource.simple", name = "enabled")
@EnableConfigurationProperties(SimpleRepositoryUUIDSourceProperties.class)
public class SimpleRepositoryUUIDSourceConfig {
	@Bean
	public SimpleRepositoryUUIDSource getSimpleRepositoryUUIDSource() {
		return new SimpleRepositoryUUIDSource();
	}
}
