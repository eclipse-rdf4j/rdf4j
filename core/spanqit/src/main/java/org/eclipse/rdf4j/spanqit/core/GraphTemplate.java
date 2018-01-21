package org.eclipse.rdf4j.spanqit.core;

import org.eclipse.rdf4j.spanqit.graphpattern.TriplePattern;

/**
 * A SPARQL Graph Template, used in Construct queries
 * 
 * @see <a
 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">
 *      SPARQL CONSTRUCT Query</a>
 */
public class GraphTemplate implements QueryElement {
	private static final String CONSTRUCT = "CONSTRUCT";
	private TriplesTemplate triplesTemplate = Spanqit.triplesTemplate();
	
	GraphTemplate() { }

	/**
	 * Add triple patterns to this graph template
	 * 
	 * @param triples
	 *            the patterns to add
	 * @return this
	 */
	public GraphTemplate construct(TriplePattern... triples) {
		triplesTemplate.and(triples);
		
		return this;
	}
	
	/**
	 * Set, rather than augment, this graph template's triples template 
	 * @param triplesTemplate
	 * 		the {@link TriplesTemplate} instance to set
	 * @return this graph template
	 */
	public GraphTemplate construct(TriplesTemplate triplesTemplate) {
		this.triplesTemplate = triplesTemplate;
		
		return this;
	}
	
	@Override
	public String getQueryString() {
		StringBuilder graphTemplate = new StringBuilder();
		
		graphTemplate.append(CONSTRUCT).append(" ").append(triplesTemplate.getQueryString());
		
		return graphTemplate.toString();
	}
}