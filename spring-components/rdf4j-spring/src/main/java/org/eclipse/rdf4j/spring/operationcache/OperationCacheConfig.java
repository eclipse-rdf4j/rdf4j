package org.eclipse.rdf4j.spring.operationcache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty("rdf4j.spring.operationcache.enabled")
@EnableConfigurationProperties(OperationCacheProperties.class)
public class OperationCacheConfig {
}
