package org.eclipse.rdf4j.spanqit.core.query;

/**
 * A SPARQL COPY Query
 * 
 * @see <a href="https://www.w3.org/TR/sparql11-update/#copy">
 * 		SPARQL COPY query</a>
 */
public class CopyQuery extends DestinationSourceManagementQuery<CopyQuery> {	
	private static String COPY = "COPY";

	CopyQuery() { }
	
	protected String getQueryActionString() {
		return COPY;
	}
}
