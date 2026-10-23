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
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/** Traces an {@link Update} execution as a {@code db.operation.name = UPDATE} span. */
class TracingUpdate extends TracingOperation<Update> implements Update {

	TracingUpdate(Update delegate, String operationString, String repositoryId, RDF4JOpenTelemetryConfig config,
			Tracer tracer) {
		super(delegate, operationString, repositoryId, config, tracer);
	}

	@Override
	public void execute() throws UpdateExecutionException {
		Span span = startSpan();
		try (Scope scope = span.makeCurrent()) {
			getDelegate().execute();
		} catch (RuntimeException e) {
			recordException(e, span);
			throw e;
		} finally {
			span.end();
		}
	}

	@Override
	protected String getOperationName() {
		return "UPDATE";
	}
}
