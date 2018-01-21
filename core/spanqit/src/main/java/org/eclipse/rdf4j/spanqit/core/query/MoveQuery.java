package org.eclipse.rdf4j.spanqit.core.query;

/**
 * A SPARQL MOVE Query
 * 
 * @see <a href="https://www.w3.org/TR/sparql11-update/#move">
 * 		SPARQL MOVE Query</a>
 */
public class MoveQuery extends DestinationSourceManagementQuery<MoveQuery> {
	private static final String MOVE = "MOVE";
	
	MoveQuery() { }

	@Override
	protected String getQueryActionString() {
		return MOVE;
	}

}
