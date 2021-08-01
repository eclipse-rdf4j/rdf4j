package org.eclipse.rdf4j.spring.resultcache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("rdf4j.spring.resultcache.enabled")
@EnableConfigurationProperties(ResultCacheProperties.class)
public class ResultCacheConfig {
}
