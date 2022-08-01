/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.graphpattern;

import java.util.function.Consumer;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sparqlbuilder.constraint.propertypath.builder.EmptyPropertyPathBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Projectable;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfBlankNode.PropertiesBlankNode;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfObject;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicateObjectList;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfSubject;

/**
 * A class with static methods to create graph patterns.
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#GraphPattern">SPARQL Graph Pattern</a>
 */
public class GraphPatterns {
	// prevent instantiation of this class
	private GraphPatterns() {
	}

	/**
	 * Create a triple pattern with the given subject, predicate, and object(s)
	 *
	 * @param subject
	 * @param predicate
	 * @param objects
	 *
	 * @return a new {@link TriplePattern}
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynTriples"> Triple pattern syntax</a>
	 */
	public static TriplePattern tp(RdfSubject subject, RdfPredicate predicate, RdfObject... objects) {
		return new TriplesSameSubject(subject, predicate, objects);
	}

	public static TriplePattern tp(RdfSubject subject, RdfPredicate predicate, Value... objects) {
		return tp(subject, predicate, Rdf.objects(objects));
	}

	/**
	 * Create a triple pattern with the given subject, predicate, and object(s)
	 *
	 * @param subject   the triple pattern subject
	 * @param predicate the triple pattern predicate as a {@link IRI}
	 * @param objects   the triples pattern object(s)
	 *
	 * @return a new {@link TriplePattern}
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynTriples"> Triple pattern syntax</a>
	 */
	public static TriplePattern tp(RdfSubject subject, IRI predicate, RdfObject... objects) {
		return tp(subject, Rdf.iri(predicate), objects);
	}

	public static TriplePattern tp(RdfSubject subject, IRI predicate, Value... objects) {
		return tp(subject, Rdf.iri(predicate), Rdf.objects(objects));
	}

	/**
	 * Create a triple pattern with the given subject, predicate, and object(s)
	 *
	 * @param subject   the triple pattern subject
	 * @param predicate the triple pattern predicate as a {@link IRI}
	 * @param objects   the triples pattern object(s)
	 *
	 * @return a new {@link TriplePattern}
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynTriples"> Triple pattern syntax</a>
	 */
	public static TriplePattern tp(Resource subject, RdfPredicate predicate, RdfObject... objects) {
		if (subject instanceof IRI) {
			return tp(Rdf.iri((IRI) subject), predicate, objects);
		}
		return tp(Rdf.bNode(((BNode) subject).getID()), predicate, objects);
	}

	public static TriplePattern tp(Resource subject, RdfPredicate predicate, Value... objects) {
		return tp(subject, predicate, Rdf.objects(objects));
	}

	/**
	 * Create a triple pattern with the given subject, predicate, and object(s)
	 *
	 * @param subject   the triple pattern subject as a {@link Resource}
	 * @param predicate the triple pattern predicate as a {@link IRI}
	 * @param objects   the triples pattern object(s)
	 *
	 * @return a new {@link TriplePattern}
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynTriples"> Triple pattern syntax</a>
	 */
	public static TriplePattern tp(Resource subject, IRI predicate, RdfObject... objects) {
		return tp(subject, Rdf.iri(predicate), objects);
	}

	public static TriplePattern tp(Resource subject, IRI predicate, Value... objects) {
		return tp(subject, Rdf.iri(predicate), Rdf.objects(objects));
	}

	/**
	 * Create a triple pattern with the given subject and predicate-object list(s)
	 *
	 * @param subject
	 * @param lists
	 *
	 * @return a new {@link TriplePattern}
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynTriples"> Triple pattern syntax</a>
	 */
	public static TriplePattern tp(RdfSubject subject, RdfPredicateObjectList... lists) {
		return new TriplesSameSubject(subject, lists);
	}

	/**
	 * Create a triple pattern from a property-list blank node
	 *
	 * @param bnode the PropertiesBlankNode instance to convert to a triple pattern
	 *
	 * @return the triple pattern represented by the expansion of this blank node
	 *
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynBlankNodes"> blank node syntax</a>
	 */
	public static TriplePattern tp(PropertiesBlankNode bnode) {
		return new BNodeTriplePattern(bnode);
	}

