package org.eclipse.rdf4j.spring.uuidsource.noveltychecking;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rdf4j.spring.uuidsource.noveltychecking")
public class NoveltyCheckingUUIDSourceProperties {
	private boolean enabled;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
