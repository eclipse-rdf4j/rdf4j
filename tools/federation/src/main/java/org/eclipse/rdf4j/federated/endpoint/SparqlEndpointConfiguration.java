/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.endpoint;

import org.eclipse.rdf4j.federated.evaluation.TripleSource;

/**
 * Additional {@link EndpointConfiguration} for SPARQL endpoints.
 *
 * @author Andreas Schwarte
 *
 */
public class SparqlEndpointConfiguration implements EndpointConfiguration {

	private boolean supportsASKQueries = true;

	/**
	 * Flag indicating whether ASK queries are supported. Specific {@link TripleSource} implementations may use this
	 * information to decide whether to use ASK or SELECT for source selection.
	 *
	 * @return boolean indicating whether ASK queries are supported
	 */
	public boolean supportsASKQueries() {
		return supportsASKQueries;
	}

	/**
	 * Define whether this endpoint supports ASK queries.
	 *
	 * @param flag
	 */
	public void setSupportsASKQueries(boolean flag) {
		this.supportsASKQueries = flag;
	}
}
