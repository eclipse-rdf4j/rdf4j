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
package org.eclipse.rdf4j.rio.helpers;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

/**
 * Base class for {@link RDFHandler}s with dummy implementations of all methods.
 */
public abstract class AbstractRDFHandler implements RDFHandler {

	@Override
	public void startRDF() throws RDFHandlerException {
	}

	@Override
	public void endRDF() throws RDFHandlerException {
	}

	@Override
	public void handleNamespace(String prefix, String uri) throws RDFHandlerException {
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
	}

	@Override
	public void handleComment(String comment) throws RDFHandlerException {
	}
}
