/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.algebra;

import org.eclipse.rdf4j.query.algebra.QueryModelVisitor;
import org.eclipse.rdf4j.query.algebra.StatementPattern;

/**
 * EmptyStatementPattern represents a statement that cannot produce any results for the registered endpoints.
 *
 * @author Andreas Schwarte
 *
 */
public class EmptyStatementPattern extends StatementPattern implements EmptyResult, BoundJoinTupleExpr {

	private static final long serialVersionUID = 1026901522434201643L;

	public EmptyStatementPattern(StatementPattern node) {
		super(node.getSubjectVar().clone(), node.getPredicateVar().clone(), node.getObjectVar().clone(),
				node.getContextVar() != null ? node.getContextVar().clone() : null);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meetOther(this);
	}
}
