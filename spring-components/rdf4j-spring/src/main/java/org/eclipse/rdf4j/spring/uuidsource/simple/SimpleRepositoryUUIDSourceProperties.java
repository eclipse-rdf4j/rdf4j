package org.eclipse.rdf4j.spring.uuidsource.simple;

import javax.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rdf4j.spring.uuidsource.simple")
public class SimpleRepositoryUUIDSourceProperties {
	@NotBlank
	private boolean enabled = false;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
