/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.QueryParserFactory;
import org.eclipse.rdf4j.query.parser.QueryParserRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;

public class SparqlQueryParserCache {

	private static final Logger logger = LoggerFactory.getLogger(SparqlQueryParserCache.class);

	private static final Cache<String, TupleExpr> PARSER_QUERY_CACHE = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.MINUTES)
			.concurrencyLevel(Runtime.getRuntime().availableProcessors() * 2)
			.maximumSize(10000)
			.build();

	private static final QueryParser QUERY_PARSER;

	static {
		Optional<QueryParserFactory> queryParserFactory = QueryParserRegistry.getInstance()
				.get(QueryLanguage.SPARQL);

		QUERY_PARSER = queryParserFactory
				.orElseThrow(() -> new IllegalStateException("Query parser factory for SPARQL is missing!"))
				.getParser();
	}

	public static TupleExpr get(String query) {
		try {
			return PARSER_QUERY_CACHE.get(query, () -> QUERY_PARSER.parseQuery(query, null).getTupleExpr()).clone();
		} catch (ExecutionException | UncheckedExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof MalformedQueryException) {
				logger.error("Error parsing query: \n{}", query, cause);
				throw ((MalformedQueryException) cause);
			}
			if (cause instanceof RuntimeException) {
				throw ((RuntimeException) cause);
			}
			if (cause instanceof Error) {
				throw ((Error) cause);
			}
			if (cause != null) {
				throw new IllegalStateException(cause);
			}
			throw new IllegalStateException(e);
		}

	}

}
