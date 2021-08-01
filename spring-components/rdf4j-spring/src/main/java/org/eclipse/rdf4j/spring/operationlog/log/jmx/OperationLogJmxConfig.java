package org.eclipse.rdf4j.spring.operationlog.log.jmx;

import java.util.Map;

import org.eclipse.rdf4j.spring.operationlog.log.OperationLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.MBeanExporter;

@Configuration
@ConditionalOnBean(OperationLog.class)
@ConditionalOnProperty(value = "rdf4j.spring.operationlog.jmx.enabled")
@EnableConfigurationProperties(OperationLogJmxProperties.class)
public class OperationLogJmxConfig {

	@Bean
	public OperationStatsBean getOperationStatsBean() {
		return new OperationStatsBean();
	}

	@Bean
	public MBeanExporter getMBeanExporter(@Autowired OperationStatsBean operationStatsBean) {
		MBeanExporter exporter = new MBeanExporter();
		exporter.setBeans(
				Map.of(
						"org.eclipse.rdf4j.spring.operationlog:name=OperationStats",
						operationStatsBean));
		return exporter;
	}
}
