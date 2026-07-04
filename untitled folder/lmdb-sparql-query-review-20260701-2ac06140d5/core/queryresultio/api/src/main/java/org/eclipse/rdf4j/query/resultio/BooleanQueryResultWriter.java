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

import java.io.IOException;

import org.eclipse.rdf4j.query.BooleanQueryResultHandler;

/**
 * The interface of objects that writer query results in a specific query result format.
 *
 * @author Arjohn Kampman
 */
public interface BooleanQueryResultWriter extends QueryResultWriter, BooleanQueryResultHandler {

	/**
	 * Writes the specified boolean value.
	 *
	 * @deprecated Use {@link #handleBoolean(boolean)} instead.
	 */
	void write(boolean value) throws IOException;

	/**
	 * Gets the query result format that this writer uses.
	 */
	BooleanQueryResultFormat getBooleanQueryResultFormat();

}
