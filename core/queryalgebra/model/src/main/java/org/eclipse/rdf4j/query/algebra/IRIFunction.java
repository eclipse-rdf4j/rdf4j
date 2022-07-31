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
package org.eclipse.rdf4j.query.algebra;

/**
 * The IRI function, as defined in <a href="http://www.w3.org/TR/sparql11-query/#SparqlOps">SPARQL 1.1 Query Language
 * for RDF</a>.
 *
 * @author Jeen Broekstra
 */
public class IRIFunction extends UnaryValueOperator {

	private String baseURI;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public IRIFunction() {
	}

	public IRIFunction(ValueExpr arg) {
		super(arg);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof IRIFunction && super.equals(other);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ "IRI".hashCode();
	}

	@Override
	public IRIFunction clone() {
		return (IRIFunction) super.clone();
	}

	/**
	 * @param baseURI The baseURI to set.
	 */
	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
	}

	/**
	 * @return Returns the baseURI.
	 */
	public String getBaseURI() {
		return baseURI;
	}
}
