package org.eclipse.rdf4j.spanqit.graphpattern;

import org.eclipse.rdf4j.spanqit.core.QueryElement;

/**
 * Denotes a SPARQL Graph Pattern
 * 
 * @see <a
 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#GraphPattern">
 *      SPARQL Graph Patterns</a>
 */
public interface GraphPattern extends QueryElement {
	/**
	 * @return if this pattern is a collection of GraphPatterns (ie., Group or
	 *         Alternative patterns), returns if the collection contains any
	 *         patterns
	 */
	default boolean isEmpty() { return true; }
}