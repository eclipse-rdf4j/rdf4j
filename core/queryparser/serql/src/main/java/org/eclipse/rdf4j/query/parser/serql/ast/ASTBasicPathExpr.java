/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql.ast;

public class ASTBasicPathExpr extends ASTPathExpr {

	public ASTBasicPathExpr(int id) {
		super(id);
	}

	public ASTBasicPathExpr(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
		return visitor.visit(this, data);
	}

	public ASTNode getHead() {
		return (ASTNode) children.get(0);
	}

	public ASTBasicPathExprTail getTail() {
		return (ASTBasicPathExprTail) children.get(1);
	}
}
