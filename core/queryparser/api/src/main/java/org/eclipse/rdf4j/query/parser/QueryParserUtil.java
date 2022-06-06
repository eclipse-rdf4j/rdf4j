/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.parser;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.parser.QueryPrologLexer.Token;

/**
 * Utility class for creating query parsers and parsing queries in various query languages.
 */
public class QueryParserUtil {

	public static QueryParser createParser(QueryLanguage ql) throws UnsupportedQueryLanguageException {
		QueryParserFactory factory = QueryParserRegistry.getInstance()
				.get(ql)
				.orElseThrow(
						() -> new UnsupportedQueryLanguageException("No factory available for query language " + ql));
		return factory.getParser();
	}

	/**
	 * Parses the supplied operation into a query model.
	 *
	 * @param ql        The language in which the operation is formulated.
	 * @param operation The operation.
	 * @param baseURI   The base URI to resolve any relative URIs that are in the operation against, can be
	 *                  <var>null</var> if the operation does not contain any relative URIs.
	 * @return The model for the parsed operation.
	 * @throws MalformedQueryException           If the supplied operation was malformed.
	 * @throws UnsupportedQueryLanguageException If the specified query language is not supported.
	 */
	public static ParsedOperation parseOperation(QueryLanguage ql, String operation, String baseURI)
			throws MalformedQueryException {
		ParsedOperation parsedOperation;
		QueryParser parser = createParser(ql);

		if (QueryLanguage.SPARQL.equals(ql)) {
			String strippedOperation = removeSPARQLQueryProlog(operation).toUpperCase();

			if (strippedOperation.startsWith("SELECT") || strippedOperation.startsWith("CONSTRUCT")
					|| strippedOperation.startsWith("DESCRIBE") || strippedOperation.startsWith("ASK")) {
				parsedOperation = parser.parseQuery(operation, baseURI);
			} else {
				parsedOperation = parser.parseUpdate(operation, baseURI);
			}
		} else {
			// SPARQL is the only QL supported by sesame that has update
			// operations, so we simply redirect to parseQuery
			parsedOperation = parser.parseQuery(operation, baseURI);
		}

		return parsedOperation;
	}

	/**
	 * Parses the supplied update operation into a query model.
	 *
	 * @param ql      The language in which the update operation is formulated.
	 * @param update  The update operation.
	 * @param baseURI The base URI to resolve any relative URIs that are in the operation against, can be
	 *                <var>null</var> if the update operation does not contain any relative URIs.
	 * @return The model for the parsed update operation.
	 * @throws MalformedQueryException           If the supplied update operation was malformed.
	 * @throws UnsupportedQueryLanguageException If the specified query language is not supported.
	 */
	public static ParsedUpdate parseUpdate(QueryLanguage ql, String update, String baseURI)
			throws MalformedQueryException, UnsupportedQueryLanguageException {
		QueryParser parser = createParser(ql);
		return parser.parseUpdate(update, baseURI);
	}

	/**
	 * Parses the supplied query into a query model.
	 *
	 * @param ql      The language in which the query is formulated.
	 * @param query   The query.
	 * @param baseURI The base URI to resolve any relative URIs that are in the query against, can be <var>null</var> if
	 *                the query does not contain any relative URIs.
	 * @return The query model for the parsed query.
	 * @throws MalformedQueryException           If the supplied query was malformed.
	 * @throws UnsupportedQueryLanguageException If the specified query language is not supported.
	 */
	public static ParsedQuery parseQuery(QueryLanguage ql, String query, String baseURI)
			throws MalformedQueryException, UnsupportedQueryLanguageException {
		QueryParser parser = createParser(ql);
		return parser.parseQuery(query, baseURI);
	}

	/**
	 * Parses the supplied query into a query model.
	 *
	 * @param ql    The language in which the query is formulated.
	 * @param query The query.
	 * @return The query model for the parsed query.
	 * @throws IllegalArgumentException          If the supplied query is not a tuple query.
	 * @throws MalformedQueryException           If the supplied query was malformed.
	 * @throws UnsupportedQueryLanguageException If the specified query language is not supported.
	 */
	public static ParsedTupleQuery parseTupleQuery(QueryLanguage ql, String query, String baseURI)
			throws MalformedQueryException, UnsupportedQueryLanguageException {
		ParsedOperation q = parseQuery(ql, query, baseURI);

		if (q instanceof ParsedTupleQuery) {
			return (ParsedTupleQuery) q;
		}

		throw new IllegalArgumentException("query is not a tuple query: " + query);
	}

	/**
	 * Parses the supplied query into a query model.
	 *
	 * @param ql    The language in which the query is formulated.
	 * @param query The query.
	 * @return The query model for the parsed query.
	 * @throws IllegalArgumentException          If the supplied query is not a graph query.
	 * @throws MalformedQueryException           If the supplied query was malformed.
	 * @throws UnsupportedQueryLanguageException If the specified query language is not supported.
	 */
	public static ParsedGraphQuery parseGraphQuery(QueryLanguage ql, String query, String baseURI)
			throws MalformedQueryException, UnsupportedQueryLanguageException {
		ParsedOperation q = parseQuery(ql, query, baseURI);

		if (q instanceof ParsedGraphQuery) {
			return (ParsedGraphQuery) q;
		}

		throw new IllegalArgumentException("query is not a graph query: " + query);
	}

	/**
	 * Parses the supplied query into a query model.
	 *
	 * @param ql    The language in which the query is formulated.
	 * @param query The query.
	 * @return The query model for the parsed query.
	 * @throws IllegalArgumentException          If the supplied query is not a graph query.
	 * @throws MalformedQueryException           If the supplied query was malformed.
	 * @throws UnsupportedQueryLanguageException If the specified query language is not supported.
	 */
	public static ParsedBooleanQuery parseBooleanQuery(QueryLanguage ql, String query, String baseURI)
			throws MalformedQueryException, UnsupportedQueryLanguageException {
		ParsedOperation q = parseQuery(ql, query, baseURI);

		if (q instanceof ParsedBooleanQuery) {
			return (ParsedBooleanQuery) q;
		}

		throw new IllegalArgumentException("query is not a boolean query: " + query);
	}

	/**
	 * Removes SPARQL prefix and base declarations, if any, from the supplied SPARQL query string. The supplied query
	 * string is assumed to be syntactically legal.
	 *
	 * @param queryString a syntactically legal SPARQL query string
	 * @return a substring of queryString, with prefix and base declarations removed.
	 */
	public static String removeSPARQLQueryProlog(String queryString) {
		final Token t = QueryPrologLexer.getRestOfQueryToken(queryString);
		if (t != null) {
			return t.getStringValue();
		} else {
			return queryString;
		}
	}

}
