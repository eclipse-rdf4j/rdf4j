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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.common.io.UncloseableOutputStream;
import org.eclipse.rdf4j.console.Util;
import org.eclipse.rdf4j.console.setting.ConsoleWidth;
import org.eclipse.rdf4j.console.setting.Prefixes;
import org.eclipse.rdf4j.console.setting.QueryPrefix;
import org.eclipse.rdf4j.console.setting.ShowPrefix;
import org.eclipse.rdf4j.console.setting.WorkDir;
import org.eclipse.rdf4j.console.util.ConsoleQueryResultWriter;
import org.eclipse.rdf4j.console.util.ConsoleRDFWriter;
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
import org.eclipse.rdf4j.query.resultio.QueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.QueryResultWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

/**
 * Abstract query evaluator command
 *
 * @author Dale Visser
 * @author Bart Hanssens
 */
public abstract class QueryEvaluator extends ConsoleCommand {
	private final TupleAndGraphQueryEvaluator evaluator;

	private final List<String> sparqlQueryStart = Arrays
			.asList(new String[] { "select", "construct", "describe", "ask", "prefix", "base" });

	private final long MAX_INPUT = 1_000_000;

	// [INFILE="input file"[,enc]] [OUTPUT="out/file"]
	private final static Pattern PATTERN_IO = Pattern.compile(
			"^(?<in>INFILE=\"(?<i>[^\"]+)\"" + ",?(?<enc>\\w[\\w-]+)?)? ?" + "(?<out>OUTFILE=\"(?<o>[^\"]+)\")?",
			Pattern.CASE_INSENSITIVE);

