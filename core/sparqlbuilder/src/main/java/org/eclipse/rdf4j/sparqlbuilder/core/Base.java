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
 * A SPARQL Base declaration
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#relIRIs"> SPARQL Relative IRIs</a>
 */
public class Base implements QueryElement {
	private static final String BASE = "BASE ";

	private final Iri iri;

	Base(Iri iri) {
		this.iri = iri;
	}

	Base(IRI iri) {
		this(iri(iri));
	}

	@Override
	public String getQueryString() {
		return BASE + iri.getQueryString();
	}
}
