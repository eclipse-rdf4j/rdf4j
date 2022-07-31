/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.Literal;
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

@State(Scope.Benchmark)
@BenchmarkMode({ Mode.AverageTime })
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G" })
public class ReasoningBenchmark {

	private int expectedCount;

	@Param({ "moreRdfs::12180", "longChain::5803", "medium::544", "simple::152" })
	public String param;

	@Benchmark
	public void noReasoning() throws IOException {
		SailRepository sail = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = sail.getConnection()) {
			connection.begin();

			connection.add(resourceAsStream("schema.ttl"), "", RDFFormat.TURTLE);
			addAllDataSingleTransaction(connection);

			connection.commit();
		}
	}

	@Benchmark
	public void noReasoningMultipleTransactions() throws IOException {
		SailRepository sail = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = sail.getConnection()) {

			connection.begin();
			connection.add(resourceAsStream("schema.ttl"), "", RDFFormat.TURTLE);
			connection.commit();

			addAllDataMultipleTransactions(connection);

		}
	}

	@Benchmark
	public void forwardChainingSchemaCachingRDFSInferencer() throws IOException {
		SailRepository sail = new SailRepository(new SchemaCachingRDFSInferencer(new MemoryStore()));

		try (SailRepositoryConnection connection = sail.getConnection()) {
			connection.begin();

			connection.add(resourceAsStream("schema.ttl"), "", RDFFormat.TURTLE);
			addAllDataSingleTransaction(connection);

			connection.commit();
		}

		checkSize(sail);
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

	@Benchmark
	public void forwardChainingSchemaCachingRDFSInferencerMultipleTransactions() throws IOException {
		SailRepository sail = new SailRepository(new SchemaCachingRDFSInferencer(new MemoryStore()));

		try (SailRepositoryConnection connection = sail.getConnection()) {

			connection.begin();
			connection.add(resourceAsStream("schema.ttl"), "", RDFFormat.TURTLE);
			connection.commit();

			addAllDataMultipleTransactions(connection);

		}
		checkSize(sail);

	}

	@Benchmark
	public void forwardChainingSchemaCachingRDFSInferencerSchema() throws IOException {
		SailRepository sail = new SailRepository(new SchemaCachingRDFSInferencer(new MemoryStore(), createSchema()));

		try (SailRepositoryConnection connection = sail.getConnection()) {
			connection.begin();
			addAllDataSingleTransaction(connection);
			connection.commit();
		}
		checkSize(sail);

	}

	@Benchmark
	public void forwardChainingSchemaCachingRDFSInferencerMultipleTransactionsSchema() throws IOException {
		SailRepository sail = new SailRepository(new SchemaCachingRDFSInferencer(new MemoryStore(), createSchema()));

		try (SailRepositoryConnection connection = sail.getConnection()) {
			addAllDataMultipleTransactions(connection);
		}
		checkSize(sail);

	}

	private SailRepository createSchema() throws IOException {
		SailRepository schema = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection schemaConnection = schema.getConnection()) {
			schemaConnection.begin();
			schemaConnection.add(resourceAsStream("schema.ttl"), "", RDFFormat.TURTLE);
			schemaConnection.commit();
		}
		return schema;
	}

	private void addAllDataSingleTransaction(SailRepositoryConnection connection) throws IOException {
		InputStream data = resourceAsStream("data.ttl");

		if (data != null) {
			connection.add(data, "", RDFFormat.TURTLE);
		}

		int counter = 0;
		while (true) {
			data = resourceAsStream("data" + counter++ + ".ttl");
			if (data == null) {
				break;
			}
			connection.add(data, "", RDFFormat.TURTLE);
		}
	}

	private void addAllDataMultipleTransactions(SailRepositoryConnection connection) throws IOException {
		InputStream data = resourceAsStream("data.ttl");

		if (data != null) {
			connection.begin();
			connection.add(data, "", RDFFormat.TURTLE);
			connection.commit();
		}

		int counter = 0;
		while (true) {
			data = resourceAsStream("data" + counter++ + ".ttl");
			if (data == null) {
				break;
			}
			connection.begin();
			connection.add(data, "", RDFFormat.TURTLE);
			connection.commit();
		}
	}

	private InputStream resourceAsStream(String resourceName) {
		String[] split = param.split("::");

		this.expectedCount = Integer.parseInt(split[1]);
		return ReasoningBenchmark.class.getClassLoader().getResourceAsStream(split[0] + "/" + resourceName);

	}

}
