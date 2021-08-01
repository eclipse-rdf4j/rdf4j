package org.eclipse.rdf4j.spring.tx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "rdf4j.spring.tx", name = "enabled")
@EnableConfigurationProperties(TxProperties.class)
public class TxConfig {

	@Bean
	Rdf4JRepositoryTransactionManager getTxManager(
			@Autowired TransactionalRepositoryConnectionFactory txConnectionFactory) {
		return new Rdf4JRepositoryTransactionManager(txConnectionFactory);
	}
}
