/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.spring.resultcache;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
@ConfigurationProperties(prefix = "rdf4j.spring.resultcache")
@Validated
public class ResultCacheProperties {

	private boolean enabled = false;

	/** Initial size of each cache * */
	private int initialSize = 10;

	/** Maximum size of each cache * */
	private int maxSize = 1000;

	/**
	 * If true, a global result cache is used that is cleared when the application writes to the repository. If false,
	 * no global result cache is used.
	 */
	private boolean assumeNoOtherRepositoryClients = false;

	/** Max age for cache entries. Specifiy as Duration, e.g. 1H, 10m, etc. */
	private Duration entryLifetime = Duration.ofHours(1);

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getInitialSize() {
		return initialSize;
	}

	public void setInitialSize(int initialSize) {
		this.initialSize = initialSize;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	public boolean isAssumeNoOtherRepositoryClients() {
		return assumeNoOtherRepositoryClients;
	}

	public void setAssumeNoOtherRepositoryClients(boolean assumeNoOtherRepositoryClients) {
		this.assumeNoOtherRepositoryClients = assumeNoOtherRepositoryClients;
	}

	public Duration getEntryLifetime() {
		return entryLifetime;
	}

	public void setEntryLifetime(Duration entryLifetime) {
		this.entryLifetime = entryLifetime;
	}
}
