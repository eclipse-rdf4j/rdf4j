/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql.ast;

public abstract class ASTGraphQuerySet extends ASTGraphQuery {

	public ASTGraphQuerySet(int i) {
		super(i);
	}

	public ASTGraphQuerySet(SyntaxTreeBuilder p, int i) {
		super(p, i);
	}

	public ASTGraphQuery getLeftArg() {
		return (ASTGraphQuery) children.get(0);
	}

	public ASTGraphQuery getRightArg() {
		return (ASTGraphQuery) children.get(1);
	}
}
