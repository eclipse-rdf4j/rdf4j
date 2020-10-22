/*******************************************************************************
 Copyright (c) 2018 Eclipse RDF4J contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Distribution License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core.query;

import java.util.Optional;

import org.eclipse.rdf4j.sparqlbuilder.core.Base;
import org.eclipse.rdf4j.sparqlbuilder.core.Prefix;
import org.eclipse.rdf4j.sparqlbuilder.core.PrefixDeclarations;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.util.SparqlBuilderUtils;

/**
 * A non-subquery query.
 *
 * @param <T> The query type. Used to support fluency.
 */
@SuppressWarnings("unchecked")
public abstract class OuterQuery<T extends OuterQuery<T>> extends Query<T> {
	protected Optional<Base> base = Optional.empty();
	protected Optional<PrefixDeclarations> prefixes = Optional.empty();

	/**
	 * Set the base IRI of this query
	 *
	 * @param iri the base IRI
	 * @return this
	 */
	public T base(Iri iri) {
		this.base = Optional.of(SparqlBuilder.base(iri));

		return (T) this;
	}

	/**
	 * Set the Base clause of this query
	 *
	 * @param base the {@link Base} clause to set
	 * @return this
	 */
	public T base(Base base) {
		this.base = Optional.of(base);

		return (T) this;
	}

	/**
	 * Add prefix declarations to this query
	 *
	 * @param prefixes the prefixes to add
	 * @return this
	 */
	public T prefix(Prefix... prefixes) {
		this.prefixes = SparqlBuilderUtils.getOrCreateAndModifyOptional(this.prefixes, SparqlBuilder::prefixes,
				p -> p.addPrefix(prefixes));

		return (T) this;
	}

	/**
	 * Set the Prefix declarations of this query
	 *
	 * @param prefixes the {@link PrefixDeclarations} to set
	 * @return this
	 */
	public T prefix(PrefixDeclarations prefixes) {
		this.prefixes = Optional.of(prefixes);

		return (T) this;
	}

	@Override
	public String getQueryString() {
		StringBuilder query = new StringBuilder();

		SparqlBuilderUtils.appendAndNewlineIfPresent(base, query);
		SparqlBuilderUtils.appendAndNewlineIfPresent(prefixes, query);

		query.append(super.getQueryString());

		return query.toString();
	}
}
