package org.eclipse.rdf4j.spanqit.rdf;

import org.eclipse.rdf4j.spanqit.core.QueryElementCollection;

/**
 * An RDF predicate-object list collection
 * 
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#predObjLists">
 * 		Predicate-Object Lists</a>
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#objLists">
 * 		Object Lists</a>
 */
public class RdfPredicateObjectListCollection extends QueryElementCollection<RdfPredicateObjectList> {
	private static final String DELIMITER = " ;\n    ";

	RdfPredicateObjectListCollection() {
		super(DELIMITER);
	};
	
	/**
	 * add predicate-object lists to this collection
	 * 
	 * @param predicate the predicate of the predicate-object list to add
	 * @param objects the object or objects to add
	 * 
	 * @return this instance
	 */
	public RdfPredicateObjectListCollection andHas(RdfPredicate predicate, RdfObject... objects) {
		return andHas(Rdf.predicateObjectList(predicate, objects));
	}
	
	/**
	 * add predicate-object lists to this collection
	 * 
	 * @param lists the {@link RdfPredicateObjectList}'s to add to this collection
	 * @return this instance
	 */
	public RdfPredicateObjectListCollection andHas(RdfPredicateObjectList... lists) {
		addElements(lists);
		
		return this;
	}
	
	// TODO add suffix for if elements.size > 1; here or in triplessamesubject
}
