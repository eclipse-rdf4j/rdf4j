/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import static org.eclipse.rdf4j.query.QueryLanguage.SERQL;
import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.rdf4j.common.iteration.Iterations;
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
import org.eclipse.rdf4j.query.parser.serql.SeRQLUtil;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLUtil;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dale Visser
 */
public class QueryEvaluator {

	private static final Logger LOGGER = LoggerFactory.getLogger(QueryEvaluator.class);

	private final ConsoleIO consoleIO;

	private final ConsoleState state;

	private final ConsoleParameters parameters;

	private final TupleAndGraphQueryEvaluator tg_eval;

	QueryEvaluator(ConsoleIO consoleIO, ConsoleState state, ConsoleParameters parameters) {
		this.consoleIO = consoleIO;
		this.state = state;
		this.parameters = parameters;
		this.tg_eval = new TupleAndGraphQueryEvaluator(consoleIO, state, parameters);
	}

	public void executeQuery(final String command, final String operation) {
		final List<String> sparqlQueryStart = Arrays.asList(new String[]{ "select", "construct", "describe", "ask", "prefix", "base"});
		if (sparqlQueryStart.contains(operation)) {
			evaluateQuery(QueryLanguage.SPARQL, command);
		}
		else if ("serql".equals(operation)) {
			evaluateQuery(QueryLanguage.SERQL, command.substring("serql".length()));
		}
		else if ("sparql".equals(operation)) {
			evaluateQuery(QueryLanguage.SPARQL, command.substring("sparql".length()));
		}
		else {
			consoleIO.writeError("Unknown command");
		}
	}

	private void evaluateQuery(final QueryLanguage queryLn, String queryText) {
		try {
			if (queryText.trim().isEmpty()) {
				consoleIO.writeln("enter multi-line " + queryLn.getName() + " query (terminate with line containing single '.')");
				queryText = consoleIO.readMultiLineInput();
			}
			final String queryString = addQueryPrefixes(queryLn, queryText);
			final ParsedOperation query = QueryParserUtil.parseOperation(queryLn, queryString, null);
			evaluateQuery(queryLn, queryString, query);
		}
		catch (UnsupportedQueryLanguageException e) {
			consoleIO.writeError("Unsupported query lanaguge: " + queryLn.getName());
		}
		catch (MalformedQueryException e) {
			consoleIO.writeError("Malformed query: " + e.getMessage());
		}
		catch (QueryInterruptedException e) {
			consoleIO.writeError("Query interrupted: " + e.getMessage());
			LOGGER.error("Query interrupted", e);
		}
		catch (QueryEvaluationException e) {
			consoleIO.writeError("Query evaluation error: " + e.getMessage());
			LOGGER.error("Query evaluation error", e);
		}
		catch (RepositoryException e) {
			consoleIO.writeError("Failed to evaluate query: " + e.getMessage());
			LOGGER.error("Failed to evaluate query", e);
		}
		catch (UpdateExecutionException e) {
			consoleIO.writeError("Failed to execute update: " + e.getMessage());
			LOGGER.error("Failed to execute update", e);
		}
		catch (IOException e) {
			consoleIO.writeError("I/O error: " + e.getMessage());
			LOGGER.error("Failed to read query", e);
		}
	}

	private void evaluateQuery(final QueryLanguage queryLn, final String queryString,
			final ParsedOperation query)
		throws MalformedQueryException, QueryEvaluationException, RepositoryException, UpdateExecutionException
	{
		if (query instanceof ParsedTupleQuery) {
			tg_eval.evaluateTupleQuery(queryLn, queryString);
		}
		else if (query instanceof ParsedGraphQuery) {
			tg_eval.evaluateGraphQuery(queryLn, queryString);
		}
		else if (query instanceof ParsedBooleanQuery) {
			evaluateBooleanQuery(queryLn, queryString);
		}
		else if (query instanceof ParsedUpdate) {
			executeUpdate(queryLn, queryString);
		}
		else {
			consoleIO.writeError("Unexpected query type");
		}
	}

