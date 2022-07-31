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

import org.eclipse.rdf4j.sparqlbuilder.constraint.Expression;
import org.eclipse.rdf4j.sparqlbuilder.core.QueryElement;

/**
 * Denotes a SPARQL Graph Pattern
 *
 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#GraphPattern"> SPARQL Graph Patterns</a>
 */
public interface GraphPattern extends QueryElement {
	/**
	 * Convert this graph pattern into a group graph pattern, combining this graph pattern with the given patterns: <br>
	 *
	 * <pre>
	 * {
	 *   thisPattern .
	 *   pattern1 .
	 *   pattern2 .
	 *   ...
	 *   patternN
	 * }
	 * </pre>
	 *
	 * @param patterns the patterns to add
	 * @return the new {@code GraphPattern} instance
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#GroupPatterns">SPARQL Group Graph
	 *      Pattern</a>
	 */
	default GraphPattern and(GraphPattern... patterns) {
		return GraphPatterns.and(this).and(patterns);
	}

	/**
	 * Convert this graph pattern into an alternative graph pattern, combining this graph pattern with the given
	 * patterns: <br>
	 *
	 * <pre>
	 * {
	 *   { thisPattern } UNION
	 *   { pattern1 } UNION
	 *   { pattern2 } UNION
	 *   ...
	 *   { patternN }
	 * }
	 * </pre>
	 *
	 * @param patterns the patterns to add
	 * @return the new {@code GraphPattern} instance
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#alternatives">SPARQL Alternative Graph
	 *      Pattern</a>
	 */
	default GraphPattern union(GraphPattern... patterns) {
		return GraphPatterns.union(this).union(patterns);
	}

	/**
	 * Convert this graph pattern into an optional group graph pattern: <br>
	 *
	 * <pre>
	 * OPTIONAL {thisPattern}
	 * </pre>
	 *
	 * @return the new {@code GraphPattern} instance
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#optionals"> SPARQL Optional Graph
	 *      Patterns</a>
	 */
	default GraphPattern optional() {
		return optional(true);
	}

	/**
	 * Specify if this graph pattern should be optional.
	 *
	 * <p>
	 * NOTE: This converts this graph pattern into a group graph pattern.
	 *
	 * @param isOptional if this graph pattern should be optional or not
	 * @return the new {@code GraphPattern} instance
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#optionals"> SPARQL Optional Graph
	 *      Patterns</a>
	 */
	default GraphPattern optional(boolean isOptional) {
		return and().optional(isOptional);
	}

	/**
	 * Convert this graph pattern into a group graph pattern and add a filter: <br>
	 *
	 * <pre>
	 * {
	 *   thisPattern
	 *   FILTER { constraint }
	 * }
	 * </pre>
	 *
	 * @param constraint the filter constraint
	 * @return the new {@code GraphPattern} instance
	 *
	 * @see <a href="http://www.w3.org/TR/sparql11-query/#termConstraint"> SPARQL Filter</a>
	 */
	default GraphPattern filter(Expression<?> constraint) {
		return and().filter(constraint);
	}

	/**
	 * Create an <code>EXISTS{}</code> filter expression with the given graph patterns and add it to this graph pattern
	 * (converting this to a group graph pattern in the process): <br>
	 *
	 * <pre>
	 * {
	 * 	thisPattern
	 * 	FILTER EXISTS { patterns }
	 * }
	 * </pre>
	 *
	 * @param patterns the patterns to pass as arguments to the <code>EXISTS</code> expression
	 *
	 * @return the new {@code GraphPattern} instance
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#neg-pattern"> Filtering using Graph
	 *      Pattern</a>
	 */
	default GraphPattern filterExists(GraphPattern... patterns) {
		return filterExists(true, patterns);
	}

	/**
	 * Create a <code>NOT EXISTS{}</code> filter expression with the given graph patterns and add it to this graph
	 * pattern (converting this to a group graph pattern in the process): <br>
	 *
	 * <pre>
	 * {
	 * 	thisPattern
	 * 	FILTER NOT EXISTS { patterns }
	 * }
	 * </pre>
	 *
	 * @param patterns the patterns to pass as arguments to the <code>NOT EXISTS</code> expression
	 *
	 * @return the new {@code GraphPattern} instance
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#neg-pattern"> Filtering using Graph
	 *      Pattern</a>
	 */
	default GraphPattern filterNotExists(GraphPattern... patterns) {
		return filterExists(false, patterns);
	}

	/**
	 * Create an {@code EXISTS} or {@code NOT EXISTS} filter expression with the given patterns based on the
	 * {@code exists} paramater and add it to this graph pattern (converting this to a group graph pattern in the
	 * process)
	 *
	 * @param exists   if the filter should ensure the patterns exist or not
	 * @param patterns the patterns to pass to the filter
	 * @return the new {@code GraphPattern} instance
	 */
	default GraphPattern filterExists(boolean exists, GraphPattern... patterns) {
		return and(GraphPatterns.filterExists(exists, patterns));
	}

	/**
	 * Create a <code>MINUS</code> graph pattern with the given graph patterns and add it to this graph pattern
	 * (converting this to a group graph pattern in the process): <br>
	 *
	 * <pre>
	 * {
	 * 	thisPattern
	 * 	MINUS { patterns }
	 * }
	 * </pre>
	 *
	 * @param patterns the patterns to construct the <code>MINUS</code> graph pattern with
	 * @return the new {@code GraphPattern} instance
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#neg-minus"> SPARQL MINUS Graph Pattern</a>
	 */
	default GraphPattern minus(GraphPattern... patterns) {
		return and(GraphPatterns.minus(patterns));
	}

	/**
	 * Convert this graph pattern into a named group graph pattern: <br>
	 *
	 * <pre>
	 * GRAPH graphName { thisPattern }
	 * </pre>
	 *
	 * @param name the name to specify
	 * @return the new {@code GraphPattern} instance
	 *
	 * @see <a href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#queryDataset"> Specifying Datasets in SPARQL
	 *      Queries</a>
	 */
	default GraphPattern from(GraphName name) {
		return and().from(name);
	}

	/**
	 * @return if this pattern is a collection of GraphPatterns (ie., Group or Alternative patterns), returns if the
	 *         collection contains any patterns
	 */
	default boolean isEmpty() {
		return true;
	}
}
