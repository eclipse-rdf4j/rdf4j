/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.ast;

import java.util.List;

/**
 * @author jeen
 */
public abstract class ASTOperation extends SimpleNode {

	/**
	 * @param id
	 */
	public ASTOperation(int id) {
		super(id);
	}

	/**
	 * @param p
	 * @param id
	 */
	public ASTOperation(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	public List<ASTDatasetClause> getDatasetClauseList() {
		return jjtGetChildren(ASTDatasetClause.class);
	}
	
	
}
