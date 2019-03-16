/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.serql.ast;

public abstract class ASTTupleQuerySet extends ASTTupleQuery {

	public ASTTupleQuerySet(int i) {
		super(i);
	}

	public ASTTupleQuerySet(SyntaxTreeBuilder p, int i) {
		super(p, i);
	}

	public ASTTupleQuery getLeftArg() {
		return (ASTTupleQuery) children.get(0);
	}

	public ASTTupleQuery getRightArg() {
		return (ASTTupleQuery) children.get(1);
	}
}
