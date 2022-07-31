/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.ast;

public abstract class ASTQuery extends ASTOperation {

	protected ASTQuery(int id) {
		super(id);
	}

	protected ASTQuery(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	public ASTOrderClause getOrderClause() {
		return jjtGetChild(ASTOrderClause.class);
	}

	public ASTGroupClause getGroupClause() {
		return jjtGetChild(ASTGroupClause.class);
	}

	public ASTHavingClause getHavingClause() {
		return jjtGetChild(ASTHavingClause.class);
	}

	public ASTBindingsClause getBindingsClause() {
		return jjtGetChild(ASTBindingsClause.class);
	}

	public boolean hasLimit() {
		return getLimit() != null;
	}

	public ASTLimit getLimit() {
		return jjtGetChild(ASTLimit.class);
	}

	public boolean hasOffset() {
		return getOffset() != null;
	}

	public ASTOffset getOffset() {
		return jjtGetChild(ASTOffset.class);
	}
}
