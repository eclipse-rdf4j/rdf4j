/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.rdf4j.common.iteration.Iterations;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleParameters;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.console.setting.ConsoleSetting;
import org.eclipse.rdf4j.console.setting.QueryPrefix;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.parser.ParsedBooleanQuery;
import org.eclipse.rdf4j.query.parser.ParsedGraphQuery;
import org.eclipse.rdf4j.query.parser.ParsedOperation;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.ParsedUpdate;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dale Visser
 */
public abstract class QueryEvaluator extends ConsoleCommand {

	private static final Logger LOGGER = LoggerFactory.getLogger(QueryEvaluator.class);

	private final Map<String,ConsoleSetting> settings;
	private final TupleAndGraphQueryEvaluator evaluator;

	private final List<String> sparqlQueryStart = Arrays.asList(
								new String[]{"select", "construct", "describe", "ask", "prefix", "base"});
	
	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 * @param parameters 
	 */
	@Deprecated
	public QueryEvaluator(ConsoleIO consoleIO, ConsoleState state, ConsoleParameters parameters) {
		super(consoleIO, state);
		this.settings = convertParams(parameters);
		this.evaluator = new TupleAndGraphQueryEvaluator(consoleIO, state, parameters);
	}

	/**
	 * Constructor
	 * 
	 * @param evaluator
	 * @param settings 
	 */
	public QueryEvaluator(TupleAndGraphQueryEvaluator evaluator, Map<String,ConsoleSetting> settings) {
		super(evaluator.getConsoleIO(), evaluator.getConsoleState());
		this.settings = settings;
		this.evaluator = evaluator;
	}
	
	/**
	 * Check if query string already contains query prefixes
	 * 
	 * @param query query string
	 * @return true if namespaces are already used 
	 */
	protected abstract boolean hasQueryPrefixes(String query);
		
	/**
	 * Add namespace prefixes to query
	 * 
	 * @param result 
	 * @param namespaces collection of known namespaces
	 */
	protected abstract void addQueryPrefixes(StringBuffer result, Collection<Namespace> namespaces);
	

	/**
	 * Get show prefix setting
	 * Use a new show prefix setting when not found.
	 * 
	 * @return boolean
	 */
	private boolean getQueryPrefix() {
		return ((QueryPrefix) settings.getOrDefault(QueryPrefix.NAME, new QueryPrefix())).get();
	}
	
	/**
	 * Execute a SPARQL or SERQL query, defaults to SPARQL
	 * 
	 * @param command to execute
	 * @param operation "sparql", "serql", "base" or SPARQL query form
	 */
	public void executeQuery(final String command, final String operation) {
		if (sparqlQueryStart.contains(operation)) {
			evaluateQuery(QueryLanguage.SPARQL, command);
		} else if ("serql".equals(operation)) {
			evaluateQuery(QueryLanguage.SERQL, command.substring("serql".length()));
		} else if ("sparql".equals(operation)) {
			evaluateQuery(QueryLanguage.SPARQL, command.substring("sparql".length()));
		} else {
			consoleIO.writeError("Unknown command");
		}
	}

	/**
	 * Evaluate a SERQL or SPARQL query
	 * 
	 * @param queryLn query language
	 * @param queryText query string
	 */
	private void evaluateQuery(QueryLanguage queryLn, String queryText) {
		try {
			if (queryText.trim().isEmpty()) {
				consoleIO.writeln("enter multi-line " + queryLn.getName()
						+ " query (terminate with line containing single '.')");
				queryText = consoleIO.readMultiLineInput();
			}
		} catch (IOException e) {
			consoleIO.writeError("I/O error: " + e.getMessage());
			LOGGER.error("Failed to read query", e);
		}
	
		String queryString = addQueryPrefixes(queryText);

		try {
			ParsedOperation query = QueryParserUtil.parseOperation(queryLn, queryString, null);
			evaluateQuery(queryLn, queryString, query);
		} catch (UnsupportedQueryLanguageException e) {
			consoleIO.writeError("Unsupported query language: " + queryLn.getName());
		} catch (MalformedQueryException e) {
			consoleIO.writeError("Malformed query: " + e.getMessage());
		} catch (QueryInterruptedException e) {
			consoleIO.writeError("Query interrupted: " + e.getMessage());
			LOGGER.error("Query interrupted", e);
		} catch (QueryEvaluationException e) {
			consoleIO.writeError("Query evaluation error: " + e.getMessage());
			LOGGER.error("Query evaluation error", e);
		} catch (RepositoryException e) {
			consoleIO.writeError("Failed to evaluate query: " + e.getMessage());
			LOGGER.error("Failed to evaluate query", e);
		} catch (UpdateExecutionException e) {
			consoleIO.writeError("Failed to execute update: " + e.getMessage());
			LOGGER.error("Failed to execute update", e);
		}
	}

	/**
	 * Evaluate a SPARQL or SERQL query that has already been parsed
	 * 
	 * @param queryLn query language
	 * @param queryString query string
	 * @param query parsed query
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 * @throws RepositoryException
	 * @throws UpdateExecutionException 
	 */
	private void evaluateQuery(QueryLanguage queryLn, String queryString,  ParsedOperation query)
			throws MalformedQueryException, QueryEvaluationException, RepositoryException,
				UpdateExecutionException {
		Repository repository = state.getRepository();
		if (repository == null) {
			consoleIO.writeUnopenedError();
			return;
		}
	
		if (query instanceof ParsedTupleQuery) {
			evaluator.evaluateTupleQuery(queryLn, queryString);
		} else if (query instanceof ParsedGraphQuery) {
			evaluator.evaluateGraphQuery(queryLn, queryString);
		} else if (query instanceof ParsedBooleanQuery) {
			evaluator.evaluateBooleanQuery(queryLn, queryString);
		} else if (query instanceof ParsedUpdate) {
			evaluator.executeUpdate(queryLn, queryString);
		} else {
			consoleIO.writeError("Unexpected query type");
		}
	}

	/**
	 * Add namespaces prefixes to SPARQL or SERQL query
	 * 
	 * @param queryString query string
	 * @return query string with prefixes
	 */
	private String addQueryPrefixes(String queryString) {
		StringBuffer result = new StringBuffer(queryString.length() + 512);
		result.append(queryString);
		
		String upperCaseQuery = queryString.toUpperCase(Locale.ENGLISH);
		Repository repository = state.getRepository();
			
		if (repository != null && getQueryPrefix() && !hasQueryPrefixes(upperCaseQuery)) {
			// FIXME this is a bit of a sloppy hack, a better way would be to
			// explicitly provide the query parser with name space mappings in
			// advance.
			try {
				try (RepositoryConnection con = repository.getConnection()) {
					Collection<Namespace> namespaces = Iterations.asList(con.getNamespaces());
					if (!namespaces.isEmpty()) {
						addQueryPrefixes(result, namespaces);
					}
				}
			} catch (RepositoryException e) {
				consoleIO.writeError("Error connecting to repository: " + e.getMessage());
				LOGGER.error("Error connecting to repository", e);
			}
		}
		return result.toString();
	}
}
