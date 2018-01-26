package org.eclipse.rdf4j.spanqit.core.query;

import java.util.Optional;

import org.eclipse.rdf4j.spanqit.rdf.Iri;

/**
 * A SPARQL Update Query that has a source and a destination
 *
 * @param <T> the type of the query; used to support fluency
 */
public abstract class DestinationSourceManagementQuery<T extends DestinationSourceManagementQuery<T>> extends GraphManagementQuery<DestinationSourceManagementQuery<T>> {
	private static String DEFAULT = "DEFAULT";
	private static String TO = "TO";

	private Optional<Iri> from = Optional.empty();
	private Optional<Iri> to = Optional.empty();
	private boolean fromDefault = false, toDefault = false;
	
	/**
	 * Specify the query source graph
	 * 
	 * @param from the Iri identifying the source graph
	 * 
	 * @return this query instance
	 */
	public T from(Iri from) {
		this.from = Optional.ofNullable(from);
		
		return fromDefault(false);
	}
	
	/**
	 * Specify the query destination graph
	 * 
	 * @param to the Iri identifying the destination graph
	 * 
	 * @return this query instance
	 */
	public T to(Iri to) {
		this.to = Optional.ofNullable(to);
		
		return toDefault(false);
	}
	
	/**
	 * Specify that the source graph of this query should be the default graph
	 * 
	 * @return this query instance
	 */
	public T fromDefault() {
		return fromDefault(true);
	}

	/**
	 * Specify if this query's source should be the default graph
	 * 
	 * @param fromDefault if this query's source should be the default graph
	 * 
	 * @return this query instance
	 */
	@SuppressWarnings("unchecked")
	public T fromDefault(boolean fromDefault) {
		this.fromDefault = fromDefault;
		
		return (T) this;
	}
	
	/**
	 * Specify that the destination graph of this query should be the default graph
	 * 
	 * @return this query instance
	 */
	public T toDefault() {
		return toDefault(true);
	}
	
	/**
	 * Specify if this query's destination should be the default graph
	 * 
	 * @param toDefault if this query's destination should be the default graph
	 * 
	 * @return this query instance
	 */
	@SuppressWarnings("unchecked")
	public T toDefault(boolean toDefault) {
		this.toDefault = toDefault;
		
		return (T) this;
	}
	
	protected abstract String getQueryActionString();
	
	@Override
	public String getQueryString() {
		StringBuilder query = new StringBuilder();
		query.append(getQueryActionString()).append(" ");
		
		appendSilent(query);
		
//		if(fromDefault) {
//			query.append(DEFAULT);
//		} else {
//			query.append(from.map(Iri::getQueryString).orElse(DEFAULT));
//		}
		
		query.append(from.filter(f -> !fromDefault)
				.map(Iri::getQueryString)
				.orElse(DEFAULT));
		
		query.append(" ").append(TO).append(" ");
		
		query.append(to.filter(t -> !toDefault)
				.map(Iri::getQueryString)
				.orElse(DEFAULT));

//		if(toDefault) {
//			query.append(DEFAULT);
//		} else {
//			query.append(to.map(Iri::getQueryString).orElse(DEFAULT));
//		}
		
		return query.toString();
	}
}
