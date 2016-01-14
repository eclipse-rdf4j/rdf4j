/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql.ast;

public class ASTLike extends ASTBooleanExpr {

	private boolean ignoreCase = false;

	public ASTLike(int id) {
		super(id);
	}

	public ASTLike(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data)
		throws VisitorException
	{
		return visitor.visit(this, data);
	}

	public boolean ignoreCase() {
		return ignoreCase;
	}

	public void setIgnoreCase(boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
	}

	public ASTValueExpr getValueExpr() {
		return (ASTValueExpr)children.get(0);
	}

	public ASTString getPattern() {
		return (ASTString)children.get(1);
	}

	@Override
	public String toString()
	{
		String result = super.toString();

		if (ignoreCase) {
			result += " (ignore case)";
		}

		return result;
	}
}
