/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

/**
 * Convenience base class for RDF handlers that wrap one or more other RDF handler. This class provides default methods
 * that forward method calls to the wrapped RDF handler(s).
 * 
 * @author Arjohn Kampman
 * @author Jeen Broekstra
 */
public class RDFHandlerWrapper implements RDFHandler {

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The wrapped RDF handler(s).
	 */
	private final RDFHandler[] rdfHandlers;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new RDFHandlerWrapper that wraps the supplied RDF handler(s). If more than one RDFHandler is supplied
	 * for wrapping, the RDFHandlerWrapper forwards every method call to each of the supplied handler, in the order in
	 * which the handlers are supplied.
	 * 
	 * @param rdfHandlers One or more wrapped RDF handlers for this <tt>RDFHandlerWrapper</tt>, must not be
	 *                    <tt>null</tt>.
	 */
	public RDFHandlerWrapper(RDFHandler... rdfHandlers) {
		assert rdfHandlers != null;
		this.rdfHandlers = rdfHandlers;
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void startRDF() throws RDFHandlerException {
		for (RDFHandler rdfHandler : rdfHandlers) {
			rdfHandler.startRDF();
		}
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		for (RDFHandler rdfHandler : rdfHandlers) {
			rdfHandler.endRDF();
		}
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
		for (RDFHandler rdfHandler : rdfHandlers) {
			rdfHandler.handleNamespace(prefix, uri);
		}
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		for (RDFHandler rdfHandler : rdfHandlers) {
			rdfHandler.handleStatement(st);
		}
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
		for (RDFHandler rdfHandler : rdfHandlers) {
			rdfHandler.handleComment(comment);
		}
	}
}