	private String addQueryPrefixes(final QueryLanguage queryLn, final String queryString) {
		final StringBuffer result = new StringBuffer(queryString.length() + 512);
		result.append(queryString);
		final String lowerCaseQuery = queryString.toLowerCase(Locale.ENGLISH);
		Repository repository = state.getRepository();
		if (repository != null
				&& parameters.isQueryPrefix()
				&& ((SERQL.equals(queryLn) && lowerCaseQuery.indexOf("using namespace ") == -1) || SPARQL.equals(queryLn)
						&& !lowerCaseQuery.startsWith("prefix")))
		{
			// FIXME this is a bit of a sloppy hack, a better way would be to
			// explicitly provide the query parser with name space mappings in
			// advance.
			try {
				final RepositoryConnection con = repository.getConnection();
				try {
					final Collection<Namespace> namespaces = Iterations.asList(con.getNamespaces());
					if (!namespaces.isEmpty()) {
						addQueryPrefixes(queryLn, result, namespaces);
					}
				}
				finally {
					con.close();
				}
			}
			catch (RepositoryException e) {
				consoleIO.writeError("Error connecting to repository: " + e.getMessage());
				LOGGER.error("Error connecting to repository", e);
			}
		}
		return result.toString();
	}

	private void addQueryPrefixes(final QueryLanguage queryLn, final StringBuffer result,
			final Collection<Namespace> namespaces)
	{
		final StringBuilder namespaceClause = new StringBuilder(512);
		if (SERQL.equals(queryLn)) {
			namespaceClause.append(" USING NAMESPACE ");
			for (Namespace namespace : namespaces) {
				namespaceClause.append(namespace.getPrefix());
				namespaceClause.append(" = ");
				namespaceClause.append("<");
				namespaceClause.append(SeRQLUtil.encodeString(namespace.getName()));
				namespaceClause.append(">, ");
			}
			// Remove trailing ", "
			namespaceClause.setLength(namespaceClause.length() - 2);
			result.append(namespaceClause.toString());
		}
		else if (SPARQL.equals(queryLn)) {
			for (Namespace namespace : namespaces) {
				namespaceClause.append("PREFIX ");
				namespaceClause.append(namespace.getPrefix());
				namespaceClause.append(": ");
				namespaceClause.append("<");
				namespaceClause.append(SPARQLUtil.encodeString(namespace.getName()));
				namespaceClause.append("> ");
			}
			result.insert(0, namespaceClause);
		}
	}

	private void evaluateBooleanQuery(final QueryLanguage queryLn, final String queryString)
		throws UnsupportedQueryLanguageException, MalformedQueryException, QueryEvaluationException,
		RepositoryException
	{
		Repository repository = state.getRepository();
		if (repository == null) {
			consoleIO.writeUnopenedError();
			return;
		}
		final RepositoryConnection con = repository.getConnection();
		try {
			consoleIO.writeln("Evaluating " + queryLn.getName() + " query...");
			final long startTime = System.nanoTime();
			final boolean result = con.prepareBooleanQuery(queryLn, queryString).evaluate();
			consoleIO.writeln("Answer: " + result);
			final long endTime = System.nanoTime();
			consoleIO.writeln("Query evaluated in " + (endTime - startTime) / 1000000 + " ms");
		}
		finally {
			con.close();
		}
	}

	private void executeUpdate(final QueryLanguage queryLn, final String queryString)
		throws RepositoryException, UpdateExecutionException, MalformedQueryException
	{
		Repository repository = state.getRepository();
		if (repository == null) {
			consoleIO.writeUnopenedError();
			return;
		}
		final RepositoryConnection con = repository.getConnection();
		try {
			consoleIO.writeln("Executing update...");
			final long startTime = System.nanoTime();
			con.prepareUpdate(queryLn, queryString).execute();
			final long endTime = System.nanoTime();
			consoleIO.writeln("Update executed in " + (endTime - startTime) / 1000000 + " ms");
		}
		finally {
			con.close();
		}
	}

}
