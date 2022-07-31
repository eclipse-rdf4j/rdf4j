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
package org.eclipse.rdf4j.rio;

/**
 * A RDFParserFactory returns {@link RDFParser}s for a specific RDF format.
 *
 * @author Arjohn Kampman
 */
public interface RDFParserFactory {

	/**
	 * Returns the RDF format for this factory.
	 */
	RDFFormat getRDFFormat();

	/**
	 * Returns a RDFParser instance.
	 */
	RDFParser getParser();
}
