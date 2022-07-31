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
package org.eclipse.rdf4j.query.resultio;

import java.io.OutputStream;

/**
 * Returns {@link BooleanQueryResultWriter}s for a specific boolean query result format.
 *
 * @author Arjohn Kampman
 */
public interface BooleanQueryResultWriterFactory {

	/**
	 * Returns the boolean query result format for this factory.
	 */
	BooleanQueryResultFormat getBooleanQueryResultFormat();

	/**
	 * Returns a {@link BooleanQueryResultWriter} instance that will write to the supplied output stream.
	 *
	 * @param out The OutputStream to write the result to.
	 */
	BooleanQueryResultWriter getWriter(OutputStream out);
}
