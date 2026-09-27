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

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.opentelemetry.RDF4JOpenTelemetryConfig;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.Operation;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

/**
 * Base class for {@link Operation} wrappers ({@link TracingQuery}/{@link TracingUpdate}) that forward every
 * {@link Operation} method to a delegate and centralize span creation. RDF4J core has no "delegating operation" base of
 * its own ({@link org.eclipse.rdf4j.query.impl.AbstractOperation} stores its own state rather than delegating), so this
 * is a minimal one written for this package.
 */
abstract class TracingOperation<T extends Operation> implements Operation {

	private final T delegate;
	protected final String operationString;
	protected final String repositoryId;
	protected final RDF4JOpenTelemetryConfig config;
	protected final Tracer tracer;

	TracingOperation(T delegate, String operationString, String repositoryId, RDF4JOpenTelemetryConfig config,
			Tracer tracer) {
		this.delegate = delegate;
		this.operationString = operationString;
		this.repositoryId = repositoryId;
		this.config = config;
		this.tracer = tracer;
	}

	protected T getDelegate() {
		return delegate;
	}

	/**
	 * @return the {@code db.operation.name} value for this operation, e.g. {@code "SELECT"} or {@code "UPDATE"}.
	 */
	protected abstract String getOperationName();

	/**
	 * Starts a new CLIENT span for this operation, with the standard {@code db.*} attributes attached. The caller is
	 * responsible for ending the span.
	 */
	protected Span startSpan() {
		String operationName = getOperationName();
		String spanName = operationName + " " + repositoryId;

		Span span = tracer.spanBuilder(spanName)
				.setSpanKind(SpanKind.CLIENT)
				.startSpan();

		span.setAttribute(DbOtelAttributes.DB_SYSTEM_NAME, config.getDbSystemName());
		span.setAttribute(DbOtelAttributes.DB_OPERATION_NAME, operationName);
		span.setAttribute(DbOtelAttributes.DB_NAMESPACE, repositoryId);
		span.setAttribute(DbOtelAttributes.DB_QUERY_SUMMARY, spanName);

		if (config.isCaptureQueryText() && operationString != null) {
			span.setAttribute(DbOtelAttributes.DB_QUERY_TEXT, truncate(operationString));
		}

		return span;
	}

	private String truncate(String text) {
		int maxLength = config.getMaxQueryTextLength();
		return text.length() > maxLength ? text.substring(0, maxLength) : text;
	}

	/**
	 * Records a failed operation on the given span: sets the span status to {@code ERROR}, records the exception, and
	 * sets the {@code error.type} attribute.
	 */
	static void recordException(Throwable t, Span span) {
		span.recordException(t);
		span.setStatus(StatusCode.ERROR, t.getMessage() != null ? t.getMessage() : t.getClass().getName());
		span.setAttribute(DbOtelAttributes.ERROR_TYPE, t.getClass().getName());
	}

	@Override
	public void setBinding(String name, Value value) {
		getDelegate().setBinding(name, value);
	}

	@Override
	public void removeBinding(String name) {
		getDelegate().removeBinding(name);
	}

	@Override
	public void clearBindings() {
		getDelegate().clearBindings();
	}

	@Override
	public BindingSet getBindings() {
		return getDelegate().getBindings();
	}

	@Override
	public void setDataset(Dataset dataset) {
		getDelegate().setDataset(dataset);
	}

	@Override
	public Dataset getDataset() {
		return getDelegate().getDataset();
	}

	@Override
	public void setIncludeInferred(boolean includeInferred) {
		getDelegate().setIncludeInferred(includeInferred);
	}

	@Override
	public boolean getIncludeInferred() {
		return getDelegate().getIncludeInferred();
	}

	@Override
	public void setMaxExecutionTime(int maxExecutionTimeSeconds) {
		getDelegate().setMaxExecutionTime(maxExecutionTimeSeconds);
	}

	@Override
	public int getMaxExecutionTime() {
		return getDelegate().getMaxExecutionTime();
	}
}
