/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core.query;

/**
 * A SPARQL COPY Query
 *
 * @see <a href="https://www.w3.org/TR/sparql11-update/#copy"> SPARQL COPY query</a>
 */
public class CopyQuery extends DestinationSourceManagementQuery<CopyQuery> {
	private static final String COPY = "COPY";

	CopyQuery() {
	}

	@Override
	protected String getQueryActionString() {
		return COPY;
	}
}
