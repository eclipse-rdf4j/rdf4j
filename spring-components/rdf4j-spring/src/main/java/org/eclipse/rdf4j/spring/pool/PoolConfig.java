package org.eclipse.rdf4j.spring.pool;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "rdf4j.spring.pool", name = "enabled")
@EnableConfigurationProperties(PoolProperties.class)
public class PoolConfig {
}
