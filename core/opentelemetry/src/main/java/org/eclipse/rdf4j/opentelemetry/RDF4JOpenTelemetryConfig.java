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
package org.eclipse.rdf4j.opentelemetry;

import java.util.Objects;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

/**
 * Configuration shared by RDF4J's OpenTelemetry instrumentation areas (see {@link org.eclipse.rdf4j.opentelemetry.http}
 * and {@link org.eclipse.rdf4j.opentelemetry.repository}).
 * <p>
 * Instances are immutable and created via {@link #builder()}, or use {@link #defaultConfig()} for an instance that uses
 * {@link GlobalOpenTelemetry#get()} and records no SPARQL query/update text.
 *
 * @see OpenTelemetrySupport
 */
public final class RDF4JOpenTelemetryConfig {

	/**
	 * The default instrumentation scope name used to obtain a {@link Tracer} from the configured {@link OpenTelemetry}
	 * instance.
	 */
	public static final String DEFAULT_INSTRUMENTATION_SCOPE_NAME = "org.eclipse.rdf4j.opentelemetry";

	/**
	 * System property used to configure the {@link Builder#captureQueryText(boolean)} default, e.g. for deployments
	 * that cannot set it programmatically (such as the RDF4J Server).
	 */
	public static final String CAPTURE_QUERY_TEXT_PROPERTY = "org.eclipse.rdf4j.opentelemetry.captureQueryText";

	/**
	 * The default value of {@link Builder#captureQueryText(boolean)}, used unless overridden by the
	 * {@value #CAPTURE_QUERY_TEXT_PROPERTY} system property.
	 */
	public static final boolean DEFAULT_CAPTURE_QUERY_TEXT = false;

	/**
	 * System property used to configure the {@link Builder#maxQueryTextLength(int)} default, e.g. for deployments that
	 * cannot set it programmatically (such as the RDF4J Server).
	 */
	public static final String MAX_QUERY_TEXT_LENGTH_PROPERTY = "org.eclipse.rdf4j.opentelemetry.maxQueryTextLength";

	/**
	 * The default maximum length, in characters, for the captured SPARQL query/update text attribute, used unless
	 * overridden by the {@value #MAX_QUERY_TEXT_LENGTH_PROPERTY} system property.
	 */
	public static final int DEFAULT_MAX_QUERY_TEXT_LENGTH = 1000;

	/**
	 * System property used to configure the {@link Builder#dbSystemName(String)} default, e.g. for deployments that
	 * cannot set it programmatically (such as the RDF4J Server).
	 */
	public static final String DB_SYSTEM_NAME_PROPERTY = "org.eclipse.rdf4j.opentelemetry.dbSystemName";

	/**
	 * The default value of the {@code db.system.name} span attribute recorded by the
	 * {@code org.eclipse.rdf4j.opentelemetry.repository} instrumentation, used unless overridden by the
	 * {@value #DB_SYSTEM_NAME_PROPERTY} system property.
	 */
	public static final String DEFAULT_DB_SYSTEM_NAME = "rdf4j";

	private final OpenTelemetry openTelemetry;
	private final String instrumentationScopeName;
	private final String instrumentationScopeVersion;
	private final boolean captureQueryText;
	private final int maxQueryTextLength;
	private final String dbSystemName;

	private RDF4JOpenTelemetryConfig(Builder builder) {
		this.openTelemetry = builder.openTelemetry;
		this.instrumentationScopeName = builder.instrumentationScopeName;
		this.instrumentationScopeVersion = builder.instrumentationScopeVersion;
		this.captureQueryText = builder.captureQueryText;
		this.maxQueryTextLength = builder.maxQueryTextLength;
		this.dbSystemName = builder.dbSystemName;
	}

	/**
	 * @return a config using {@link GlobalOpenTelemetry#get()}, with query/update text capture disabled.
	 */
	public static RDF4JOpenTelemetryConfig defaultConfig() {
		return builder().build();
	}

	/**
	 * @return a new {@link Builder} for a custom {@link RDF4JOpenTelemetryConfig}.
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * @return the {@link OpenTelemetry} instance used to create tracers and propagators.
	 */
	public OpenTelemetry getOpenTelemetry() {
		return openTelemetry;
	}

	/**
	 * @return whether the SPARQL query/update text is recorded as a span attribute. Disabled by default, since query
	 *         text can be large and may contain sensitive literal values.
	 */
	public boolean isCaptureQueryText() {
		return captureQueryText;
	}

	/**
	 * @return the maximum number of characters recorded for the query/update text attribute, when
	 *         {@link #isCaptureQueryText()} is {@code true}.
	 */
	public int getMaxQueryTextLength() {
		return maxQueryTextLength;
	}

