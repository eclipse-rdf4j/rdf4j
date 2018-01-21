package org.eclipse.rdf4j.spanqit.core.query;

import org.eclipse.rdf4j.spanqit.core.GraphTemplate;
import org.eclipse.rdf4j.spanqit.core.Spanqit;
import org.eclipse.rdf4j.spanqit.graphpattern.TriplePattern;

/**
 * The SPARQL CONSTRUCT query
 * 
 * @see <a
 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#construct">
 *      SPARQL CONSTRUCT Query</a>
 */
public class ConstructQuery extends OuterQuery<ConstructQuery> {
	// package-protect instantiation of this class
	ConstructQuery() { }

	private GraphTemplate construct = Spanqit.construct();

	/**
	 * Add triples to this query's graph template
	 * 
	 * @param patterns
	 *            the triples to include in the graph template
	 * @return this
	 */
	public ConstructQuery construct(TriplePattern... patterns) {
		construct.construct(patterns);

		return this;
	}

	/**
	 * Set this query's graph template
	 * 
	 * @param construct
	 *            the {@link GraphTemplate} to set
	 * @return this
	 */
	public ConstructQuery construct(GraphTemplate construct) {
		this.construct = construct;

		return this;
	}

	@Override
	protected String getQueryActionString() {
		return construct.getQueryString();
	}
}