package org.eclipse.rdf4j.spanqit.core.query;

import org.eclipse.rdf4j.spanqit.rdf.Iri;

/**
 * A SPARQL CREATE Query
 * 
 * @see <a href="https://www.w3.org/TR/sparql11-update/#create">
 * 		SPARQL CREATE Query</a>
 */
public class CreateQuery extends GraphManagementQuery<CreateQuery> {
	private static final String CREATE = "CREATE";
	private static final String GRAPH = "GRAPH";
	
	private Iri graph;
	
	CreateQuery() { }
	
	/**
	 * Specify the graph to create
	 * 
	 * @param graph the IRI identifier for the new graph
	 * 
	 * @return this CreateQuery instance
	 */
	public CreateQuery graph(Iri graph) {
		this.graph = graph;
		
		return this;
	}

	@Override
	public String getQueryString() {
		StringBuilder create = new StringBuilder();
		
		create.append(CREATE).append(" ");
		
		appendSilent(create);
		
		create.append(GRAPH).append(" ").append(graph.getQueryString());
		
		return create.toString();
	}

}
