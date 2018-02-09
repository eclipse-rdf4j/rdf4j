/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql.ast;

public class ASTEdge extends SimpleNode {

	public ASTEdge(int id) {
		super(id);
	}

	public ASTEdge(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}

	/**
	 * Gets the value expression for this edge.
	 * 
	 * @return An {@link ASTVar}, {@link ASTURI} or {@link ASTQName} object.
	 */
	public ASTValueExpr getValueExpr() {
		return (ASTValueExpr)children.get(0);
	}
}
