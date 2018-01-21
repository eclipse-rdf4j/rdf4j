package org.eclipse.rdf4j.spanqit.core;

import org.eclipse.rdf4j.spanqit.rdf.Iri;

/**
 * A SPARQL Base declaration
 * 
 * @see <a
 *      	href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#relIRIs">
 *      	SPARQL Relative IRIs</a>
 */
public class Base implements QueryElement {
	private static final String BASE = "BASE ";

	private Iri iri;

	Base(Iri iri) {
		this.iri = iri;
	}

	@Override
	public String getQueryString() {
		return BASE + iri.getQueryString();
	}
}