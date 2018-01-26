package org.eclipse.rdf4j.spanqit.core;

import org.eclipse.rdf4j.spanqit.rdf.Iri;

/**
 * A SPARQL Dataset specifier.
 * 
 * @see <a
 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#specifyingDataset">
 *      Specifying RDF Datasets</a>
 */
public class From implements QueryElement {
	private static final String FROM = "FROM";
	private static final String NAMED = "NAMED";
	private Iri iri;
	private boolean isNamed;

	From(Iri iri) {
		this(iri, false);
	}

	From(Iri iri, boolean isNamed) {
		this.iri = iri;
		this.isNamed = isNamed;
	}

	@Override
	public String getQueryString() {
		StringBuilder fromClause = new StringBuilder();

		fromClause.append(FROM).append(" ");
		if (isNamed) {
			fromClause.append(NAMED).append(" ");
		}
		fromClause.append(iri.getQueryString());

		return fromClause.toString();
	}
}