/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import java.io.IOException;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.URI;

@Deprecated
public interface SearchQuery {

	/**
	 * Queries for the given subject or all subjects if null.
	 */
	Iterable<? extends DocumentScore> query(Resource subject)
		throws IOException;

	/**
	 * Highlights the given field or all fields if null.
	 */
	void highlight(URI property);
}
