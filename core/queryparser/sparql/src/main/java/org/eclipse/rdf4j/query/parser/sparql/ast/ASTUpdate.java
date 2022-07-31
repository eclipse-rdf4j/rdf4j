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

/**
 * @author jeen
 */
public abstract class ASTUpdate extends ASTOperation {

	/**
	 * @param id
	 */
	protected ASTUpdate(int id) {
		super(id);
	}

	protected ASTUpdate(SyntaxTreeBuilder p, int id) {
		super(p, id);
	}

}
