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
		try {
			try (SailRepositoryConnection connection = sail.getConnection()) {
				connection.begin();

				addRequiredResource(connection, "schema.ttl");
				addAllDataSingleTransaction(connection);

				connection.commit();
			}
		} finally {
			sail.shutDown();
		}
	}

	@Benchmark
	public void noReasoningMultipleTransactions() throws IOException {
		SailRepository sail = new SailRepository(new MemoryStore());
		try {
			try (SailRepositoryConnection connection = sail.getConnection()) {

				connection.begin();
				addRequiredResource(connection, "schema.ttl");
				connection.commit();

				addAllDataMultipleTransactions(connection);

			}
		} finally {
			sail.shutDown();
		}
	}

	@Benchmark
	public void forwardChainingSchemaCachingRDFSInferencer() throws IOException {
		SailRepository sail = new SailRepository(new SchemaCachingRDFSInferencer(new MemoryStore()));
		try {
			try (SailRepositoryConnection connection = sail.getConnection()) {
				connection.begin();

				addRequiredResource(connection, "schema.ttl");
				addAllDataSingleTransaction(connection);

				connection.commit();
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

	@Benchmark
	public void forwardChainingSchemaCachingRDFSInferencerMultipleTransactions() throws IOException {
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

	@Benchmark
	public void forwardChainingSchemaCachingRDFSInferencerSchema() throws IOException {
		SailRepository schema = createSchema();
		SailRepository sail = new SailRepository(new SchemaCachingRDFSInferencer(new MemoryStore(), schema));
		try {
			try (SailRepositoryConnection connection = sail.getConnection()) {
				connection.begin();
				addAllDataSingleTransaction(connection);
				connection.commit();
			}
			checkSize(sail);
		} finally {
			try {
				sail.shutDown();
			} finally {
				schema.shutDown();
			}
		}
	}

	@Benchmark
	public void forwardChainingSchemaCachingRDFSInferencerMultipleTransactionsSchema() throws IOException {
		SailRepository schema = createSchema();
		SailRepository sail = new SailRepository(new SchemaCachingRDFSInferencer(new MemoryStore(), schema));
		try {
			try (SailRepositoryConnection connection = sail.getConnection()) {
				addAllDataMultipleTransactions(connection);
			}
			checkSize(sail);
		} finally {
			try {
				sail.shutDown();
			} finally {
				schema.shutDown();
			}
		}
	}

	private SailRepository createSchema() throws IOException {
		SailRepository schema = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection schemaConnection = schema.getConnection()) {
			schemaConnection.begin();
			addRequiredResource(schemaConnection, "schema.ttl");
			schemaConnection.commit();
		}
		return schema;
	}

	private void addAllDataSingleTransaction(SailRepositoryConnection connection) throws IOException {
		addOptionalResource(connection, "data.ttl");

		int counter = 0;
		while (true) {
			if (!addOptionalResource(connection, "data" + counter++ + ".ttl")) {
				break;
			}
		}
	}

	private void addAllDataMultipleTransactions(SailRepositoryConnection connection) throws IOException {
		addOptionalResourceInTransaction(connection, "data.ttl");

		int counter = 0;
		while (true) {
			if (!addOptionalResourceInTransaction(connection, "data" + counter++ + ".ttl")) {
				break;
			}
		}
	}

	private void addRequiredResource(SailRepositoryConnection connection, String resourceName) throws IOException {
		try (InputStream data = resourceAsStream(resourceName)) {
			if (data == null) {
				throw new IOException("Resource not found: " + resourceName);
			}
			connection.add(data, "", RDFFormat.TURTLE);
		}
	}

	private boolean addOptionalResource(SailRepositoryConnection connection, String resourceName) throws IOException {
		try (InputStream data = resourceAsStream(resourceName)) {
			if (data == null) {
				return false;
			}
			connection.add(data, "", RDFFormat.TURTLE);
			return true;
		}
	}

	private boolean addOptionalResourceInTransaction(SailRepositoryConnection connection, String resourceName)
			throws IOException {
		try (InputStream data = resourceAsStream(resourceName)) {
			if (data == null) {
				return false;
			}
			connection.begin();
			connection.add(data, "", RDFFormat.TURTLE);
			connection.commit();
			return true;
		}
	}

	private InputStream resourceAsStream(String resourceName) {
		String[] split = param.split("::");

		this.expectedCount = Integer.parseInt(split[1]);
		return ReasoningBenchmark.class.getClassLoader().getResourceAsStream(split[0] + "/" + resourceName);

	}

}
