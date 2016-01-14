/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio;

import java.io.OutputStream;
import java.io.Writer;

/**
 * A RDFWriterFactory returns {@link RDFWriter}s for a specific RDF format.
 * 
 * @author Arjohn Kampman
 */
public interface RDFWriterFactory {

	/**
	 * Returns the RDF format for this factory.
	 */
	public RDFFormat getRDFFormat();

	/**
	 * Returns an RDFWriter instance that will write to the supplied output
	 * stream.
	 * 
	 * @param out
	 *        The OutputStream to write the RDF to.
	 */
	public RDFWriter getWriter(OutputStream out);

	/**
	 * Returns an RDFWriter instance that will write to the supplied writer.
	 * (Optional operation)
	 * 
	 * @param writer
	 *        The Writer to write the RDF to.
	 * @throws UnsupportedOperationException
	 *         if the RDFWriter the specific format does not support writing to a
	 *         {@link java.io.Writer}
	 */
	public RDFWriter getWriter(Writer writer);
}
