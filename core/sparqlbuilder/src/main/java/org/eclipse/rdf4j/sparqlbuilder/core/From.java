/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;

/**
 * A SPARQL Dataset specifier.
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#specifyingDataset"> Specifying RDF Datasets</a>
 */
public class From implements QueryElement {
	private static final String FROM = "FROM";
	private static final String NAMED = "NAMED";
	private final Iri iri;
	private final boolean isNamed;

	From(Iri iri) {
		this(iri, false);
	}

	From(Iri iri, boolean isNamed) {
		this.iri = iri;
		this.isNamed = isNamed;
	}

	From(IRI iri) {
		this(iri, false);
	}

	From(IRI iri, boolean isNamed) {
		this(iri(iri), isNamed);
	}

	@Override
	public String getQueryString() {
		StringBuilder fromClause = new StringBuilder();

		fromClause.append(FROM).append(" ");
		if (isNamed) {
			fromClause.append(NAMED).append(" ");
		}
		fromClause.append(iri.getQueryString());

		return fromClause.toString();
	}
}
