package org.eclipse.rdf4j.spring.uuidsource.sequence;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rdf4j.spring.uuidsource.sequence")
public class UUIDSequenceProperties {

	private boolean enabled;

	// Approximate number of UUIDs to prefetch from the repository
	@NotBlank
	@Min(value = 8, message = "Value must be 8 or higher!")
	private int prefetchCount = 1000;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getPrefetchCount() {
		return prefetchCount;
	}

	public void setPrefetchCount(int prefetchCount) {
		this.prefetchCount = prefetchCount;
	}
}
