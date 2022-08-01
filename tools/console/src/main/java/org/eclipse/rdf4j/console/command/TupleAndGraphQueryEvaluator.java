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
package org.eclipse.rdf4j.console.command;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.console.setting.ConsoleSetting;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.query.resultio.QueryResultWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;

/**
 * Evaluator tuple and graph queries
 *
 * @author dale
 */
public class TupleAndGraphQueryEvaluator {

	private final ConsoleIO consoleIO;
	private final ConsoleState state;
	private final Map<String, ConsoleSetting> settings;

	private static final ParserConfig nonVerifyingParserConfig;

	static {
		nonVerifyingParserConfig = new ParserConfig();
		nonVerifyingParserConfig.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false);
		nonVerifyingParserConfig.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false);
		nonVerifyingParserConfig.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false);
	}

	/**
	 * Constructor
	 *
	 * @param consoleIO
	 * @param state
	 * @param settings
	 */
	public TupleAndGraphQueryEvaluator(ConsoleIO consoleIO, ConsoleState state, Map<String, ConsoleSetting> settings) {
		this.consoleIO = consoleIO;
		this.state = state;
		this.settings = settings;
	}

	/**
	 * Get console IO
	 *
	 * @return console IO
	 */
	protected ConsoleIO getConsoleIO() {
		return this.consoleIO;
	}

	/**
	 * Get console State
	 *
	 * @return console state
	 */
	protected ConsoleState getConsoleState() {
		return this.state;
	}

	/**
	 * Get console settings
	 *
	 * @return console settings
	 */
	protected Map<String, ConsoleSetting> getConsoleSettings() {
		return this.settings;
	}

	/**
	 * Evaluate SPARQL tuple query and send the output to a writer. If writer is null, the console will be used for
	 * output.
	 *
	 * @param queryLn     query language
	 * @param queryString query string
	 * @param writer      result writer or null
	 * @throws UnsupportedQueryLanguageException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 * @throws RepositoryException
	 */
	protected void evaluateTupleQuery(QueryLanguage queryLn, String queryString, QueryResultWriter writer)
			throws UnsupportedQueryLanguageException, MalformedQueryException, QueryEvaluationException,
			RepositoryException {
		Repository repository = state.getRepository();

		consoleIO.writeln("Evaluating " + queryLn.getName() + " query...");
		int resultCount = 0;
		long startTime = System.nanoTime();

		try (RepositoryConnection con = repository.getConnection();
				TupleQueryResult res = con.prepareTupleQuery(queryLn, queryString).evaluate()) {

			List<String> bindingNames = res.getBindingNames();
			if (bindingNames.isEmpty()) {
				while (res.hasNext()) {
					res.next();
					resultCount++;
				}
			} else {
				writer.startDocument();
				writer.startHeader();
				writer.startQueryResult(bindingNames);
				writer.endHeader();

				while (res.hasNext()) {
					writer.handleSolution(res.next());
					resultCount++;
				}
				writer.endQueryResult();
			}
		}
		long endTime = System.nanoTime();
		consoleIO.writeln(resultCount + " result(s) (" + (endTime - startTime) / 1_000_000 + " ms)");
	}

	/**
	 * Evaluate SPARQL graph query
	 *
	 * @param queryLn     query language
	 * @param queryString query string
	 * @param writer      RDFWriter to write the results to
	 * @param namespaces  namespaces to write to the RDFWriter
	 * @throws UnsupportedQueryLanguageException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 * @throws RepositoryException
	 */
	protected void evaluateGraphQuery(QueryLanguage queryLn, String queryString, RDFWriter writer,
			Collection<Namespace> namespaces) throws UnsupportedQueryLanguageException, MalformedQueryException,
			QueryEvaluationException, RepositoryException {
		Repository repository = state.getRepository();

		consoleIO.writeln("Evaluating " + queryLn.getName() + " query...");
		int resultCount = 0;
		long startTime = System.nanoTime();

		try (RepositoryConnection con = repository.getConnection();
				GraphQueryResult res = con.prepareGraphQuery(queryLn, queryString).evaluate()) {

			con.setParserConfig(nonVerifyingParserConfig);

			writer.startRDF();

			namespaces.forEach(ns -> writer.handleNamespace(ns.getPrefix(), ns.getName()));

			while (res.hasNext()) {
				writer.handleStatement(res.next());
				resultCount++;
			}
			writer.endRDF();
		}
		long endTime = System.nanoTime();
		consoleIO.writeln(resultCount + " results (" + (endTime - startTime) / 1_000_000 + " ms)");
	}

	/**
	 * Evaluate a boolean SPARQL query
	 *
	 * @param queryLn     query language
	 * @param queryString query string
	 * @param writer
	 * @throws UnsupportedQueryLanguageException
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 * @throws RepositoryException
	 */
	protected void evaluateBooleanQuery(QueryLanguage queryLn, String queryString, QueryResultWriter writer)
			throws UnsupportedQueryLanguageException, MalformedQueryException, QueryEvaluationException,
			RepositoryException {
		Repository repository = state.getRepository();

		consoleIO.writeln("Evaluating " + queryLn.getName() + " query...");
		long startTime = System.nanoTime();

		try (RepositoryConnection con = repository.getConnection()) {
			boolean result = con.prepareBooleanQuery(queryLn, queryString).evaluate();

			writer.startDocument();
			writer.handleBoolean(result);
			writer.endQueryResult();
		}
		long endTime = System.nanoTime();
		consoleIO.writeln("Query evaluated in " + (endTime - startTime) / 1_000_000 + " ms");
	}

	/**
	 * Execute a SPARQL update
	 *
	 * @param queryLn     query language
	 * @param queryString query string
	 * @throws RepositoryException
	 * @throws UpdateExecutionException
	 * @throws MalformedQueryException
	 */
	protected void executeUpdate(QueryLanguage queryLn, String queryString)
			throws RepositoryException, UpdateExecutionException, MalformedQueryException {
		Repository repository = state.getRepository();

		consoleIO.writeln("Executing update...");
		long startTime = System.nanoTime();

		try (RepositoryConnection con = repository.getConnection()) {
			con.prepareUpdate(queryLn, queryString).execute();
		}

		long endTime = System.nanoTime();
		consoleIO.writeln("Update executed in " + (endTime - startTime) / 1_000_000 + " ms");
	}
}
