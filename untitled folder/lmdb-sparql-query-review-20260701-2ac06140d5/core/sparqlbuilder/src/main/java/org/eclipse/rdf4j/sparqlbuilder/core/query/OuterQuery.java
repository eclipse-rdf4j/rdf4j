/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core.query;

import static org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf.iri;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
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
	 * Set the base IRI of this query
	 *
	 * @param iri the base IRI
	 * @return this
	 */
	public T base(IRI iri) {
		return base(iri(iri));
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
	 * Add prefix declarations to this query
	 *
	 * @param namespaces the namespaces to use for prefixes
	 * @return
	 */
	public T prefix(Namespace... namespaces) {
		return prefix(Arrays
				.stream(namespaces)
				.map(n -> SparqlBuilder.prefix(n))
				.collect(Collectors.toList())
				.toArray(new Prefix[namespaces.length])
		);
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
		String queryString = super.getQueryString();
		if (prefixes.isPresent()) {
			queryString = prefixes.get().replacePrefixesInQuery(queryString);
		}
		query.append(queryString);

		return query.toString();
	}
}
