package org.eclipse.rdf4j.spring.uuidsource.sequence;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(UUIDSequenceProperties.class)
@ConditionalOnProperty(prefix = "rdf4j.spring.uuidsource.sequence", name = "enabled")
public class UUIDSequenceConfig {
	@Bean
	public UUIDSequence getUUIDSource(@Valid @Autowired UUIDSequenceProperties properties) {
		return new UUIDSequence(properties);
	}
}
