package org.eclipse.rdf4j.spring;

import org.eclipse.rdf4j.spring.support.DataInserter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@TestConfiguration
@EnableTransactionManagement
@ComponentScan(basePackages = "org.eclipse.rdf4j.spring.domain.dao")
public class TestConfig {
	@Bean
	DataInserter getDataInserter() {
		return new DataInserter();
	}
}
