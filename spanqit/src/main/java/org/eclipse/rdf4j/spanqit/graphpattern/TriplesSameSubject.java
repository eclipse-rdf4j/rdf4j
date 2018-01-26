package org.eclipse.rdf4j.spanqit.graphpattern;

import org.eclipse.rdf4j.spanqit.rdf.Rdf;
import org.eclipse.rdf4j.spanqit.rdf.RdfObject;
import org.eclipse.rdf4j.spanqit.rdf.RdfPredicate;
import org.eclipse.rdf4j.spanqit.rdf.RdfPredicateObjectList;
import org.eclipse.rdf4j.spanqit.rdf.RdfPredicateObjectListCollection;
import org.eclipse.rdf4j.spanqit.rdf.RdfSubject;

/**
 * A SPARQL Triple Pattern.
 * 
 * @see <a
 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynTriples">
 *      Triple pattern syntax</a>
 */
class TriplesSameSubject implements TriplePattern {
	private RdfSubject subject;
	private RdfPredicateObjectListCollection predicateObjectLists = Rdf.predicateObjectListCollection();

	TriplesSameSubject(RdfSubject subject, RdfPredicate predicate, RdfObject... objects) {
		this.subject = subject;
		andHas(predicate, objects);
	}
	
	TriplesSameSubject(RdfSubject subject, RdfPredicateObjectList... lists) {
		this.subject = subject;
		andHas(lists);
	}
	
	@Override
	public TriplesSameSubject andHas(RdfPredicateObjectList... lists) {
		predicateObjectLists.andHas(lists);
		
		return this;
	}

	@Override
	public String getQueryString() {
		return subject.getQueryString() + " " + predicateObjectLists.getQueryString() + SUFFIX;
	}
}