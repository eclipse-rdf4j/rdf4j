/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.graphpattern;

import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfBlankNode.PropertiesBlankNode;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicateObjectList;

/**
 * A triple pattern formed by a property-list blank node
 *
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynBlankNodes"> blank node syntax</a>
 */
class BNodeTriplePattern implements TriplePattern {
	private final PropertiesBlankNode bnode;

	BNodeTriplePattern(PropertiesBlankNode subject) {
		this.bnode = subject;
	}

	@Override
	public BNodeTriplePattern andHas(RdfPredicateObjectList... lists) {
		bnode.andHas(lists);

		return this;
	}

	@Override
	public String getQueryString() {
		return bnode.getQueryString() + SUFFIX;
	}
}
