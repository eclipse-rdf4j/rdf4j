package org.eclipse.rdf4j.spanqit.rdf;

import org.eclipse.rdf4j.spanqit.core.QueryElement;
import org.eclipse.rdf4j.spanqit.graphpattern.TriplePattern;

/**
 * Denotes an element that can represent a predicate in a
 * {@link TriplePattern}
 */
public interface RdfPredicate extends QueryElement {
	/**
	 * The built-in predicate shortcut for <code>rdf:type</code>
	 * 
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#abbrevRdfType">
	 * 		RDF Type abbreviation</a>
	 */
	public static RdfPredicate a = () -> "a";
}