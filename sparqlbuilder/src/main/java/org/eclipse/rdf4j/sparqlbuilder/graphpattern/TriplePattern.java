/*******************************************************************************
Copyright (c) 2018 Eclipse RDF4J contributors.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Distribution License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/org/documents/edl-v10.php.
*******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.graphpattern;

import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfObject;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicateObjectList;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.toRdfLiteralArray;

/**
 * Denotes a SPARQL Triple Pattern
 * 
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynTriples"> Triple pattern syntax</a>
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynBlankNodes"> blank node syntax</a>
 */
public interface TriplePattern extends GraphPattern {
	@SuppressWarnings("javadoc")
	String SUFFIX = " .";

	/**
	 * Add predicate-object lists describing this triple pattern's subject
	 * 
	 * @param predicate the predicate to use to describe this triple pattern's subject
	 * @param objects   the corresponding object(s)
	 * 
	 * @return this triple pattern
	 */
	default TriplePattern andHas(RdfPredicate predicate, RdfObject... objects) {
		return andHas(Rdf.predicateObjectList(predicate, objects));
	}

	/**
	 * Add predicate-object lists describing this triple pattern's subject
	 * 
	 * @param lists the {@link RdfPredicateObjectList}(s) to add
	 * 
	 * @return this triple pattern
	 */
	TriplePattern andHas(RdfPredicateObjectList... lists);

	/**
	 * Convenience version of {@link #andHas(RdfPredicate, RdfObject...)} that takes Strings and converts them to
	 * StringLiterals
	 * 
	 * @param predicate the predicate to use to describe this triple pattern's subject
	 * @param objects   the corresponding object(s)
	 * 
	 * @return this triple pattern
	 */
	default TriplePattern andHas(RdfPredicate predicate, String... objects) {
		return andHas(predicate, toRdfLiteralArray(objects));
	};

	/**
	 * Convenience version of {@link #andHas(RdfPredicate, RdfObject...)} that takes Boolean and converts them to
	 * BooleanLiterals
	 * 
	 * @param predicate the predicate to use to describe this triple pattern's subject
	 * @param objects   the corresponding object(s)
	 * 
	 * @return this triple pattern
	 */
	default TriplePattern andHas(RdfPredicate predicate, Boolean... objects) {
		return andHas(predicate, toRdfLiteralArray(objects));
	};

	/**
	 * Convenience version of {@link #andHas(RdfPredicate, RdfObject...)} that takes Numbers and converts them to
	 * NumberLiterals
	 * 
	 * @param predicate the predicate to use to describe this triple pattern's subject
	 * @param objects   the corresponding object(s)
	 * 
	 * @return this triple pattern
	 */
	default TriplePattern andHas(RdfPredicate predicate, Number... objects) {
		return andHas(predicate, toRdfLiteralArray(objects));
	};

	/**
	 * Use the built-in RDF shortcut {@code a} for {@code rdf:type} to specify the subject's type
	 * 
	 * @param object the object describing this triple pattern's subject's {@code rdf:type}
	 * 
	 * @return this triple pattern
	 * 
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#abbrevRdfType"> RDF Type abbreviation</a>
	 */
	default TriplePattern andIsA(RdfObject object) {
		return andHas(RdfPredicate.a, object);
	}

	@Override
	default boolean isEmpty() {
		return false;
	}
}
