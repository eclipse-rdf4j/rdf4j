/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import ch.qos.logback.classic.Logger;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.Utils;
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
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@State(Scope.Benchmark)
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=15s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ParallelBenchmark {

	private List<List<Statement>> allStatements;

	@Setup(Level.Iteration)
	public void setUp() {
		Logger root = (Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName());
		root.setLevel(ch.qos.logback.classic.Level.INFO);

		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		allStatements = BenchmarkConfigs.generateStatements(((statements, i, j) -> {
			IRI iri = vf.createIRI("http://example.com/" + i + "_" + j);
			statements.add(vf.createStatement(iri, RDF.TYPE, RDFS.RESOURCE));
			statements.add(vf.createStatement(iri, FOAF.AGE, vf.createLiteral(i)));
		}));

		System.gc();

	}

	@Benchmark
	public void shaclSnapshot() throws Exception {
		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("shaclDatatype.ttl"));

		runBenchmark(IsolationLevels.SNAPSHOT, repository, true);
	}

	@Benchmark
	public void shaclSnapshotWithoutSerializableValidation() throws Exception {
		SailRepository repository = Utils.getInitializedShaclRepository("shaclDatatype.ttl", false);
		((ShaclSail) repository.getSail()).setSerializableValidation(false);

		runBenchmark(IsolationLevels.SNAPSHOT, repository, true);
	}

	@Benchmark
	public void shaclSerializable() throws Exception {
		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("shaclDatatype.ttl"));

		runBenchmark(IsolationLevels.SERIALIZABLE, repository, true);
	}

	@Benchmark
	public void shaclSerializableNotParallel() throws Exception {
		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("shaclDatatype.ttl"));

		runBenchmark(IsolationLevels.SERIALIZABLE, repository, false);
	}

	@Benchmark
	public void nativeStoreShaclSerializable() throws Exception {
		File file = Files.newTemporaryFolder();

		try {
			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSail(new NativeStore(file, "spoc,ospc,psoc"), "shaclDatatype.ttl"));
			runBenchmark(IsolationLevels.SERIALIZABLE, repository, true);

		} finally {
			FileUtils.deleteDirectory(file);
		}

	}

	@Benchmark
	public void nativeStoreShaclSerializableNotParallel() throws Exception {
		File file = Files.newTemporaryFolder();

		try {
			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSail(new NativeStore(file, "spoc,ospc,psoc"), "shaclDatatype.ttl"));
			runBenchmark(IsolationLevels.SERIALIZABLE, repository, false);

		} finally {
			FileUtils.deleteDirectory(file);
		}

	}

	@Benchmark
	public void nativeStoreShaclSnapshot() throws Exception {
		File file = Files.newTemporaryFolder();

		try {
			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSail(new NativeStore(file, "spoc,ospc,psoc"), "shaclDatatype.ttl"));
			runBenchmark(IsolationLevels.SNAPSHOT, repository, true);

		} finally {
			FileUtils.deleteDirectory(file);
		}

	}

	@Benchmark
	public void nativeStoreShaclSnapshotWithoutSerializableValidation() throws Exception {
		File file = Files.newTemporaryFolder();

		try {
			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSail(new NativeStore(file, "spoc,ospc,psoc"), "shaclDatatype.ttl"));
			((ShaclSail) repository.getSail()).setSerializableValidation(false);

			runBenchmark(IsolationLevels.SNAPSHOT, repository, true);

		} finally {
			FileUtils.deleteDirectory(file);
		}

	}

	private void runBenchmark(IsolationLevels isolationLevel, SailRepository repository, boolean parallel) {
		Stream<List<Statement>> listStream;
		if (parallel) {
			listStream = allStatements.parallelStream();
		} else {
			listStream = allStatements.stream();
		}

		listStream.forEach(statements -> {
			boolean success = false;
			while (!success) {
				try (SailRepositoryConnection connection = repository.getConnection()) {
					connection.begin(isolationLevel);
					connection.add(statements);
					try {
						connection.commit();
						success = true;
					} catch (RepositoryException ignored) {

					}
				}
			}
		});

		repository.shutDown();
	}

}
