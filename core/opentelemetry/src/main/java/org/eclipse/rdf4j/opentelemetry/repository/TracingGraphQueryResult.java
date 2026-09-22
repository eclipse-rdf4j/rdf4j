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

import java.util.Map;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResult;

import io.opentelemetry.api.trace.Span;

/** A {@link TracingQueryResult} for {@link GraphQueryResult}s. */
class TracingGraphQueryResult extends TracingQueryResult<Statement> implements GraphQueryResult {

	TracingGraphQueryResult(QueryResult<Statement> delegate, Span span) {
		super(delegate, span);
	}

	@Override
	public Map<String, String> getNamespaces() throws QueryEvaluationException {
		return ((GraphQueryResult) getDelegate()).getNamespaces();
	}
}
