/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.ast;

/**
 * @author Jeen
 */
public abstract class ASTAggregate extends SimpleNode {

	private boolean distinct;

	/**
	 * @param id
	 */
	public ASTAggregate(int id) {
		super(id);
	}

	public ASTAggregate(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	public boolean isDistinct() {
		return distinct;
	}

	public void setDistinct(boolean distinct) {
		this.distinct = distinct;
	}

}
