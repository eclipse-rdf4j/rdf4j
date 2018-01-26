package org.eclipse.rdf4j.spanqit.core.query;

import java.util.Optional;

import org.eclipse.rdf4j.spanqit.rdf.Iri;

@SuppressWarnings("javadoc")
public abstract class TargetedGraphManagementQuery<T extends TargetedGraphManagementQuery<T>> extends GraphManagementQuery<TargetedGraphManagementQuery<T>> {
	private static final String GRAPH = "GRAPH";
	private static final String DEFAULT = "DEFAULT";
	private static final String NAMED = "NAMED";
	private static final String ALL = "ALL";

	private String target = DEFAULT;
	private Optional<Iri> graph = Optional.empty();
	
	/**
	 * Specify which graph to target
	 * 
	 * @param graph the IRI identifying the graph to target
	 * 
	 * @return this query instance
	 */
	@SuppressWarnings("unchecked")
	public T graph(Iri graph) {
		this.graph = Optional.ofNullable(graph);
		
		return (T) this;
	}

	/**
	 * Target the default graph
	 * 
	 * @return this query instance
	 */
	public T def() {
		return target(DEFAULT);
	}
	
	/**
	 * Target all named graphs
	 * 
	 * @return this query instance
	 */
	public T named() {
		return target(NAMED);
	}
	
	/**
	 * Target all graphs
	 * 
	 * @return this query instance
	 */
	public T all() {
		return target(ALL);
	}
	
	@SuppressWarnings("unchecked")
	private T target(String target) {
		this.target = target;
		graph = Optional.empty();
		
		return (T) this;
	}
	
	protected abstract String getQueryActionString();

	@Override
	public String getQueryString() {
		StringBuilder query = new StringBuilder();
		
		query.append(getQueryActionString()).append(" ");
		
		appendSilent(query);
		
		String targetString = graph.map(iri -> GRAPH + " " + iri.getQueryString()).orElse(target);
		query.append(targetString);
		
		return query.toString();
	}
}
