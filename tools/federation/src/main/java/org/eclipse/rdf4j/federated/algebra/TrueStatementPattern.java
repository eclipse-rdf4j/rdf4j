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

import org.eclipse.rdf4j.query.algebra.StatementPattern;

/**
 * A TrueStatementPattern represents a stmt with no free variables which in addition is available at one of the provided
 * sources.
 *
 * @author Andreas Schwarte
 *
 */
public class TrueStatementPattern extends StatementPattern implements BoundJoinTupleExpr {

	private static final long serialVersionUID = -5389629235610754092L;

	public TrueStatementPattern(StatementPattern node) {
		super(node.getSubjectVar(), node.getPredicateVar(), node.getObjectVar(), node.getContextVar());
	}
}
