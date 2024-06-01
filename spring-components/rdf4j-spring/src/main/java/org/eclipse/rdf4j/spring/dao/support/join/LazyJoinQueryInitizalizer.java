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

import org.eclipse.rdf4j.spring.support.RDF4JTemplate;

/**
 * Holds a fully configured {@link JoinQueryBuilder} until the time comes at which a {@link RDF4JTemplate} is available
 * and the {@link JoinQuery} can be instantiated via {@link #get(RDF4JTemplate)}. Subsequent calls to
 * {@link #get(RDF4JTemplate)} will always use the same {@link JoinQuery} object.
 *
 * <p>
 * This construct is thread-safe because the JoinQuery's internal state does not change after initialization; rather,
 * when used to perform actual queries, it re-uses or creates a reusable TupleQuery using the {@link RDF4JTemplate}, and
 * uses a {@link JoinQueryEvaluationBuilder} object to encapsulate per-evaluation state.
 *
 * <p>
 * Usually, you would assign this to a static member of a class and obtain the value in an instance method.
 */
public class LazyJoinQueryInitizalizer {
	private JoinQueryBuilder joinQueryBuilder;
	private JoinQuery joinQuery;

	LazyJoinQueryInitizalizer(JoinQueryBuilder joinQueryBuilder) {
		this.joinQueryBuilder = joinQueryBuilder;
	}

	public JoinQueryEvaluationBuilder get(RDF4JTemplate rdf4JTemplate) {
		if (joinQuery != null) {
			return joinQuery.evaluationBuilder(rdf4JTemplate);
		} else {
			synchronized (this) {
				if (this.joinQuery == null) {
					this.joinQuery = this.joinQueryBuilder.build();
				}
			}
		}
		return joinQuery.evaluationBuilder(rdf4JTemplate);
	}
}
