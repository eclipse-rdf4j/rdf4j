/*******************************************************************************
Copyright (c) 2018 Eclipse RDF4J contributors.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Distribution License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/org/documents/edl-v10.php.
*******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.graphpattern;

import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfObject;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicateObjectList;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfSubject;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfBlankNode.PropertiesBlankNode;

/**
 * A class with static methods to create graph patterns.
 * 
 * @see <a
 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#GraphPattern">SPARQL
 *      Graph Pattern</a>
 */
public class GraphPatterns {
	// prevent instantiation of this class
	private GraphPatterns() { }

	/**
	 * Create a triple pattern with the given subject, predicate, and object(s)
	 * 
	 * @param subject
	 * @param predicate
	 * @param objects
	 * 
	 * @return a new {@link TriplePattern}
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynTriples">
	 *      Triple pattern syntax</a>
	 */
	public static TriplePattern tp(RdfSubject subject, RdfPredicate predicate, RdfObject... objects) {
		return new TriplesSameSubject(subject, predicate, objects);
	}
	
	/**
	 * Create a triple pattern with the given subject and predicate-object list(s)
	 * 
	 * @param subject
	 * @param lists
	 * 
	 * @return a new {@link TriplePattern}
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynTriples">
	 *      Triple pattern syntax</a>
	 */
	public static TriplePattern tp(RdfSubject subject, RdfPredicateObjectList... lists) {
		return new TriplesSameSubject(subject, lists);
	}
	/**
	 * Create a triple pattern from a property-list blank node
	 * @param bnode the PropertiesBlankNode instance to convert to a triple pattern
	 * 
	 * @return the triple pattern represented by the expansion of this blank node
	 * 
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynBlankNodes">
	 * 		blank node syntax</a>
	 */
	public static TriplePattern tp(PropertiesBlankNode bnode) {
		return new BNodeTriplePattern(bnode);
	}

	/**
	 * Create a group graph pattern containing the given graph patterns
	 * 
	 * @param patterns
	 *            the patterns to include in the group graph a pattern
	 * @return a new group graph pattern
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#GroupPatterns">SPARQL
	 *      Group Graph Pattern</a>
	 */
	public static GraphPatternNotTriple and(GraphPattern... patterns) {
		return new GraphPatternNotTriple().and(patterns);
	}

	/**
	 * Create an alternative graph pattern containing the union of the given
	 * graph patterns: <br>
	 * 
	 * <pre>
	 * { { pattern1 } UNION { pattern2 } UNION ... UNION { patternN } }
	 * </pre>
	 * 
	 * @param patterns
	 *            the patterns to include in the union
	 * @return a new alternative graph pattern
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#alternatives">
	 *      SPARQL Alternative Graph Patterns</a>
	 */
	public static GraphPatternNotTriple union(GraphPattern... patterns) {
		return new GraphPatternNotTriple().union(patterns);
	}

	/**
	 * Create an optional group graph pattern containing the given graph
	 * patterns: <br>
	 * 
	 * <pre>
	 * {
	 *   OPTIONAL {
	 *     pattern1 .
	 *     pattern2 .
	 *     ... .
	 *     patternN
	 *   }
	 * }
	 * </pre>
	 * 
	 * @param patterns
	 *            the patterns to include in the optional graph pattern
	 * @return a new optional graph pattern
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#optionals">
	 *      SPARQL Optional Graph Patterns</a>
	 */
	public static GraphPatternNotTriple optional(GraphPattern... patterns) {
		return and(patterns).optional();
	}

	/**
	 * Create a SPARQL subquery, including the given elements in its projection.
	 * 
	 * @param projectables
	 *            the elements to include in the projection of the subquery
	 * @return a new subquery
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#subqueries">
	 *      SPARQL Subquery</a>
	 */
	public static SubSelect select(Projectable... projectables) {
		return new SubSelect().select(projectables);
	}
}