/*******************************************************************************
Copyright (c) 2018 Eclipse RDF4J contributors.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Distribution License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/org/documents/edl-v10.php.
*******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.rdf;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.toRdfLiteralArray;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sparqlbuilder.core.QueryElement;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * Denotes an element that can represent a subject in a {@link TriplePattern}
 */
public interface RdfSubject extends QueryElement {

	/**
	 * Create a triple pattern from this subject and the given predicate and object
	 * 
	 * @param predicate the predicate of the triple pattern
	 * @param objects   the object(s) of the triple pattern
	 * @return a new {@link TriplePattern} with this subject, and the given predicate and object(s)
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynTriples"> Triple pattern syntax</a>
	 */
	default TriplePattern has(RdfPredicate predicate, RdfObject... objects) {
		return GraphPatterns.tp(this, predicate, objects);
	}

	/**
	 * Create a triple pattern from this subject and the given predicate and object
	 * 
	 * @param predicate the predicate of the triple pattern
	 * @param values    the object value(s) of the triple pattern.
	 * @return a new {@link TriplePattern} with this subject, and the given predicate and object(s)
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynTriples"> Triple pattern syntax</a>
	 */
	default TriplePattern has(RdfPredicate predicate, Value... values) {
		return has(predicate, Rdf.objects(values));
	}

	/**
	 * Create a triple pattern from this subject and the given predicate and object
	 * 
	 * @param predicate the predicate {@link IRI} of the triple pattern
	 * @param objects   the object(s) of the triple pattern
	 * @return a new {@link TriplePattern} with this subject, and the given predicate and object(s)
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynTriples"> Triple pattern syntax</a>
	 */
	default TriplePattern has(IRI predicate, RdfObject... objects) {
		return has(Rdf.iri(predicate), objects);
	}

	/**
	 * Create a triple pattern from this subject and the given predicate and object
	 * 
	 * @param predicate the predicate {@link IRI} of the triple pattern
	 * @param values    the object value(s) of the triple pattern.
	 * @return a new {@link TriplePattern} with this subject, and the given predicate and object(s)
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynTriples"> Triple pattern syntax</a>
	 */
	default TriplePattern has(IRI predicate, Value... values) {
		return has(Rdf.iri(predicate), Rdf.objects(values));
	}

	/**
	 * Create a triple pattern from this subject and the given predicate-object list(s)
	 * 
	 * @param lists the {@link RdfPredicateObjectList}(s) to describing this subject
	 * @return a new {@link TriplePattern} with this subject, and the given predicate-object list(s)
	 */
	default TriplePattern has(RdfPredicateObjectList... lists) {
		return GraphPatterns.tp(this, lists);
	}

	/**
	 * Wrapper for {@link #has(RdfPredicate, RdfObject...)} that converts String objects into RdfLiteral instances
	 * 
	 * @param predicate the predicate of the triple pattern
	 * @param objects   the String object(s) of the triple pattern
	 * @return a new {@link TriplePattern} with this subject, and the given predicate and object(s)
	 */
	default TriplePattern has(RdfPredicate predicate, String... objects) {
		return GraphPatterns.tp(this, predicate, toRdfLiteralArray(objects));
	}

	/**
	 * Wrapper for {@link #has(RdfPredicate, RdfObject...)} that converts Number objects into RdfLiteral instances
	 * 
	 * @param predicate the predicate of the triple pattern
	 * @param objects   the Number object(s) of the triple pattern
	 * @return a new {@link TriplePattern} with this subject, and the given predicate and object(s)
	 */
	default TriplePattern has(RdfPredicate predicate, Number... objects) {
		return GraphPatterns.tp(this, predicate, toRdfLiteralArray(objects));
	}

	/**
	 * Wrapper for {@link #has(RdfPredicate, RdfObject...)} that converts Boolean objects into RdfLiteral instances
	 * 
	 * @param predicate the predicate of the triple pattern
	 * @param objects   the Boolean object(s) of the triple pattern
	 * @return a new {@link TriplePattern} with this subject, and the given predicate and object(s)
	 */
	default TriplePattern has(RdfPredicate predicate, Boolean... objects) {
		return GraphPatterns.tp(this, predicate, toRdfLiteralArray(objects));
	}

	/**
	 * Use the built-in shortcut "a" for <code>rdf:type</code> to build a triple with this subject and the given objects
	 * 
	 * @param objects the objects to use to describe the <code>rdf:type</code> of this subject
	 * @return a {@link TriplePattern} object with this subject, the "a" shortcut predicate, and the given objects
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#abbrevRdfType"> RDF Type abbreviation</a>
	 */
	default TriplePattern isA(RdfObject... objects) {
		return has(RdfPredicate.a, objects);
	}
}