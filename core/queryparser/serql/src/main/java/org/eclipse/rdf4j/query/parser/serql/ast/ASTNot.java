/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql.ast;

public class ASTNot extends ASTBooleanExpr {

	public ASTNot() {
		this(SyntaxTreeBuilderTreeConstants.JJTNOT);
	}

	public ASTNot(ASTBooleanExpr operand) {
		this();
		setOperand(operand);
	}

	public ASTNot(int id) {
		super(id);
	}

	public ASTNot(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
		return visitor.visit(this, data);
	}

	public ASTBooleanExpr getOperand() {
		return (ASTBooleanExpr) children.get(0);
	}

	public void setOperand(ASTBooleanExpr operand) {
		jjtAddChild(operand, 0);
	}
}
