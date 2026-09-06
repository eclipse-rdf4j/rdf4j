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
package org.eclipse.rdf4j.opentelemetry.repository;

import org.eclipse.rdf4j.opentelemetry.RDF4JOpenTelemetryConfig;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.explanation.Explanation;

import io.opentelemetry.api.trace.Tracer;

/**
 * Base class for {@link Query} wrappers ({@link TracingTupleQuery}/{@link TracingGraphQuery}/
 * {@link TracingBooleanQuery}).
 */
abstract class TracingQuery<T extends Query> extends TracingOperation<T> implements Query {

	TracingQuery(T delegate, String operationString, String repositoryId, RDF4JOpenTelemetryConfig config,
			Tracer tracer) {
		super(delegate, operationString, repositoryId, config, tracer);
	}

	@Override
	public Explanation explain(Explanation.Level level) {
		return getDelegate().explain(level);
	}

	// Query still declares the deprecated setMaxQueryTime/getMaxQueryTime as abstract methods, so a Query wrapper
	// must implement them. They forward to the current setMaxExecutionTime/getMaxExecutionTime API (mirroring
	// RDF4J's own AbstractQuery), so no deprecated method is actually invoked.
	@Deprecated
	@Override
	public void setMaxQueryTime(int maxQueryTime) {
		getDelegate().setMaxExecutionTime(maxQueryTime);
	}

	@Deprecated
	@Override
	public int getMaxQueryTime() {
		return getDelegate().getMaxExecutionTime();
	}
}
