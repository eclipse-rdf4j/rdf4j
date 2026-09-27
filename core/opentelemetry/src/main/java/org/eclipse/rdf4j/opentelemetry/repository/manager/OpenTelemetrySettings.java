/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.opentelemetry.repository.manager;

/**
 * The system property gating whether {@link TracingLocalRepositoryManager}/{@link TracingRemoteRepositoryManager}
 * actually instrument the repositories they create.
 * <p>
 * Follows the same convention as other RDF4J-server-side system properties (e.g. the workbench's
 * {@code accepted-server-prefixes}): a plain system property, disabled by default.
 */
public final class OpenTelemetrySettings {

	/**
	 * System property enabling {@code RepositoryConnection}-level OpenTelemetry tracing for every repository created by
	 * a {@link TracingLocalRepositoryManager}/{@link TracingRemoteRepositoryManager}. Disabled by default.
	 */
	public static final String ENABLED_PROPERTY = "org.eclipse.rdf4j.opentelemetry.enabled";

	private OpenTelemetrySettings() {
	}

	/**
	 * @return whether {@value #ENABLED_PROPERTY} is set to {@code true}.
	 */
	public static boolean isEnabled() {
		return Boolean.getBoolean(ENABLED_PROPERTY);
	}
}
