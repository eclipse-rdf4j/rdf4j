/*******************************************************************************
Copyright (c) 2018 Eclipse RDF4J contributors.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Distribution License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/org/documents/edl-v10.php.
*******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.rdf;

import org.eclipse.rdf4j.sparqlbuilder.core.StandardQueryElementCollection;

/**
 * A Predicate-Object List
 * 
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#predObjLists"> SPARQL Predicate-Object List</a>
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
