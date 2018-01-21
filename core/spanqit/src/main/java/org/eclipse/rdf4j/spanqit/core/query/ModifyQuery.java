package org.eclipse.rdf4j.spanqit.core.query;

import java.util.Optional;

import org.eclipse.rdf4j.spanqit.core.QueryPattern;
import org.eclipse.rdf4j.spanqit.core.Spanqit;
import org.eclipse.rdf4j.spanqit.core.TriplesTemplate;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphName;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphPattern;
import org.eclipse.rdf4j.spanqit.graphpattern.TriplePattern;
import org.eclipse.rdf4j.spanqit.rdf.Iri;
import org.eclipse.rdf4j.spanqit.util.SpanqitUtils;

/**
 * The SPARQL Modify Queries
 * 
 * @see <a href="https://www.w3.org/TR/sparql11-update/#deleteInsert">
 * 		SPARQL DELETE/INSERT Query</a>
 */
public class ModifyQuery extends UpdateQuery<ModifyQuery> {
	private static final String INSERT = "INSERT";
	private static final String DELETE = "DELETE";
	private static final String WITH = "WITH";
	private static final String USING = "USING";
	private static final String NAMED = "NAMED";
	
	private Optional<Iri> with = Optional.empty();
	private Optional<Iri> using = Optional.empty();
	private boolean usingNamed = false;

	private Optional<TriplesTemplate> deleteTriples = Optional.empty();
	private Optional<TriplesTemplate> insertTriples = Optional.empty();
	private Optional<GraphName> deleteGraph = Optional.empty();
	private Optional<GraphName> insertGraph = Optional.empty();

	private QueryPattern where = Spanqit.where();

	ModifyQuery() { }
	
	/**
	 * Define the graph that will be modified or matched against in the absence of more explicit graph definitions
	 * 
	 * @param iri the IRI identifying the desired graph
	 * 
	 * @return this modify query instance
	 */
	public ModifyQuery with(Iri iri) {
		with = Optional.ofNullable(iri);
		
		return this;
	}
	
	/** 
	 * Specify triples to delete (or leave empty for DELETE WHERE shortcut)
	 * 
	 * @param triples the triples to delete
	 * 
	 * @return this modify query instance
	 * 
	 * @see <a href="https://www.w3.org/TR/sparql11-update/#deleteWhere">
	 * 			SPARQL DELETE WHERE shortcut</a>
	 */
	public ModifyQuery delete(TriplePattern... triples) {
		deleteTriples = SpanqitUtils.getOrCreateAndModifyOptional(deleteTriples, Spanqit::triplesTemplate, tt -> tt.and(triples));
		
		return this;
	}
	
	/**
	 * Specify the graph to delete triples from
	 * 
	 * @param graphName the identifier of the graph
	 * 
	 * @return this modify query instance
	 */
	public ModifyQuery from(GraphName graphName) {
		this.deleteGraph = Optional.ofNullable(graphName);
		
		return this;
	}
	
	/** 
	 * Specify triples to insert
	 * 
	 * @param triples the triples to insert
	 * 
	 * @return this modify query instance
	 */
	public ModifyQuery insert(TriplePattern... triples) {
		insertTriples = SpanqitUtils.getOrCreateAndModifyOptional(insertTriples, Spanqit::triplesTemplate, tt -> tt.and(triples));
		
		return this;
	}
	
	/**
	 * Specify the graph to insert triples into
	 * 
	 * @param graphName the identifier of the graph
	 * 
	 * @return this modify query instance
	 */
	public ModifyQuery into(GraphName graphName) {
		insertGraph = Optional.ofNullable(graphName);
		
		return this;
	}

	/**
	 * Specify the graph used when evaluating the WHERE clause
	 * 
	 * @param iri the IRI identifying the desired graph
	 * 
	 * @return this modify query instance
	 */
	public ModifyQuery using(Iri iri) {
		using = Optional.ofNullable(iri);
		
		return this;
	}
	
	/**
	 * Specify a named graph to use to when evaluating the WHERE clause
	 * 
	 * @param iri the IRI identifying the desired graph
	 * 
	 * @return this modify query instance
	 */
	public ModifyQuery usingNamed(Iri iri) {
		usingNamed = true;
		
		return using(iri);
	}
	
	/**
	 * Add graph patterns to this query's query pattern
	 * 
	 * @param patterns the patterns to add
	 * 
	 * @return this modify query instance
	 */
	public ModifyQuery where(GraphPattern... patterns) {
		where.where(patterns);
		
		return this;
	}
	
	@Override
	protected String getQueryActionString() {
		StringBuilder modifyQuery = new StringBuilder();
		
		with.ifPresent(withIri -> modifyQuery.append(WITH).append(" ").append(withIri.getQueryString()).append("\n"));
		
		deleteTriples.ifPresent(delTriples -> {
				modifyQuery.append(DELETE).append(" ");
				
				// DELETE WHERE shortcut
				// https://www.w3.org/TR/sparql11-update/#deleteWhere
				if(!delTriples.isEmpty()) {
					appendNamedTriplesTemplates(modifyQuery, deleteGraph, delTriples);
				}
				modifyQuery.append("\n");
			});
		
		insertTriples.ifPresent(insTriples -> {
				modifyQuery.append(INSERT).append(" ");
				appendNamedTriplesTemplates(modifyQuery, insertGraph, insTriples);
				modifyQuery.append("\n");
			});
		
		using.ifPresent(usingIri -> {
				modifyQuery.append(USING).append(" ");
				
				if(usingNamed) {
					modifyQuery.append(NAMED).append(" ");
				}
	
				modifyQuery.append(usingIri.getQueryString());
				modifyQuery.append("\n");
			});
		
		modifyQuery.append(where.getQueryString());
		
		return modifyQuery.toString();
	}
}