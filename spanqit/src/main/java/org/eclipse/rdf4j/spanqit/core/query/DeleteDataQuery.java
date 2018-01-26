package org.eclipse.rdf4j.spanqit.core.query;

import org.eclipse.rdf4j.spanqit.core.TriplesTemplate;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphName;
import org.eclipse.rdf4j.spanqit.graphpattern.TriplePattern;

/**
 * The SPARQL Delete Data Query
 * 
 * @see <a href="https://www.w3.org/TR/sparql11-update/#deleteData"> SPARQL
 *      DELETE DATA Query</a>
 * 
 */
public class DeleteDataQuery extends UpdateDataQuery<DeleteDataQuery> {
	private static final String DELETE_DATA = "DELETE DATA";

	/**
	 * Add triples to be deleted
	 * 
	 * @param triples the triples to add to this delete data query
	 * 
	 * @return this Delete Data query instance
	 */
	public DeleteDataQuery deleteData(TriplePattern... triples) {
		return addTriples(triples);
	}
	
	/**
	 * Set this query's triples template
	 * 
	 * @param triplesTemplate
	 * 		the {@link TriplesTemplate} instance to set
	 * 
	 * @return this instance
	 */
	public DeleteDataQuery deleteData(TriplesTemplate triplesTemplate) {
		return setTriplesTemplate(triplesTemplate);
	}

	/**
	 * Specify a graph to delete the data from
	 * 
	 * @param graph the identifier of the graph
	 * 
	 * @return this Delete Data query instance
	 */
	public DeleteDataQuery from(GraphName graph) {
		return graph(graph);
	}
	
	@Override
	protected String getPrefix() {
		return DELETE_DATA;
	}
}
