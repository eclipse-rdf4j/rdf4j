package org.eclipse.rdf4j.spanqit.core.query;

import java.util.Optional;

import org.eclipse.rdf4j.spanqit.core.Base;
import org.eclipse.rdf4j.spanqit.core.Prefix;
import org.eclipse.rdf4j.spanqit.core.PrefixDeclarations;
import org.eclipse.rdf4j.spanqit.core.QueryElement;
import org.eclipse.rdf4j.spanqit.core.Spanqit;
import org.eclipse.rdf4j.spanqit.core.TriplesTemplate;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphName;
import org.eclipse.rdf4j.spanqit.rdf.Iri;
import org.eclipse.rdf4j.spanqit.util.SpanqitUtils;

/**
 * A SPARQL Update query
 * 
 * @param <T> The type of update query. Used to support fluency. 
 *
 * @see <a
 * 		 href="https://www.w3.org/TR/sparql11-update/">
 * 		 SPARQL Update Query</a>
 */
@SuppressWarnings("unchecked")
abstract class UpdateQuery<T extends UpdateQuery<T>> implements QueryElement {
	private Optional<Base> base = Optional.empty();
	private Optional<PrefixDeclarations> prefixes = Optional.empty();
	
	UpdateQuery() {	}

	/**
	 * Set the base IRI of this query
	 * 
	 * @param iri
	 *            the base IRI
	 * @return this
	 */
	public T base(Iri iri) {
		this.base = Optional.of(Spanqit.base(iri));

		return (T) this;
	}

	/**
	 * Set the Base clause of this query
	 * 
	 * @param base
	 *            the {@link Base} clause to set
	 * @return this
	 */
	public T base(Base base) {
		this.base = Optional.of(base);

		return (T) this;
	}

	/**
	 * Add prefix declarations to this query
	 * 
	 * @param prefixes
	 *            the prefixes to add
	 * @return this
	 */
	public T prefix(Prefix... prefixes) {
		this.prefixes = SpanqitUtils.getOrCreateAndModifyOptional(this.prefixes, Spanqit::prefixes, p -> p.addPrefix(prefixes));

		return (T) this;
	}

	/**
	 * Set the Prefix declarations of this query
	 * 
	 * @param prefixes
	 *            the {@link PrefixDeclarations} to set
	 * @return this
	 */
	public T prefix(PrefixDeclarations prefixes) {
		this.prefixes = Optional.of(prefixes);

		return (T) this;
	}
	
	protected abstract String getQueryActionString();

	@Override
	public String getQueryString() {
		StringBuilder query = new StringBuilder();

		SpanqitUtils.appendAndNewlineIfPresent(base, query);
		SpanqitUtils.appendAndNewlineIfPresent(prefixes, query);

		query.append(getQueryActionString());
		
		return query.toString();
	}

	protected void appendNamedTriplesTemplates(StringBuilder queryString, Optional<GraphName> graphName, TriplesTemplate triples) {
		queryString.append(graphName.map(graph ->
				SpanqitUtils.getBracedString("GRAPH " + graph.getQueryString() + " " + triples.getQueryString()))
			.orElseGet(triples::getQueryString));
	}
}
