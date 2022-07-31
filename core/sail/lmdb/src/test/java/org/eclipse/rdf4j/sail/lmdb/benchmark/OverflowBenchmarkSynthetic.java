/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.lmdb.benchmark;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * Benchmarks transaction isolation and overflow performance with synthetic data.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 0)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms64M", "-Xmx64M", "-XX:+UseG1GC" })
@Measurement(iterations = 10, batchSize = 1, time = 1, timeUnit = TimeUnit.MILLISECONDS)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class OverflowBenchmarkSynthetic {

	private final Random random = new Random(389012849);
	private final String ns = "http://example.org/";

	@Setup(Level.Trial)
	public void setup() {
		((Logger) (LoggerFactory
				.getLogger("org.eclipse.rdf4j.sail.lmdbrdf.MemoryOverflowModel")))
						.setLevel(ch.qos.logback.classic.Level.DEBUG);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("OverflowBenchmarkSynthetic") // adapt to run other benchmark tests
				.build();

		new Runner(opt).run();
	}

	@Benchmark
	public long loadLotsOfDataEmptyStore() throws IOException {
		File temporaryFolder = Files.newTemporaryFolder();
		SailRepository sailRepository = null;
		try {
			sailRepository = new SailRepository(new LmdbStore(temporaryFolder, ConfigUtil.createConfig()));

			try (SailRepositoryConnection connection = sailRepository.getConnection()) {

				connection.begin();
				addData(connection, 4000);
				connection.commit();

				return connection.size();
			}

		} finally {
			try {
				if (sailRepository != null) {
					sailRepository.shutDown();
				}
			} finally {
				FileUtils.deleteDirectory(temporaryFolder);
			}
		}

	}

	@Benchmark
	public long loadLotsOfDataNonEmptyStore() throws IOException {
		File temporaryFolder = Files.newTemporaryFolder();
		SailRepository sailRepository = null;
		try {
			sailRepository = new SailRepository(new LmdbStore(temporaryFolder));

			try (SailRepositoryConnection connection = sailRepository.getConnection()) {

				connection.begin();
				addData(connection, 1000);
				connection.commit();

				connection.begin(IsolationLevels.READ_COMMITTED);
				addData(connection, 4000);
				connection.commit();

				return connection.size();
			}

		} finally {
			try {
				if (sailRepository != null) {
					sailRepository.shutDown();
				}
			} finally {
				FileUtils.deleteDirectory(temporaryFolder);
			}
		}

	}

	private void addData(SailRepositoryConnection connection, int upperLimit) {
		ValueFactory vf = connection.getValueFactory();

		IntStream
				.range(0, upperLimit)
				.mapToObj(String::valueOf)
				.flatMap(i -> Stream.of(
						vf.createStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral(i)),
						vf.createStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral(i)),
						vf.createStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral(i)),
						vf.createStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral(i)),
						vf.createStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral(i)),
						vf.createStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral(i)),
						vf.createStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral(i)),
						vf.createStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral(i)),
						vf.createStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral(i)),
						vf.createStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral(i)),
						vf.createStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral(i)),
						vf.createStatement(vf.createIRI(ns, random.nextInt(upperLimit) + ""), FOAF.KNOWS,
								vf.createIRI(ns, random.nextInt(upperLimit) + "")),
						vf.createStatement(vf.createIRI(ns, random.nextInt(upperLimit) + ""), FOAF.KNOWS,
								vf.createIRI(ns, random.nextInt(upperLimit) + "")),
						vf.createStatement(vf.createIRI(ns, random.nextInt(upperLimit) + ""), FOAF.KNOWS,
								vf.createIRI(ns, random.nextInt(upperLimit) + "")),
						vf.createStatement(vf.createIRI(ns, random.nextInt(upperLimit) + ""), FOAF.KNOWS,
								vf.createIRI(ns, random.nextInt(upperLimit) + "")),
						vf.createStatement(vf.createIRI(ns, random.nextInt(upperLimit) + ""), FOAF.KNOWS,
								vf.createIRI(ns, random.nextInt(upperLimit) + "")),
						vf.createStatement(vf.createIRI(ns, random.nextInt(upperLimit) + ""), FOAF.KNOWS,
								vf.createIRI(ns, random.nextInt(upperLimit) + "")),
						vf.createStatement(vf.createIRI(ns, random.nextInt(upperLimit) + ""), FOAF.KNOWS,
								vf.createIRI(ns, random.nextInt(upperLimit) + "")),
						vf.createStatement(vf.createIRI(ns, random.nextInt(upperLimit) + ""), FOAF.KNOWS,
								vf.createIRI(ns, random.nextInt(upperLimit) + "")),
						vf.createStatement(vf.createIRI(ns, random.nextInt(upperLimit) + ""), FOAF.KNOWS,
								vf.createIRI(ns, random.nextInt(upperLimit) + "")),
						vf.createStatement(vf.createIRI(ns, random.nextInt(upperLimit) + ""), FOAF.KNOWS,
								vf.createIRI(ns, random.nextInt(upperLimit) + "")),
						vf.createStatement(vf.createIRI(ns, random.nextInt(upperLimit) + ""), FOAF.KNOWS,
								vf.createIRI(ns, random.nextInt(upperLimit) + ""))
				)
				)
				.forEach(connection::add);
	}

}
