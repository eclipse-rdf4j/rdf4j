/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.text.StringUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.ntriples.NTriplesUtil;

/**
 * @author dale
 */
public class TupleAndGraphQueryEvaluator {

	private final ConsoleIO consoleIO;

	private final ConsoleState state;

	private final ConsoleParameters parameters;

	private static final ParserConfig nonVerifyingParserConfig;
	
	static {
		nonVerifyingParserConfig = new ParserConfig();
		nonVerifyingParserConfig.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false);
		nonVerifyingParserConfig.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false);
		nonVerifyingParserConfig.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false);
	}
	
	TupleAndGraphQueryEvaluator(ConsoleIO consoleIO, ConsoleState state, ConsoleParameters parameters) {
		this.consoleIO = consoleIO;
		this.state = state;
		this.parameters = parameters;
	}

	protected void evaluateTupleQuery(final QueryLanguage queryLn, final String queryString)
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
			final long startTime = System.nanoTime();
			consoleIO.writeln("Evaluating " + queryLn.getName() + " query...");
			final TupleQueryResult tupleQueryResult = con.prepareTupleQuery(queryLn, queryString).evaluate();
			try {
				int resultCount = 0;
				final List<String> bindingNames = tupleQueryResult.getBindingNames();
				if (bindingNames.isEmpty()) {
					while (tupleQueryResult.hasNext()) {
						tupleQueryResult.next();
						resultCount++;
					}
				}
				else {
					int consoleWidth = parameters.getWidth();
					final int columnWidth = (consoleWidth - 1) / bindingNames.size() - 3;

					// Build table header
					final StringBuilder builder = new StringBuilder(consoleWidth);
					for (String bindingName : bindingNames) {
						builder.append("| ").append(bindingName);
						StringUtil.appendN(' ', columnWidth - bindingName.length(), builder);
					}
					builder.append("|");
					final String header = builder.toString();

					// Build separator line
					builder.setLength(0);
					for (int i = bindingNames.size(); i > 0; i--) {
						builder.append('+');
						StringUtil.appendN('-', columnWidth + 1, builder);
					}
					builder.append('+');
					final String separatorLine = builder.toString();

					// consoleIO.write table header
					consoleIO.writeln(separatorLine);
					consoleIO.writeln(header);
					consoleIO.writeln(separatorLine);

					// consoleIO.write table rows
					final Collection<Namespace> namespaces = Iterations.asList(con.getNamespaces());
					while (tupleQueryResult.hasNext()) {
						final BindingSet bindingSet = tupleQueryResult.next();
						resultCount++;
						builder.setLength(0);
						for (String bindingName : bindingNames) {
							final Value value = bindingSet.getValue(bindingName);
							final String valueStr = getStringRepForValue(value, namespaces);
							builder.append("| ").append(valueStr);
							StringUtil.appendN(' ', columnWidth - valueStr.length(), builder);
						}
						builder.append("|");
						consoleIO.writeln(builder.toString());
					}
					consoleIO.writeln(separatorLine);
				}
				final long endTime = System.nanoTime();
				consoleIO.writeln(resultCount + " result(s) (" + (endTime - startTime) / 1000000 + " ms)");
			}
			finally {
				tupleQueryResult.close();
			}
		}
		finally {
			con.close();
		}
	}

	protected void evaluateGraphQuery(final QueryLanguage queryLn, final String queryString)
		throws UnsupportedQueryLanguageException, MalformedQueryException, QueryEvaluationException,
		RepositoryException
	{
		Repository repository = state.getRepository();
		if (repository == null) {
			consoleIO.writeUnopenedError();
			return;
		}
		final RepositoryConnection con = repository.getConnection();
		con.setParserConfig(nonVerifyingParserConfig);
		try {
			consoleIO.writeln("Evaluating " + queryLn.getName() + " query...");
			final long startTime = System.nanoTime();
			final Collection<Namespace> namespaces = Iterations.asList(con.getNamespaces());
			final GraphQueryResult queryResult = con.prepareGraphQuery(queryLn, queryString).evaluate();
			try {
				int resultCount = 0;
				while (queryResult.hasNext()) {
					final Statement statement = queryResult.next(); // NOPMD
					resultCount++;
					consoleIO.write(getStringRepForValue(statement.getSubject(), namespaces));
					consoleIO.write("   ");
					consoleIO.write(getStringRepForValue(statement.getPredicate(), namespaces));
					consoleIO.write("   ");
					consoleIO.write(getStringRepForValue(statement.getObject(), namespaces));
					consoleIO.writeln();
				}
				final long endTime = System.nanoTime();
				consoleIO.writeln(resultCount + " results (" + (endTime - startTime) / 1000000 + " ms)");
			}
			finally {
				queryResult.close();
			}
		}
		finally {
			con.close();
		}
	}

	private String getStringRepForValue(final Value value, final Collection<Namespace> namespaces) {
		String result = "";
		if (value != null) {
			if (parameters.isShowPrefix() && value instanceof IRI) {
				final IRI uri = (IRI)value;
				final String prefix = getPrefixForNamespace(uri.getNamespace(), namespaces);
				if (prefix == null) {
					result = NTriplesUtil.toNTriplesString(value);
				}
				else {
					result = prefix + ":" + uri.getLocalName();
				}
			}
			else {
				result = NTriplesUtil.toNTriplesString(value);
			}
		}
		return result;
	}

	private String getPrefixForNamespace(final String namespace, final Collection<Namespace> namespaces) {
		String result = null;
		for (Namespace ns : namespaces) {
			if (namespace.equals(ns.getName())) {
				result = ns.getPrefix();
				break;
			}
		}
		return result;
	}

}
