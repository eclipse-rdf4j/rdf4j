/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.trig;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFParserFactory;

/**
 * An {@link RDFParserFactory} for TriG parsers.
 * 
 * @author Arjohn Kampman
 */
public class TriGParserFactory implements RDFParserFactory {

	/**
	 * Returns {@link RDFFormat#TRIG}.
	 */
	public RDFFormat getRDFFormat() {
		return RDFFormat.TRIG;
	}

	/**
	 * Returns a new instance of {@link TriGParser}.
	 */
	public RDFParser getParser() {
		return new TriGParser();
	}
}
