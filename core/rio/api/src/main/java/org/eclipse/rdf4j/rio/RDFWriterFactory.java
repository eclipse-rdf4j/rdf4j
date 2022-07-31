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

import java.io.OutputStream;
import java.io.Writer;
import java.net.URISyntaxException;

/**
 * A RDFWriterFactory returns {@link RDFWriter}s for a specific RDF format.
 *
 * @author Arjohn Kampman
 */
public interface RDFWriterFactory {

	/**
	 * Returns the RDF format for this factory.
	 */
	RDFFormat getRDFFormat();

	/**
	 * Returns an RDFWriter instance that will write to the supplied output stream.
	 *
	 * @param out The OutputStream to write the RDF to.
	 */
	RDFWriter getWriter(OutputStream out);

	/**
	 * Returns an RDFWriter instance that will write to the supplied output stream. Using the supplied baseURI to
	 * relativize IRIs to relative IRIs.
	 *
	 * @param out     The OutputStream to write the RDF to.
	 * @param baseURI The URI associated with the data in the InputStream.
	 * @throws URISyntaxException
	 */
	RDFWriter getWriter(OutputStream out, String baseURI) throws URISyntaxException;

	/**
	 * Returns an RDFWriter instance that will write to the supplied writer. (Optional operation)
	 *
	 * @param writer The Writer to write the RDF to.
	 * @throws UnsupportedOperationException if the RDFWriter the specific format does not support writing to a
	 *                                       {@link java.io.Writer}
	 */
	RDFWriter getWriter(Writer writer);

	/**
	 * Returns an RDFWriter instance that will write to the supplied writer. Using the supplied baseURI to relativize
	 * IRIs to relative IRIs. (Optional operation)
	 *
	 * @param writer  The Writer to write the RDF to.
	 * @param baseURI The URI associated with the data in the InputStream.
	 * @throws URISyntaxException
	 * @throws UnsupportedOperationException if the RDFWriter the specific format does not support writing to a
	 *                                       {@link java.io.Writer}
	 */
	RDFWriter getWriter(Writer writer, String baseURI) throws URISyntaxException;
}
