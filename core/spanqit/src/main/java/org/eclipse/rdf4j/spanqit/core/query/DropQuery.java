package org.eclipse.rdf4j.spanqit.core.query;

/** 
 * A SPARQL DROP Query
 * 
 * @see <a href="https://www.w3.org/TR/sparql11-update/#drop">
 * 		SPARQL DROP Query</a>
 */
public class DropQuery extends TargetedGraphManagementQuery<DropQuery> {
	private static final String DROP = "DROP";
	
	DropQuery() { }
	
	@Override
	protected String getQueryActionString() {
		return DROP;
	}
}
