package org.eclipse.rdf4j.spanqit.core.query;

/**
 * A SPARQL ADD Query
 * 
 * @see <a href="https://www.w3.org/TR/sparql11-update/#add">
 * 		SPARQL ADD Query</a>
 */
public class AddQuery extends DestinationSourceManagementQuery<AddQuery> {
	private static final String ADD = "ADD";
	
	AddQuery() { }

	@Override
	protected String getQueryActionString() {
		return ADD;
	}
}
