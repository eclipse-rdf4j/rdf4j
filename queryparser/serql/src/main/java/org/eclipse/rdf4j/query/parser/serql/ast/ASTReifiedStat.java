/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql.ast;

public class ASTReifiedStat extends SimpleNode {

	private ASTVar id;

	public ASTReifiedStat(int id) {
		super(id);
	}

	public ASTReifiedStat(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	@Override
	public Object jjtAccept(SyntaxTreeBuilderVisitor visitor, Object data) throws VisitorException {
		return visitor.visit(this, data);
	}

	public ASTVar getID() {
		return id;
	}

	public void setID(ASTVar id) {
		this.id = id;
	}

	public ASTNodeElem getSubject() {
		return (ASTNodeElem) children.get(0);
	}

	public ASTEdge getPredicate() {
		return (ASTEdge) children.get(1);
	}

	public ASTNodeElem getObject() {
		return (ASTNodeElem) children.get(2);
	}
}
