/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.benchmark;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * @author Håvard Mikkelsen Ottestad
 */

@Measurement(iterations = 10)
@Warmup(iterations = 20)
@Fork(1)
@State(Scope.Thread)
public class ReasoningUpdateBenchmark {

	private int expectedCount;

	@Param({ "moreRdfs::12180" })
	public String param;

	static private final IRI schemaGraph = SimpleValueFactory.getInstance().createIRI("http://example.org/schemaGraph");

	@Benchmark
	@BenchmarkMode(Mode.AverageTime)
	@OutputTimeUnit(TimeUnit.MILLISECONDS)
	public void forwardChainingSchemaCachingRDFSInferencer() throws IOException {
		SailRepository sail = new SailRepository(new SchemaCachingRDFSInferencer(new MemoryStore()));
		try {
			try (SailRepositoryConnection connection = sail.getConnection()) {
				connection.begin();
				addRequiredResource(connection, "schema.ttl");
				connection.commit();

				addAllDataMultipleTransactions(connection);
			}
			checkSize(sail);
		} finally {
			sail.shutDown();
		}
	}

	private void checkSize(SailRepository sail) {

		assert getSize(sail) == expectedCount : "Was " + getSize(sail) + " but expected " + expectedCount;

	}

	private int getSize(SailRepository sail) {
		try (SailRepositoryConnection connection = sail.getConnection()) {
			try (TupleQueryResult evaluate = connection
					.prepareTupleQuery("select (count (*) as ?count) where {?a ?b ?c}")
					.evaluate()) {
				return ((Literal) evaluate.next().getBinding("count").getValue()).intValue();

			}
		}
	}

	private void addAllDataMultipleTransactions(SailRepositoryConnection connection) throws IOException {
		for (int i = 0; i <= 9; i++) {

			// remove a random statement
			connection.begin();
			try (Stream<Statement> stream = connection.getStatements(null, null, null).stream()) {
				stream.findFirst().ifPresent(connection::remove);
			}
			connection.commit();

			try (InputStream data = resourceAsStream("data" + i++ + ".ttl")) {
				if (data != null) {
					connection.begin();
					connection.add(data, "", RDFFormat.TURTLE);
					connection.commit();
				}
			}
		}
	}

	Map<String, String> cache = new HashMap<>();

	private InputStream resourceAsStream(String resourceName) {
		String[] split = param.split("\\:\\:");
		this.expectedCount = Integer.parseInt(split[1]);
		String filename = split[0] + "/" + resourceName;
		String content = cache.computeIfAbsent(filename, (fn) -> {
			try (InputStream stream = ReasoningUpdateBenchmark.class.getClassLoader().getResourceAsStream(fn)) {
				if (stream == null) {
					throw new IllegalStateException("Resource not found: " + fn);
				}
				return IOUtils.toString(stream, StandardCharsets.UTF_8);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

	}

	private void addRequiredResource(SailRepositoryConnection connection, String resourceName) throws IOException {
		try (InputStream schemaStream = resourceAsStream(resourceName)) {
			if (schemaStream == null) {
				throw new IOException("Resource not found: " + resourceName);
			}
			connection.add(schemaStream, "", RDFFormat.TURTLE, schemaGraph);
		}
	}

}