	/**
	 * @return the value recorded for the {@code db.system.name} span attribute by the
	 *         {@code org.eclipse.rdf4j.opentelemetry.repository} instrumentation. Defaults to
	 *         {@value #DEFAULT_DB_SYSTEM_NAME}.
	 */
	public String getDbSystemName() {
		return dbSystemName;
	}

	/**
	 * @return a {@link Tracer} obtained from {@link #getOpenTelemetry()} using the configured instrumentation scope
	 *         name/version.
	 */
	public Tracer getTracer() {
		return instrumentationScopeVersion != null
				? openTelemetry.getTracer(instrumentationScopeName, instrumentationScopeVersion)
				: openTelemetry.getTracer(instrumentationScopeName);
	}

	/**
	 * Builder for {@link RDF4JOpenTelemetryConfig}.
	 */
	public static final class Builder {

		private OpenTelemetry openTelemetry;
		private String instrumentationScopeName = DEFAULT_INSTRUMENTATION_SCOPE_NAME;
		private String instrumentationScopeVersion;
		private boolean captureQueryText = Boolean.getBoolean(CAPTURE_QUERY_TEXT_PROPERTY);
		private int maxQueryTextLength = Integer.getInteger(MAX_QUERY_TEXT_LENGTH_PROPERTY,
				DEFAULT_MAX_QUERY_TEXT_LENGTH);
		private String dbSystemName = System.getProperty(DB_SYSTEM_NAME_PROPERTY, DEFAULT_DB_SYSTEM_NAME);

		private Builder() {
		}

		/**
		 * Sets the {@link OpenTelemetry} instance to use. Defaults to {@link GlobalOpenTelemetry#get()} when not set.
		 *
		 * @param openTelemetry the {@link OpenTelemetry} instance; must not be {@code null}
		 * @return this builder
		 */
		public Builder openTelemetry(OpenTelemetry openTelemetry) {
			this.openTelemetry = Objects.requireNonNull(openTelemetry, "openTelemetry must not be null");
			return this;
		}

		/**
		 * Sets the instrumentation scope name used when obtaining a {@link Tracer}. Defaults to
		 * {@value #DEFAULT_INSTRUMENTATION_SCOPE_NAME}.
		 *
		 * @param instrumentationScopeName the scope name; must not be {@code null}
		 * @return this builder
		 */
		public Builder instrumentationScopeName(String instrumentationScopeName) {
			this.instrumentationScopeName = Objects.requireNonNull(instrumentationScopeName,
					"instrumentationScopeName must not be null");
			return this;
		}

		/**
		 * Sets the instrumentation scope version used when obtaining a {@link Tracer}.
		 *
		 * @param instrumentationScopeVersion the scope version, or {@code null} for none
		 * @return this builder
		 */
		public Builder instrumentationScopeVersion(String instrumentationScopeVersion) {
			this.instrumentationScopeVersion = instrumentationScopeVersion;
			return this;
		}

		/**
		 * Enables or disables recording the SPARQL query/update text as a span attribute. Disabled by default, unless
		 * overridden by the {@value #CAPTURE_QUERY_TEXT_PROPERTY} system property.
		 *
		 * @param captureQueryText {@code true} to record (truncated) query/update text
		 * @return this builder
		 */
		public Builder captureQueryText(boolean captureQueryText) {
			this.captureQueryText = captureQueryText;
			return this;
		}

		/**
		 * Sets the maximum number of characters recorded for the query/update text attribute. Defaults to
		 * {@value #DEFAULT_MAX_QUERY_TEXT_LENGTH}, unless overridden by the {@value #MAX_QUERY_TEXT_LENGTH_PROPERTY}
		 * system property.
		 *
		 * @param maxQueryTextLength a non-negative character count
		 * @return this builder
		 */
		public Builder maxQueryTextLength(int maxQueryTextLength) {
			if (maxQueryTextLength < 0) {
				throw new IllegalArgumentException("maxQueryTextLength must not be negative");
			}
			this.maxQueryTextLength = maxQueryTextLength;
			return this;
		}

		/**
		 * Sets the value recorded for the {@code db.system.name} span attribute by the
		 * {@code org.eclipse.rdf4j.opentelemetry.repository} instrumentation. Defaults to
		 * {@value #DEFAULT_DB_SYSTEM_NAME}, unless overridden by the {@value #DB_SYSTEM_NAME_PROPERTY} system property.
		 *
		 * @param dbSystemName the system name; must not be {@code null}
		 * @return this builder
		 */
		public Builder dbSystemName(String dbSystemName) {
			this.dbSystemName = Objects.requireNonNull(dbSystemName, "dbSystemName must not be null");
			return this;
		}

		/**
		 * @return a new, immutable {@link RDF4JOpenTelemetryConfig}.
		 */
		public RDF4JOpenTelemetryConfig build() {
			if (openTelemetry == null) {
				openTelemetry = GlobalOpenTelemetry.get();
			}
			return new RDF4JOpenTelemetryConfig(this);
		}
	}
}
