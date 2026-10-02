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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.opentelemetry.RDF4JOpenTelemetryConfig;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.repository.sparql.query.QueryStringUtil;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;

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
		Span span = startSpan(tracer, config, repositoryId, getOperationName());

		if (config.isCaptureQueryText() && operationString != null) {
			span.setAttribute(DbOtelAttributes.DB_QUERY_TEXT,
					truncate(operationString, config.getMaxQueryTextLength()));
		}

		if (config.isCaptureQueryParameters()) {
			BindingSet bindings = getBindings();
			if (bindings != null) {
				for (Binding binding : bindings) {
					span.setAttribute(DbOtelAttributes.queryParameterKey(binding.getName()),
							truncate(valueToString(binding.getValue()), config.getMaxQueryTextLength()));
				}
			}
		}

		return span;
	}

	/**
	 * Converts a {@link Value} to a query-string-like representation, e.g. for use in {@code db.query.parameter.*}/
	 * {@code db.query.text}-style attributes. {@code QueryStringUtil} only supports {@link IRI}/{@link Literal} values
	 * (SPARQL syntax can't express a {@code BNode} or {@code TripleTerm}), so those are rendered via N-Triples syntax
	 * instead; tracing must never fail/alter the actual operation just because a value can't be expressed as a SPARQL
	 * term.
	 */
	static String valueToString(Value value) {
		if (value instanceof IRI || value instanceof Literal) {
			return QueryStringUtil.valueToString(value);
		}
		return NTriplesUtil.toNTriplesString(value);
	}

	/**
	 * Starts a new CLIENT span with the standard {@code db.*} attributes attached (excluding {@code db.query.text},
	 * which requires an operation string to capture), for use by connection-level operations that aren't a
	 * {@link Operation} (e.g. {@code getStatements}/{@code hasStatement}). The caller is responsible for ending the
	 * span.
	 */
	static Span startSpan(Tracer tracer, RDF4JOpenTelemetryConfig config, String repositoryId, String operationName) {
		String spanName = operationName + " " + repositoryId;

		Span span = tracer.spanBuilder(spanName)
				.setSpanKind(SpanKind.CLIENT)
				.startSpan();

		span.setAttribute(DbOtelAttributes.DB_SYSTEM_NAME, config.getDbSystemName());
		span.setAttribute(DbOtelAttributes.DB_OPERATION_NAME, operationName);
		span.setAttribute(DbOtelAttributes.DB_NAMESPACE, repositoryId);
		span.setAttribute(DbOtelAttributes.DB_QUERY_SUMMARY, spanName);

		return span;
	}

	/**
	 * @return {@code text}, truncated to {@code maxLength} characters if it exceeds that length.
	 */
	static String truncate(String text, int maxLength) {
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
