package org.eclipse.rdf4j.spring.repository.inmemory;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rdf4j.spring.repository.inmemory")
public class InMemoryRepositoryProperties {
	private boolean enabled = true;
	/** Should a SHACL Sail be used? */
	private boolean useShaclSail = false;

	public boolean isUseShaclSail() {
		return useShaclSail;
	}

	public void setUseShaclSail(boolean useShaclSail) {
		this.useShaclSail = useShaclSail;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
