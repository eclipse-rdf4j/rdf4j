/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.helpers;

import java.util.stream.Stream;

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

	private final String indentString = "   ";

	private final StringBuilder sb;

	private int indentLevel = 0;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public QueryModelTreePrinter() {
		sb = new StringBuilder(256);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public String getTreeString() {
		return sb.toString();
	}

	@Override
	protected void meetNode(QueryModelNode node) {
		for (int i = 0; i < indentLevel; i++) {
			sb.append(indentString);
		}

		sb.append(node.getSignature());
		appendCostAnnotation(node, sb);
		sb.append(LINE_SEPARATOR);

		indentLevel++;

		super.meetNode(node);

		indentLevel--;
	}

	/**
	 *
	 * @return Human readable number. Eg. 12.1M for 1212213.4 and UNKNOWN for -1.
	 */
	static String toHumanReadableNumber(double number) {
		String humanReadbleString;
		if (number == Double.POSITIVE_INFINITY) {
			humanReadbleString = "∞";
		} else if (number > 1_000_000) {
			humanReadbleString = Math.round(number / 100_000) / 10.0 + "M";
		} else if (number > 1_000) {
			humanReadbleString = Math.round(number / 100) / 10.0 + "K";
		} else if (number >= 0) {
			humanReadbleString = Math.round(number) + "";
		} else {
			humanReadbleString = "UNKNOWN";
		}

		return humanReadbleString;
	}

	private static void appendCostAnnotation(QueryModelNode node, StringBuilder sb) {
		String costs = Stream.of(
				"costEstimate=" + toHumanReadableNumber(node.getCostEstimate()),
				"resultSizeEstimate=" + toHumanReadableNumber(node.getResultSizeEstimate()),
				"resultSizeActual=" + toHumanReadableNumber(node.getResultSizeActual()))
				.filter(s -> !s.endsWith("UNKNOWN"))
				.reduce((a, b) -> a + ", " + b)
				.orElse("");

		if (!costs.isEmpty()) {
			sb.append(" (").append(costs).append(")");
		}
	}

}
