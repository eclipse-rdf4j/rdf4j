package org.eclipse.rdf4j.spring.pool;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rdf4j.spring.pool")
public class PoolProperties {
	private boolean enabled = false;
	/** Maximum number of connections pooled. */
	private int maxConnections = 20;

	/** Minimum number of connections held idle. */
	private int minIdleConnections = 5;
	/** Duration (e.g. 30s) between checks for stale connecitons. */
	private Duration timeBetweenEvictionRuns = Duration.ofSeconds(30);
	/** Should the pool actively test connections using a SPARQL statement? */
	private boolean testWhileIdle = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	public int getMinIdleConnections() {
		return minIdleConnections;
	}

	public void setMinIdleConnections(int minIdleConnections) {
		this.minIdleConnections = minIdleConnections;
	}

	public Duration getTimeBetweenEvictionRuns() {
		return timeBetweenEvictionRuns;
	}

	public void setTimeBetweenEvictionRuns(Duration timeBetweenEvictionRuns) {
		this.timeBetweenEvictionRuns = timeBetweenEvictionRuns;
	}

	public boolean isTestWhileIdle() {
		return testWhileIdle;
	}

	public void setTestWhileIdle(boolean testWhileIdle) {
		this.testWhileIdle = testWhileIdle;
	}
}
