package org.eclipse.rdf4j.spanqit.rdf;

import org.eclipse.rdf4j.spanqit.core.StandardQueryElementCollection;

/**
 * A Predicate-Object List
 * 
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#predObjLists">
 * 		SPARQL Predicate-Object List</a>
 */
public class RdfPredicateObjectList extends StandardQueryElementCollection<RdfObject> {
	RdfPredicateObjectList(RdfPredicate predicate, RdfObject... objects) {
		super(predicate.getQueryString(), ", ");
		printNameIfEmpty(false);
		and(objects);
	}
	
	/**
	 * Add {@link RdfObject} instances to this predicate-object list
	 * 
	 * @param objects the objects to add to this list
	 * 
	 * @return this {@link RdfPredicateObjectList} instance
	 */
	public RdfPredicateObjectList and(RdfObject... objects) {
		addElements(objects);
		
		return this;
	}
}
