/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql.ast;


public class ASTFrom extends SimpleNode {

	public ASTFrom(int id) {
		super(id);
	}

	public ASTFrom(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}

	public boolean hasContextID() {
		return getContextID() != null;
	}

	public ASTValueExpr getContextID() {
		Node firstNode = children.get(0);

		if (firstNode instanceof ASTValueExpr) {
			return (ASTValueExpr)firstNode;
		}

		return null;
	}

	public ASTPathExpr getPathExpr() {
		return (ASTPathExpr)children.get(children.size() - 1);
	}
}