	/**
	 * Create a triple pattern from a property path and a list of objects.
	 *
	 * @param subject                the subject
	 * @param propertyPathConfigurer an object that accepts an {@link EmptyPropertyPathBuilder} and uses it to create a
	 *                               property path
	 * @param objects                the object(s) of the triple(s)
	 *
	 * @return the triple pattern
	 */
	public static TriplePattern tp(RdfSubject subject, Consumer<EmptyPropertyPathBuilder> propertyPathConfigurer,
			RdfObject... objects) {
		EmptyPropertyPathBuilder builder = new EmptyPropertyPathBuilder();
		propertyPathConfigurer.accept(builder);
		return new TriplesSameSubject(subject, builder.build(), objects);
	}

	/**
	 * Create a group graph pattern containing the given graph patterns
	 *
	 * @param patterns the patterns to include in the group graph a pattern
	 * @return a new group graph pattern
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#GroupPatterns">SPARQL Group Graph
	 *      Pattern</a>
	 */
	public static GraphPatternNotTriples and(GraphPattern... patterns) {
		GroupGraphPattern and = new GroupGraphPattern();

		return new GraphPatternNotTriples(and.and(patterns));
	}

	/**
	 * Create an alternative graph pattern containing the union of the given graph patterns: <br>
	 *
	 * <pre>
	 * { { pattern1 } UNION { pattern2 } UNION ... UNION { patternN } }
	 * </pre>
	 *
	 * @param patterns the patterns to include in the union
	 * @return a new alternative graph pattern
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#alternatives"> SPARQL Alternative Graph
	 *      Patterns</a>
	 */
	public static GraphPatternNotTriples union(GraphPattern... patterns) {
		AlternativeGraphPattern union = new AlternativeGraphPattern();

		return new GraphPatternNotTriples(union.union(patterns));
	}

	/**
	 * Create an optional group graph pattern containing the given graph patterns: <br>
	 *
	 * <pre>
	 * {
	 *   OPTIONAL {
	 *     pattern1 .
	 *     pattern2 .
	 *     ...
	 *     patternN
	 *   }
	 * }
	 * </pre>
	 *
	 * @param patterns the patterns to include in the optional graph pattern
	 * @return a new optional graph pattern
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#optionals"> SPARQL Optional Graph
	 *      Patterns</a>
	 */
	public static GraphPatternNotTriples optional(GraphPattern... patterns) {
		return and(patterns).optional();
	}

	public static GraphPatternNotTriples filterExists(GraphPattern... patterns) {
		return filterExists(true, patterns);
	}

	public static GraphPatternNotTriples filterNotExists(GraphPattern... patterns) {
		return filterExists(false, patterns);
	}

	public static GraphPatternNotTriples minus(GraphPattern... patterns) {
		MinusGraphPattern minus = new MinusGraphPattern();
		minus.and(patterns);

		return new GraphPatternNotTriples(minus);
	}

	public static GraphPatternNotTriples filterExists(boolean exists, GraphPattern... patterns) {
		FilterExistsGraphPattern filterExists = new FilterExistsGraphPattern().exists(exists);
		filterExists.and(patterns);

		return new GraphPatternNotTriples(filterExists);
	}

	/**
	 * Create a SPARQL subquery, including the given elements in its projection.
	 *
	 * @param projectables the elements to include in the projection of the subquery
	 * @return a new subquery
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#subqueries"> SPARQL Subquery</a>
	 */
	public static SubSelect select(Projectable... projectables) {
		return new SubSelect().select(projectables);
	}

	static GroupGraphPattern extractOrConvertToGGP(GraphPattern pattern) {
		if (pattern instanceof GroupGraphPattern) {
			return (GroupGraphPattern) pattern;
		}

		if (pattern instanceof GraphPatternNotTriples) {
			GraphPatternNotTriples gp = (GraphPatternNotTriples) pattern;
			if (gp.gp instanceof GroupGraphPattern) {
				return (GroupGraphPattern) gp.gp;
			}
		}

		return new GroupGraphPattern(pattern);
	}
}
