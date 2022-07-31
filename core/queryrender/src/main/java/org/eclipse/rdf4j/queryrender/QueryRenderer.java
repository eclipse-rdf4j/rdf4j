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
package org.eclipse.rdf4j.queryrender;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.parser.ParsedQuery;

/**
 * <p>
 * Interface for RDF4J-based query renderers
 * </p>
 *
 * @author Michael Grove
 */
public interface QueryRenderer {

	/**
	 * Return the language this QueryRenderer will render queries in.
	 *
	 * @return the query language
	 */
	QueryLanguage getLanguage();

	/**
	 * Render the query object to a string in the language supported by this renderer
	 *
	 * @param theQuery the query to render
	 * @return the rendered query
	 * @throws Exception if there is an error while rendering
	 */
	String render(ParsedQuery theQuery) throws Exception;
}
