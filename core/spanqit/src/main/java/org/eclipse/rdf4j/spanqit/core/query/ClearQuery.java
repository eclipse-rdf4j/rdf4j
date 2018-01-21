package org.eclipse.rdf4j.spanqit.core.query;

/**
 * A SPARQL CLEAR Query
 * 
 * @see <a href="https://www.w3.org/TR/sparql11-update/#clear">
 * 		SPARQL CLEAR Query</a>
 * 		
 */
public class ClearQuery extends TargetedGraphManagementQuery<ClearQuery> {
	private static final String CLEAR = "CLEAR";

	ClearQuery() { }

	@Override
	protected String getQueryActionString() {
		return CLEAR;
	}
}
