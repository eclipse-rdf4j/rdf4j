/*
 * *****************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 * *****************************************************************************
 */

package org.eclipse.rdf4j.spring.dao.support.join;

import java.util.function.Supplier;

import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.spring.support.RDF4JTemplate;

/**
 * Creates a reusable {@link org.eclipse.rdf4j.query.TupleQuery} (and takes care of it getting reused properly using
 * {@link RDF4JTemplate#tupleQuery(Class, String, Supplier)}).
 *
 * <p>
 * The JoinQuery is created using the {@link JoinQueryBuilder}.
 *
 * <p>
 * To set bindings and execute a {@link JoinQuery}, obtain the {@link JoinQueryEvaluationBuilder} via
 * {@link #evaluationBuilder(RDF4JTemplate)}.
 */
public class JoinQuery {

	public static final Variable _sourceEntity = SparqlBuilder.var("sourceEntity");
	public static final Variable _targetEntity = SparqlBuilder.var("targetEntity");

	private final String queryString;

	JoinQuery(JoinQueryBuilder joinQueryBuilder) {
		this.queryString = joinQueryBuilder.makeQueryString();
	}

	public JoinQueryEvaluationBuilder evaluationBuilder(RDF4JTemplate rdf4JTemplate) {
		return new JoinQueryEvaluationBuilder(
				rdf4JTemplate.tupleQuery(
						getClass(),
						this.getClass().getName() + "@" + this.hashCode(),
						() -> this.queryString));
	}
}
