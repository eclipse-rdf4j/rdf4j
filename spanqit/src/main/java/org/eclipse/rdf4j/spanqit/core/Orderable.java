package org.eclipse.rdf4j.spanqit.core;

/**
 * Denotes an orederable SPARQL query element (can be used in a
 * <code>ORDER BY</code> clause)
 * 
 * @see <a
 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy">
 *      SPARQL Order By Clause</a>
 */
public interface Orderable extends QueryElement {
	/**
	 * @return an ascending {@link OrderCondition} instance for this {@link Orderable} object
	 */
	default public OrderCondition asc() {
		return Spanqit.asc(this);
	}
	
	/**
	 * @return an descending {@link OrderCondition} instance for this {@link Orderable} object
	 */
	default public OrderCondition desc() {
		return Spanqit.desc(this);
	}
}