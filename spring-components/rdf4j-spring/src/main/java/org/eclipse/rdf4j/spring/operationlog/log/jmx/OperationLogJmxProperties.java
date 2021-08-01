package org.eclipse.rdf4j.spring.operationlog.log.jmx;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rdf4j.spring.operationlog.jmx")
public class OperationLogJmxProperties {
	boolean enabled = false;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
