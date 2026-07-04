/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * Generates the expected SPARQL query result files ({@code .srx}) for the 4-endpoint federation benchmark.
 *
 * <p>
 * The generator loads the four Turtle data files produced by {@link DataGenerator} into a local in-memory RDF store and
 * evaluates each benchmark query against the complete, merged dataset. The results are serialized in SPARQL Results XML
 * format ({@code .srx}) and written alongside the query files, providing the ground-truth answers used by the benchmark
 * harness.
 * </p>
 *
 * <p>
 * Run via {@link #main(String[])} to (re-)generate all result files for the default performance test directory, or call
 * {@link #run(File)} directly to target a custom location.
 * </p>
 *
 * @author andreas_s
 */
public class ResultGenerator {

	protected SailRepository repo;
	protected RepositoryConnection conn;

	/**
	 * Generates result files for all benchmark queries in {@code src/test/resources/tests/performance/}.
	 *
	 * @throws Exception if the store cannot be initialised or a query fails
	 */
	public void run() throws Exception {
		String testResources = "src/test/resources/";

		File baseFolder = new File(testResources + "tests/performance/");
		run(baseFolder);
	}

	/**
	 * Generates result files for all benchmark queries found in {@code baseFolder}.
	 *
	 * <p>
	 * Initialises an in-memory store from the four data files in {@code baseFolder}, then evaluates
	 * {@code query01..query12} and writes the corresponding {@code .srx} result files.
	 * </p>
	 *
	 * @param baseFolder directory containing the data files and query files; result files are written here too
	 * @throws Exception if the store cannot be initialised or a query fails
	 */
	public void run(File baseFolder) throws Exception {

		try {
			initStore(baseFolder);

			createResult(baseFolder, "query01");
			createResult(baseFolder, "query02");
			createResult(baseFolder, "query03");
			createResult(baseFolder, "query04");
			createResult(baseFolder, "query05");
			createResult(baseFolder, "query06");
			createResult(baseFolder, "query07");
			createResult(baseFolder, "query08");
			createResult(baseFolder, "query09");
			createResult(baseFolder, "query10");
			createResult(baseFolder, "query11");
			createResult(baseFolder, "query12");
		} finally {
			if (conn != null) {
				conn.close();
			}
			if (repo != null) {
				repo.shutDown();
			}
		}

	}

	/**
	 * Evaluates a single SPARQL query against the in-memory store and writes the result to a {@code .srx} file.
	 *
	 * <p>
	 * The query is read from {@code <baseDir>/<queryFile>.rq} and the result is written to
	 * {@code <baseDir>/<queryFile>.srx} in SPARQL Results XML format.
	 * </p>
	 *
	 * @param baseDir   directory containing the {@code .rq} file and receiving the {@code .srx} file
	 * @param queryFile query file name without extension (e.g. {@code "query01"})
	 * @throws Exception if the query file cannot be read, the query fails, or the result cannot be written
	 */
	protected void createResult(File baseDir, String queryFile) throws Exception {

		String q = readQueryString(new File(baseDir, queryFile + ".rq"));

		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, q);
		TupleQueryResult res = query.evaluate();

		try (OutputStream out = new FileOutputStream(new File(baseDir, queryFile + ".srx"))) {
			TupleQueryResultWriter qrWriter = new SPARQLResultsXMLWriter(out);
			QueryResults.report(res, qrWriter);
		}
	}

	/**
	 * Evaluates a single SPARQL query and prints each result binding set to standard output.
	 *
	 * <p>
	 * Intended for ad-hoc debugging; no file is written.
	 * </p>
	 *
	 * @param baseDir   directory containing the {@code .rq} file
	 * @param queryFile query file name without extension (e.g. {@code "query01"})
	 * @throws Exception if the query file cannot be read or the query fails
	 */
	protected void printResult(String baseDir, String queryFile) throws Exception {

		String q = readQueryString(new File(baseDir, queryFile + ".rq"));

		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, q);
		TupleQueryResult res = query.evaluate();

		while (res.hasNext()) {
			System.out.println(res.next());
		}
	}

	/**
	 * Initialises the in-memory RDF store and loads the four benchmark data files.
	 *
	 * <p>
	 * Loads {@code data1.ttl}, {@code data2.ttl}, {@code data3.ttl}, and {@code data4.ttl} from {@code baseFolder} into
	 * a single {@link MemoryStore}, merging all four endpoints into one dataset for ground-truth evaluation.
	 * </p>
	 *
	 * @param baseFolder directory containing the four Turtle data files
	 * @throws Exception if the repository cannot be initialised or a data file cannot be loaded
	 */
	protected void initStore(File baseFolder) throws Exception {

		MemoryStore mem = new MemoryStore();
		this.repo = new SailRepository(mem);
		repo.init();

		conn = repo.getConnection();

		addFromFile(baseFolder, "data1.ttl");
		addFromFile(baseFolder, "data2.ttl");
		addFromFile(baseFolder, "data3.ttl");
		addFromFile(baseFolder, "data4.ttl");

	}

	/**
	 * Loads a single Turtle file into the repository connection.
	 *
	 * @param baseDir  directory containing the file
	 * @param fileName file name relative to {@code baseDir} (e.g. {@code "data1.ttl"})
	 * @throws IOException if the file cannot be read or parsed
	 */
	protected void addFromFile(File baseDir, String fileName) throws IOException {
		String baseUri = "http://namespace.org";

		try (var in = new FileInputStream(new File(baseDir, fileName))) {
			conn.add(in, baseUri, RDFFormat.TURTLE);
		}

	}

	/**
	 * Reads the contents of a SPARQL query file as a UTF-8 string.
	 *
	 * @param queryFile the {@code .rq} file to read
	 * @return the query string
	 * @throws RepositoryException if the stream cannot be wrapped
	 * @throws IOException         if the file cannot be read
	 */
	private String readQueryString(File queryFile) throws RepositoryException, IOException {
		try (InputStream stream = new FileInputStream(queryFile)) {
			return IOUtil.readString(new InputStreamReader(Objects.requireNonNull(stream), StandardCharsets.UTF_8));
		}
	}

	/**
	 * Entry point: generates all benchmark result files in {@code src/test/resources/tests/performance/}.
	 *
	 * @param args ignored
	 * @throws Exception if data loading or query evaluation fails
	 */
	public static void main(String[] args) throws Exception {
		new ResultGenerator().run();

	}
}
