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
 * Base class for {@link RDFHandler}s with dummy implementations of all
 * methods.
 * 
 * @since 4.0
 */
public abstract class AbstractRDFHandler implements RDFHandler {

	public void startRDF()
		throws RDFHandlerException
	{
	}

	public void endRDF()
		throws RDFHandlerException
	{
	}

	public void handleNamespace(String prefix, String uri)
		throws RDFHandlerException
	{
	}

	public void handleStatement(Statement st)
		throws RDFHandlerException
	{
	}

	public void handleComment(String comment)
		throws RDFHandlerException
	{
	}
}
