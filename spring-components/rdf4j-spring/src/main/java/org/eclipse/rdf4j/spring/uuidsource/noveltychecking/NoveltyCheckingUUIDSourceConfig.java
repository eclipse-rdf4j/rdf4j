package org.eclipse.rdf4j.spring.uuidsource.noveltychecking;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "rdf4j.spring.uuidsource.noveltychecking", name = "enabled")
@EnableConfigurationProperties(NoveltyCheckingUUIDSourceProperties.class)
public class NoveltyCheckingUUIDSourceConfig {
	@Bean
	public NoveltyCheckingUUIDSource getUUIDSource() {
		return new NoveltyCheckingUUIDSource();
	}
}
