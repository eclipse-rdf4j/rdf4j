/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.dawg;

import static org.eclipse.rdf4j.query.dawg.DAWGTestResultSetSchema.BOOLEAN;
import static org.eclipse.rdf4j.query.dawg.DAWGTestResultSetSchema.RESULTSET;

import org.eclipse.rdf4j.model.Graph;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.GraphImpl;
import org.eclipse.rdf4j.model.util.GraphUtil;
import org.eclipse.rdf4j.model.util.GraphUtilException;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

/**
 * @author Arjohn Kampman
 */
public class DAWGTestBooleanParser extends AbstractRDFHandler {

	/*-----------*
	 * Variables *
	 *-----------*/

	private Graph graph = new GraphImpl();

	private boolean value;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public DAWGTestBooleanParser() {
	}

	/*---------*
	 * Methods *
	 *---------*/

	public boolean getValue() {
		return value;
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		graph.clear();
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		graph.add(st);
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		try {
			Resource resultSetNode = GraphUtil.getUniqueSubject(graph, RDF.TYPE, RESULTSET);
			Literal booleanLit = GraphUtil.getUniqueObjectLiteral(graph, resultSetNode, BOOLEAN);

			if (booleanLit.equals(DAWGTestResultSetSchema.TRUE)) {
				value = true;
			} else if (booleanLit.equals(DAWGTestResultSetSchema.FALSE)) {
				value = false;
			} else {
				throw new RDFHandlerException("Invalid boolean value: " + booleanLit);
			}
		} catch (GraphUtilException e) {
			throw new RDFHandlerException(e.getMessage(), e);
		}
	}
}
