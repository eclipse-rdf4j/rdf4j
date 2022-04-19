/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.graphpattern;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfObject;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicateObjectList;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicateObjectListCollection;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfSubject;

/**
 * A SPARQL Triple Pattern.
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynTriples"> Triple pattern syntax</a>
 */
class TriplesSameSubject implements TriplePattern {
	private final RdfSubject subject;
	private final RdfPredicateObjectListCollection predicateObjectLists = Rdf.predicateObjectListCollection();

	TriplesSameSubject(RdfSubject subject, RdfPredicate predicate, RdfObject... objects) {
		this.subject = subject;
		andHas(predicate, objects);
	}

	TriplesSameSubject(RdfSubject subject, IRI predicate, RdfObject... objects) {
		this(subject, Rdf.iri(predicate), objects);
	}

	TriplesSameSubject(RdfSubject subject, RdfPredicateObjectList... lists) {
		this.subject = subject;
		andHas(lists);
	}

	@Override
	public TriplesSameSubject andHas(RdfPredicateObjectList... lists) {
		predicateObjectLists.andHas(lists);

		return this;
	}

	@Override
	public String getQueryString() {
		return subject.getQueryString() + " " + predicateObjectLists.getQueryString() + SUFFIX;
	}
}
