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
package org.eclipse.rdf4j.federated.algebra;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TripleRef;

/**
 * A {@link FedXStatementPattern} representing a {@link StatementPattern} together with a {@link TripleRef}, i.e. a
 * SPARQL / RDF 1.2 expression.
 */
public class TripleRefStatementPattern extends FedXStatementPattern {

	private static final long serialVersionUID = 841877125206379474L;
	protected final TripleRef tripleRef;

	public TripleRefStatementPattern(StatementPattern node, TripleRef tripleRef, QueryInfo queryInfo) {
		super(node, queryInfo);
		this.tripleRef = tripleRef;
		refineFreeVars();
	}

	protected void refineFreeVars() {
		freeVars.clear();

		// main statement subject
		if (getSubjectVar().getValue() == null) {
			freeVars.add(getSubjectVar().getName());
		}

		// main statement predicate
		if (getPredicateVar().getValue() == null) {
			freeVars.add(getPredicateVar().getName());
		}

		// triple ref subject
		if (tripleRef.getSubjectVar().getValue() == null) {
			freeVars.add(tripleRef.getSubjectVar().getName());
		}

		// triple ref predicate
		if (tripleRef.getPredicateVar().getValue() == null) {
			freeVars.add(tripleRef.getPredicateVar().getName());
		}

		// triple ref predicate
		if (tripleRef.getObjectVar().getValue() == null) {
			freeVars.add(tripleRef.getObjectVar().getName());
		}
	}

	public TripleRef getTripleRef() {
		return tripleRef;
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {

		super.visitChildren(visitor);

		tripleRef.visit(visitor);
	}

	@Override
	public CloseableIteration<BindingSet> evaluate(BindingSet bindings) throws QueryEvaluationException {
		// TODO Auto-generated method stub
		return null;
	}

}
