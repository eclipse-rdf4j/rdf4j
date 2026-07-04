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

/**
 * Returns {@link BooleanQueryResultParser}s for a specific boolean query result format.
 *
 * @author Arjohn Kampman
 */
public interface BooleanQueryResultParserFactory {

	/**
	 * Returns the boolean query result format for this factory.
	 */
	BooleanQueryResultFormat getBooleanQueryResultFormat();

	/**
	 * Returns a BooleanQueryResultParser instance.
	 */
	BooleanQueryResultParser getParser();
}
