/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser.sparql.ast;

import java.util.List;

/**
 * Abstract supertype of {@link ASTQueryContainer} and {@link ASTUpdateContainer}
 *
 * @author Jeen Broekstra
 */
public abstract class ASTOperationContainer extends SimpleNode {

	/**
	 * @param id
	 */
	protected ASTOperationContainer(int id) {
		super(id);
	}

	protected ASTOperationContainer(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

	public ASTBaseDecl getBaseDecl() {
		return super.jjtGetChild(ASTBaseDecl.class);
	}

	public ASTOperation getOperation() {
		return super.jjtGetChild(ASTOperation.class);
	}

	public List<ASTPrefixDecl> getPrefixDeclList() {
		return super.jjtGetChildren(ASTPrefixDecl.class);
	}

	public abstract void setSourceString(String source);

	public abstract String getSourceString();

}
