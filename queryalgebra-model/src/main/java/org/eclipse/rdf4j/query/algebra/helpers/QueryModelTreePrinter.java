/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.helpers;

import org.eclipse.rdf4j.query.algebra.QueryModelNode;

/**
 * QueryModelVisitor implementation that "prints" a tree representation of a query model. The tree representations is
 * printed to an internal character buffer and can be retrieved using {@link #getTreeString()}. As an alternative, the
 * static utility method {@link #printTree(QueryModelNode)} can be used.
 */
public class QueryModelTreePrinter extends AbstractQueryModelVisitor<RuntimeException> {

	/*-----------*
	 * Constants *
	 *-----------*/

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	/*-----------*
	 * Constants *
	 *-----------*/

	public static String printTree(QueryModelNode node) {
		QueryModelTreePrinter treePrinter = new QueryModelTreePrinter();
		node.visit(treePrinter);
		return treePrinter.getTreeString();
	}

	/*-----------*
	 * Variables *
	 *-----------*/

	private String indentString = "   ";

	private StringBuilder buf;

	private int indentLevel = 0;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public QueryModelTreePrinter() {
		buf = new StringBuilder(256);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public String getTreeString() {
		return buf.toString();
	}

	@Override
	protected void meetNode(QueryModelNode node) {
		for (int i = 0; i < indentLevel; i++) {
			buf.append(indentString);
		}

		buf.append(node.getSignature());
		buf.append(LINE_SEPARATOR);

		indentLevel++;

		super.meetNode(node);

		indentLevel--;
	}
}