	/**
	 * Constructor
	 *
	 * @param evaluator
	 */
	public QueryEvaluator(TupleAndGraphQueryEvaluator evaluator) {
		super(evaluator.getConsoleIO(), evaluator.getConsoleState(), evaluator.getConsoleSettings());
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

	@Override
	public String[] usesSettings() {
		return new String[] { ConsoleWidth.NAME,
				Prefixes.NAME, QueryPrefix.NAME, ShowPrefix.NAME,
				WorkDir.NAME };
	}

	/**
	 * Get console width setting.
	 *
	 * @return width in columns
	 */
	private int getConsoleWidth() {
		return ((ConsoleWidth) settings.get(ConsoleWidth.NAME)).get();
	}

	/**
	 * Get query prefix setting.
	 *
	 * @return true if prefixes are used for querying
	 */
	private boolean getQueryPrefix() {
		return ((QueryPrefix) settings.get(QueryPrefix.NAME)).get();
	}

	/**
	 * Get show prefix setting.
	 *
	 * @return true if prefixes are used for displaying.
	 */
	private boolean getShowPrefix() {
		return ((ShowPrefix) settings.get(ShowPrefix.NAME)).get();
	}

	/**
	 * Get a set of namespaces
	 *
	 * @return set of namespace prefixes
	 */
	private Set<Namespace> getPrefixes() {
		return ((Prefixes) settings.get(Prefixes.NAME)).get();
	}

	/**
	 * Get working dir setting Use a working dir setting when not found.
	 *
	 * @return path of working dir
	 */
	private Path getWorkDir() {
		return ((WorkDir) settings.get(WorkDir.NAME)).get();
	}

	/**
	 * Execute a SPARQL query
	 *
	 * @param command   to execute
	 * @param operation "sparql", "base" or SPARQL query form
	 */
	public void executeQuery(final String command, final String operation) {
		Repository repository = state.getRepository();
		if (repository == null) {
			writeUnopenedError();
			return;
		}

		if (sparqlQueryStart.contains(operation)) {
			parseAndEvaluateQuery(QueryLanguage.SPARQL, command);
		} else if ("sparql".equals(operation)) {
			parseAndEvaluateQuery(QueryLanguage.SPARQL, command.substring("sparql".length()));
		} else {
			writeError("Unknown command");
		}
	}

	/**
	 * Read query string from a file. Optionally a character set can be specified, otherwise UTF-8 will be used.
	 *
	 * @param filename file name
	 * @param cset     character set name or null
	 * @return query from file as string
	 * @throws IllegalArgumentException when character set was not recognized
	 * @throws IOException              when input file could not be read
	 */
	private String readFile(String filename, String cset) throws IllegalArgumentException, IOException {
		if (filename == null || filename.isEmpty()) {
			throw new IllegalArgumentException("Empty file name");
		}
		Charset charset = (cset == null || cset.isEmpty()) ? StandardCharsets.UTF_8 : Charset.forName(cset);

		Path p = Util.getNormalizedPath(getWorkDir(), filename);
		if (!p.toFile().canRead()) {
			throw new IOException("Cannot read file " + p);
		}
		// limit file size
		if (p.toFile().length() > MAX_INPUT) {
			throw new IOException("File larger than " + MAX_INPUT + " bytes");
		}
		byte[] bytes = Files.readAllBytes(p);
		return new String(bytes, charset);
	}

	/**
	 * Get absolute path to output file, using working directory for relative file name. Verifies that the file doesn't
	 * exist or can be overwritten if it does exist.
	 *
	 * @param filename file name
	 * @return path absolute path
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	private Path getPathForOutput(String filename) throws IllegalArgumentException, IOException {
		if (filename == null || filename.isEmpty()) {
			throw new IllegalArgumentException("Empty file name");
		}

		Path p = Util.getNormalizedPath(getWorkDir(), filename);
		if (!p.toFile().exists() || askProceed("File exists, continue ?", false)) {
			return p;
		}
		throw new IOException("Could not open file for output");
	}

	/**
	 * Read (possibly multi-line) query. Returns multi-line query as one string, or the original string if query is not
	 * multi-line.
	 *
	 * @param queryLn   query language
	 * @param queryText query string
	 * @return query or null
	 */
	private String readMultiline(QueryLanguage queryLn, String queryText) {
		String str = queryText.trim();
		if (!str.isEmpty()) {
			return str;
		}
		try {
			writeln("Enter multi-line " + queryLn.getName() + " query "
					+ "(terminate with line containing single '.')");
			return consoleIO.readMultiLineInput();
		} catch (IOException e) {
			writeError("Failed to read query", e);
		}
		return null;
	}

	/**
	 * if results are to be written to an output file.
	 *
	 * @param queryLn   query language
	 * @param queryText query string
	 */
	private void parseAndEvaluateQuery(QueryLanguage queryLn, String queryText) {
		String str = readMultiline(queryLn, queryText);
		if (str == null || str.isEmpty()) {
			writeError("Empty query string");
			return;
		}

		Path path = null;

		// check if input and/or output file are specified
		Matcher m = PATTERN_IO.matcher(str);
		if (m.lookingAt()) {
			try {
				// check for output file first
				String outfile = m.group("o");
				if (outfile != null && !outfile.isEmpty()) {
					path = getPathForOutput(outfile);
					str = str.substring(m.group(0).length()); // strip both INPUT/OUTPUT from query
				}
				String infile = m.group("i");
				if (infile != null && !infile.isEmpty()) {
					str = readFile(infile, m.group("enc")); // ignore remainder of command line query
				}
			} catch (IOException | IllegalArgumentException ex) {
				writeError(ex.getMessage());
				return;
			}
		}

		// add namespace prefixes
		String queryString = addQueryPrefixes(str);

		try {
			ParsedOperation query = QueryParserUtil.parseOperation(queryLn, queryString, null);
			evaluateQuery(queryLn, query, path);
		} catch (UnsupportedQueryLanguageException e) {
			writeError("Unsupported query language: " + queryLn.getName());
		} catch (MalformedQueryException e) {
			writeError("Malformed query", e);
		} catch (QueryInterruptedException e) {
			writeError("Query interrupted", e);
		} catch (QueryEvaluationException e) {
			writeError("Query evaluation error", e);
		} catch (RepositoryException e) {
			writeError("Failed to evaluate query", e);
		} catch (UpdateExecutionException e) {
			writeError("Failed to execute update", e);
		}
	}

	/**
	 * Get a query result writer based upon the file name (extension), or return the console result writer when path is
	 * null.
	 *
	 * @param path path or null
	 * @param out  output stream or null
	 * @return result writer
	 * @throws IllegalArgumentException
	 */
	private QueryResultWriter getQueryResultWriter(Path path, OutputStream out) throws IllegalArgumentException {
		QueryResultWriter w;

		if (path == null) {
			w = new ConsoleQueryResultWriter(consoleIO, getConsoleWidth());
		} else {
			Optional<QueryResultFormat> fmt = QueryResultIO.getWriterFormatForFileName(path.toFile().toString());
			if (!fmt.isPresent()) {
				throw new IllegalArgumentException("No suitable result writer found");
			}
			w = QueryResultIO.createWriter(fmt.get(), out);
		}
		if (getShowPrefix()) {
			getPrefixes().stream().forEach(ns -> w.handleNamespace(ns.getPrefix(), ns.getName()));
		}
		return w;
	}

	/**
	 * Get a graph result (RIO) writer based upon the file name (extension), or return the console result writer when
	 * path is null.
	 *
	 * @param path path or null
	 * @param out  output stream or null
	 * @return result writer
	 * @throws IllegalArgumentException
	 */
	private RDFWriter getRDFWriter(Path path, OutputStream out) throws IllegalArgumentException {
		RDFWriter w;
		if (path == null) {
			w = new ConsoleRDFWriter(consoleIO, getConsoleWidth());
		} else {
			Optional<RDFFormat> fmt = Rio.getWriterFormatForFileName(path.toFile().toString());
			if (!fmt.isPresent()) {
				throw new IllegalArgumentException("No suitable result writer found");
			}
			w = Rio.createWriter(fmt.get(), out);
		}
		return w;
	}

	/**
	 * Get output stream for a file, or for the console output if path is null
	 *
	 * @param path file path or null
	 * @return file or console output stream
	 * @throws IOException
	 */
	private OutputStream getOutputStream(Path path) throws IOException {
		return (path != null) ? Files.newOutputStream(path, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
				: new UncloseableOutputStream(consoleIO.getOutputStream());
	}

	/**
	 * Evaluate a SPARQL query that has already been parsed
	 *
	 * @param queryLn query language
	 * @param query   parsed query
	 * @param path
	 * @throws MalformedQueryException
	 * @throws QueryEvaluationException
	 * @throws RepositoryException
	 * @throws UpdateExecutionException
	 */
	private void evaluateQuery(QueryLanguage queryLn, ParsedOperation query, Path path)
			throws MalformedQueryException, QueryEvaluationException, UpdateExecutionException {

		String queryString = query.getSourceString();

		try (OutputStream os = getOutputStream(path)) {
			if (query instanceof ParsedTupleQuery) {
				QueryResultWriter writer = getQueryResultWriter(path, os);
				evaluator.evaluateTupleQuery(queryLn, queryString, writer);
			} else if (query instanceof ParsedBooleanQuery) {
				QueryResultWriter writer = getQueryResultWriter(path, os);
				evaluator.evaluateBooleanQuery(queryLn, queryString, writer);
			} else if (query instanceof ParsedGraphQuery) {
				RDFWriter writer = getRDFWriter(path, os);
				evaluator.evaluateGraphQuery(queryLn, queryString, writer,
						getShowPrefix() ? getPrefixes() : Collections.emptySet());
			} else if (query instanceof ParsedUpdate) {
				// no outputstream for updates, can only be console output
				if (path != null) {
					throw new IllegalArgumentException("Update query does not produce output");
				}
				evaluator.executeUpdate(queryLn, queryString);
			} else {
				writeError("Unexpected query type");
			}
		} catch (IllegalArgumentException | IOException ioe) {
			writeError(ioe.getMessage());
		}
	}

	/**
	 * Add namespace prefixes to SPARQL query
	 *
	 * @param queryString query string
	 * @return query string with prefixes
	 */
	private String addQueryPrefixes(String queryString) {
		StringBuffer result = new StringBuffer(queryString.length() + 512);
		result.append(queryString);

		String upperCaseQuery = queryString.toUpperCase(Locale.ENGLISH);

		if (getQueryPrefix() && !hasQueryPrefixes(upperCaseQuery)) {
			addQueryPrefixes(result, getPrefixes());
		}
		return result.toString();
	}
}
