package org.eclipse.rdf4j.spanqit.rdf;

import static org.eclipse.rdf4j.spanqit.rdf.Rdf.toRdfLiteralArray;

import org.eclipse.rdf4j.spanqit.core.QueryElement;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.spanqit.graphpattern.TriplePattern;

/**
 * Denotes an element that can represent a subject in a
 * {@link TriplePattern}
 */
public interface RdfSubject extends QueryElement {
	/**
	 * Create a triple pattern from this subject and the given predicate and
	 * object
	 * 
	 * @param predicate
	 *            the predicate of the triple pattern
	 * @param objects
	 *            the object(s) of the triple pattern
	 * @return a new {@link TriplePattern} with this subject, and the given predicate and object(s)
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynTriples">
	 *      Triple pattern syntax</a>
	 */
	default TriplePattern has(RdfPredicate predicate, RdfObject... objects) {
		return GraphPatterns.tp(this, predicate, objects);
	}
	
	/**
	 * Create a triple pattern from this subject and the given predicate-object list(s) 
	 * @param lists
	 * 		the {@link RdfPredicateObjectList}(s) to describing this subject
	 * @return a new {@link TriplePattern} with this subject, and the given predicate-object list(s)
	 */
	default TriplePattern has(RdfPredicateObjectList... lists) {
		return GraphPatterns.tp(this, lists);
	}
	
	/**
	 * Wrapper for {@link #has(RdfPredicate, RdfObject...)} that converts String objects into RdfLiteral instances
	 * 
	 * @param predicate
	 * 			the predicate of the triple pattern
	 * @param objects
	 * 			the String object(s) of the triple pattern
	 * @return a new {@link TriplePattern} with this subject, and the given predicate and object(s)
	 */
	default TriplePattern has(RdfPredicate predicate, String... objects) {
		return GraphPatterns.tp(this, predicate, toRdfLiteralArray(objects));
	}
	
	/**
	 * Wrapper for {@link #has(RdfPredicate, RdfObject...)} that converts Number objects into RdfLiteral instances
	 * 
	 * @param predicate
	 * 			the predicate of the triple pattern
	 * @param objects
	 * 			the Number object(s) of the triple pattern
	 * @return a new {@link TriplePattern} with this subject, and the given predicate and object(s)
	 */
	default TriplePattern has(RdfPredicate predicate, Number... objects) {
		return GraphPatterns.tp(this, predicate, toRdfLiteralArray(objects));
	}
	
	/**
	 * Wrapper for {@link #has(RdfPredicate, RdfObject...)} that converts Boolean objects into RdfLiteral instances
	 * 
	 * @param predicate
	 * 			the predicate of the triple pattern
	 * @param objects
	 * 			the Boolean object(s) of the triple pattern
	 * @return a new {@link TriplePattern} with this subject, and the given predicate and object(s)
	 */
	default TriplePattern has(RdfPredicate predicate, Boolean... objects) {
		return GraphPatterns.tp(this, predicate, toRdfLiteralArray(objects));
	}
	
	/**
	 * Use the built-in shortcut "a" for <code>rdf:type</code> to build a triple with this subject and the given objects
	 * @param objects the objects to use to describe the <code>rdf:type</code> of this subject
	 * 
	 * @return a {@link TriplePattern} object with this subject, the "a" shortcut predicate, and the given objects 
	 * 
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#abbrevRdfType">
	 * 		RDF Type abbreviation</a>
	 */
	default TriplePattern isA(RdfObject... objects) {
		return has(RdfPredicate.a, objects);
	}
}