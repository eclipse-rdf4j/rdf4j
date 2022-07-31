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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.federated.SPARQLBaseTest;
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

public class ResultGenerator {

	protected SailRepository repo;
	protected RepositoryConnection conn;

	public void run() throws Exception {

		String basePackage = "/tests/performance/";

		initStore(basePackage);

		createResult(basePackage, "query01");
		createResult(basePackage, "query02");
		createResult(basePackage, "query03");
		createResult(basePackage, "query04");
		createResult(basePackage, "query05");
		createResult(basePackage, "query06");
		createResult(basePackage, "query07");
		createResult(basePackage, "query08");
		createResult(basePackage, "query09");
		createResult(basePackage, "query10");
		createResult(basePackage, "query11");
		createResult(basePackage, "query12");

	}

	/**
	 * Create the result files for queryFile (without extensions)
	 *
	 * Resources are located on classpath.
	 *
	 * e.g. createResult("/tests/medium/", "query01");
	 *
	 * @param queryFile
	 */
	protected void createResult(String baseDir, String queryFile) throws Exception {

		String q = readQueryString(baseDir + queryFile + ".rq");

		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, q);
		TupleQueryResult res = query.evaluate();

		OutputStream out = new FileOutputStream(new File("test" + baseDir, queryFile + ".srx"));
		TupleQueryResultWriter qrWriter = new SPARQLResultsXMLWriter(out);
		QueryResults.report(res, qrWriter);
		out.close();
	}

	protected void printResult(String baseDir, String queryFile) throws Exception {

		String q = readQueryString(baseDir + queryFile + ".rq");

		TupleQuery query = conn.prepareTupleQuery(QueryLanguage.SPARQL, q);
		TupleQueryResult res = query.evaluate();

		while (res.hasNext()) {
			System.out.println(res.next());
		}
	}

	protected void initStore(String basePackage) throws Exception {

		MemoryStore mem = new MemoryStore();
		this.repo = new SailRepository(mem);
		repo.init();

		conn = repo.getConnection();

		String baseUri = "http://namespace.org";

		conn.add(ResultGenerator.class.getResourceAsStream(basePackage + "data1.ttl"), baseUri, RDFFormat.TURTLE);
		conn.add(ResultGenerator.class.getResourceAsStream(basePackage + "data2.ttl"), baseUri, RDFFormat.TURTLE);
		conn.add(ResultGenerator.class.getResourceAsStream(basePackage + "data3.ttl"), baseUri, RDFFormat.TURTLE);
		conn.add(ResultGenerator.class.getResourceAsStream(basePackage + "data4.ttl"), baseUri, RDFFormat.TURTLE);

	}

	/**
	 * Read the query string from the specified resource
	 *
	 * @param queryResource
	 * @return
	 * @throws RepositoryException
	 * @throws IOException
	 */
	private String readQueryString(String queryFile) throws RepositoryException, IOException {
		try (InputStream stream = SPARQLBaseTest.class.getResourceAsStream(queryFile)) {
			return IOUtil.readString(new InputStreamReader(Objects.requireNonNull(stream), StandardCharsets.UTF_8));
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		new ResultGenerator().run();

	}

}
