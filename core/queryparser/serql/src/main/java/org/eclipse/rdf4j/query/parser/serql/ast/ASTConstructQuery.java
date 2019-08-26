/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql.ast;

public class ASTConstructQuery extends ASTGraphQuery {

	public ASTConstructQuery(int id) {
		super(id);
	}

	public ASTConstructQuery(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
		return visitor.visit(this, data);
	}

	public ASTConstruct getConstructClause() {
		return (ASTConstruct) children.get(0);
	}

	public boolean hasQueryBody() {
		return getQueryBody() != null;
	}

	public ASTQueryBody getQueryBody() {
		return jjtGetChild(ASTQueryBody.class);
	}

	public boolean hasOrderBy() {
		return getOrderBy() != null;
	}

	public ASTOrderBy getOrderBy() {
		return jjtGetChild(ASTOrderBy.class);
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
