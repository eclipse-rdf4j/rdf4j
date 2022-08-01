/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * A type-safe enumeration for RDF query languages such as {@link #SPARQL}. QueryLanguage objects are identified by
 * their name, which is treated in as case-insensitive way.
 */
public class QueryLanguage {

	/**
	 * SPARQL (Simple Protocol and RDF Query Language) is a W3C Recommendation for querying and updating RDF data.
	 *
	 * @see <a href="http://www.w3.org/TR/sparql11-overview/">SPARQL 1.1 Overview</a>
	 */
	public static final QueryLanguage SPARQL = new QueryLanguage("SPARQL");

	/**
	 * List of known query languages.
	 */
	private static final List<QueryLanguage> QUERY_LANGUAGES = new ArrayList<>(1);

	static {
		register(SPARQL);
	}

	/**
	 * Returns all known/registered query languages.
	 */
	public static Collection<QueryLanguage> values() {
		return Collections.unmodifiableList(QUERY_LANGUAGES);
	}

	/**
	 * Registers the specified query language.
	 *
	 * @param name The name of the query language, e.g. "SPARQL".
	 */
	public static QueryLanguage register(String name) {
		QueryLanguage ql = new QueryLanguage(name);
		register(ql);
		return ql;
	}

	/**
	 * Registers the specified query language.
	 */
	public static void register(QueryLanguage ql) {
		QUERY_LANGUAGES.add(ql);
	}

	/**
	 * Returns the query language whose name matches the specified name.
	 *
	 * @param qlName A query language name.
	 * @return The query language whose name matches the specified name, or <var>null</var> if there is no such query
	 *         language.
	 */
	public static QueryLanguage valueOf(String qlName) {
		for (QueryLanguage ql : QUERY_LANGUAGES) {
			if (ql.getName().equalsIgnoreCase(qlName)) {
				return ql;
			}
		}

		return null;
	}

	/**
	 * The query language's name.
	 */
	private final String name;

	/**
	 * Creates a new QueryLanguage object.
	 *
	 * @param name The (case-insensitive) name of the query language, e.g. "SPARQL".
	 */
	public QueryLanguage(String name) {
		assert name != null : "name must not be null";

		this.name = name;
	}

	/**
	 * Gets the name of this query language.
	 *
	 * @return A human-readable format name, e.g. "SPARQL".
	 */
	public String getName() {
		return name;
	}

	public boolean hasName(String name) {
		return this.name.equalsIgnoreCase(name);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof QueryLanguage) {
			QueryLanguage o = (QueryLanguage) other;
			return this.hasName(o.getName());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return getName().toUpperCase(Locale.ENGLISH).hashCode();
	}

	@Override
	public String toString() {
		return getName();
	}
}
