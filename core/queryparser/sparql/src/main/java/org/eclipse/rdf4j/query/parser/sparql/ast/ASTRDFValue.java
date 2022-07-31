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

import org.eclipse.rdf4j.model.Value;

/**
 * @author jeen
 */
public abstract class ASTRDFValue extends SimpleNode {

	private Value value;

	/**
	 * @param id
	 */
	protected ASTRDFValue(int id) {
		super(id);
	}

	/**
	 * @param parser
	 * @param id
	 */
	protected ASTRDFValue(SyntaxTreeBuilder parser, int id) {
		super(parser, id);
	}

	public Value getRDFValue() {
		return value;

	}

	public void setRDFValue(final Value value) {
		this.value = value;
	}

}
