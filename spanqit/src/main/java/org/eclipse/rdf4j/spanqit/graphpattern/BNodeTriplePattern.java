package org.eclipse.rdf4j.spanqit.graphpattern;

import org.eclipse.rdf4j.spanqit.rdf.RdfBlankNode.PropertiesBlankNode;
import org.eclipse.rdf4j.spanqit.rdf.RdfPredicateObjectList;

/**
 * A triple pattern formed by a property-list blank node
 * 
 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynBlankNodes">
 * 		blank node syntax</a>
 */
class BNodeTriplePattern implements TriplePattern {
	private PropertiesBlankNode bnode;

	BNodeTriplePattern(PropertiesBlankNode subject) {
		this.bnode = subject;
	}

	@Override
	public BNodeTriplePattern andHas(RdfPredicateObjectList... lists) {
		bnode.andHas(lists);

		return this;
	}

	@Override
	public String getQueryString() {
		return bnode.getQueryString() + SUFFIX;
	}
}
