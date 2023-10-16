/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast;

import java.util.Arrays;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;

/**
 * An exception thrown when the ?failure var is true for a SPARQL constraint select query.
 */
public class ShaclSparqlConstraintFailureException extends RDF4JException {

	private final Shape shape;
	private final String query;
	private final BindingSet resultBindingSet;
	private final Value focusNode;
	private final Resource[] dataGraph;

	public ShaclSparqlConstraintFailureException(Shape shape, String query, BindingSet resultBindingSet,
			Value focusNode, Resource[] dataGraph) {
		super("The ?failure variable was true for " + valueToString(focusNode) + " in shape "
				+ resourceToString(shape.getId()) + " with result resultBindingSet: " + resultBindingSet.toString()
				+ " and dataGraph: " + Arrays.toString(dataGraph) + " and query:" + query);
		this.shape = shape;
		this.query = query;
		this.resultBindingSet = resultBindingSet;
		this.focusNode = focusNode;
		this.dataGraph = dataGraph;
	}

	public String getShape() {
		return shape.toString();
	}

	public String getQuery() {
		return query;
	}

	public BindingSet getResultBindingSet() {
		return resultBindingSet;
	}

	public Value getFocusNode() {
		return focusNode;
	}

	public Resource[] getDataGraph() {
		return dataGraph;
	}

	private static String resourceToString(Resource id) {
		assert id != null;
		if (id == null) {
			return "null";
		}
		if (id.isIRI()) {
			return "<" + id.stringValue() + ">";
		}
		if (id.isBNode()) {
			return id.toString();
		}
		if (id.isTriple()) {
			return "TRIPLE " + id;
		}
		return id.toString();
	}

	private static String valueToString(Value value) {
		assert value != null;
		if (value == null) {
			return "null";
		}
		if (value.isResource()) {
			return resourceToString((Resource) value);
		}
		if (value.isLiteral()) {
			return value.toString();
		}
		return value.toString();
	}
}
