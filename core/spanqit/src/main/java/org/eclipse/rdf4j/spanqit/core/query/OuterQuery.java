package org.eclipse.rdf4j.spanqit.core.query;

import java.util.Optional;

import org.eclipse.rdf4j.spanqit.core.Base;
import org.eclipse.rdf4j.spanqit.core.Dataset;
import org.eclipse.rdf4j.spanqit.core.From;
import org.eclipse.rdf4j.spanqit.core.Prefix;
import org.eclipse.rdf4j.spanqit.core.PrefixDeclarations;
import org.eclipse.rdf4j.spanqit.core.Spanqit;
import org.eclipse.rdf4j.spanqit.rdf.Iri;
import org.eclipse.rdf4j.spanqit.util.SpanqitUtils;

/**
 * A non-subquery query.
 * 
 * @param <T>
 * 		The query type. Used to support fluency.
 */
@SuppressWarnings("unchecked")
public abstract class OuterQuery<T extends OuterQuery<T>> extends Query<T> {
	protected Optional<Base> base = Optional.empty();
	protected Optional<PrefixDeclarations> prefixes = Optional.empty();
	protected Optional<Dataset> from = Optional.empty();

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

	/**
	 * Add datasets to this query
	 * 
	 * @param graphs
	 *            the graph specifiers to add
	 * @return this
	 */
	public T from(From... graphs) {
		from = SpanqitUtils.getOrCreateAndModifyOptional(from, Spanqit::dataset, f -> f.from(graphs));

		return (T) this;
	}

	/**
	 * Set the Dataset clause for this query
	 * 
	 * @param from
	 *            the {@link Dataset} clause to set
	 * @return this
	 */
	public T from(Dataset from) {
		this.from = Optional.of(from);

		return (T) this;
	}

	@Override
	public String getQueryString() {
		StringBuilder query = new StringBuilder();

		SpanqitUtils.appendAndNewlineIfPresent(base, query);
		SpanqitUtils.appendAndNewlineIfPresent(prefixes, query);
		SpanqitUtils.appendAndNewlineIfPresent(from, query);

		query.append(super.getQueryString());

		return query.toString();
	}
}