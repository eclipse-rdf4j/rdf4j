/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Files;
import org.eclipse.rdf4j.common.concurrent.locks.Properties;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
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
import org.openjdk.jmh.infra.Blackhole;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G",   "-XX:StartFlightRecording=delay=15s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ParallelBenchmark {

	private List<List<Statement>> allStatements;

	@Setup(Level.Trial)
	public void setUp() throws InterruptedException {
		((Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName()))
				.setLevel(ch.qos.logback.classic.Level.ERROR);
		((Logger) LoggerFactory.getLogger(ShaclSail.class.getName()))
				.setLevel(ch.qos.logback.classic.Level.WARN);

		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		allStatements = BenchmarkConfigs.generateStatements(((statements, i, j) -> {
			IRI iri = vf.createIRI("http://example.com/" + i + "_" + j);
			statements.add(vf.createStatement(iri, RDF.TYPE, RDFS.RESOURCE));
			statements.add(vf.createStatement(iri, FOAF.AGE, vf.createLiteral(i)));
		}));

		System.gc();
		Thread.sleep(100);
	}

	@Benchmark
	public void shaclSnapshot(Blackhole blackhole) throws Exception {
		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("shaclDatatype.trig"));

		runBenchmark(IsolationLevels.SNAPSHOT, repository, true, false, blackhole);
	}

	@Benchmark
	public void shaclSnapshotMixedReadWrite(Blackhole blackhole) throws Exception {
		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("shaclDatatype.trig"));

		runBenchmark(IsolationLevels.SNAPSHOT, repository, true, true, blackhole);
	}

	@Benchmark
	public void shaclSnapshotWithoutSerializableValidation(Blackhole blackhole) throws Exception {
		SailRepository repository = Utils.getInitializedShaclRepository("shaclDatatype.trig");
		((ShaclSail) repository.getSail()).setSerializableValidation(false);

		runBenchmark(IsolationLevels.SNAPSHOT, repository, true, false, blackhole);
	}

	@Benchmark
	public void shaclSnapshotWithoutSerializableValidationMixedReadWrite(Blackhole blackhole) throws Exception {
		SailRepository repository = Utils.getInitializedShaclRepository("shaclDatatype.trig");
		((ShaclSail) repository.getSail()).setSerializableValidation(false);

		runBenchmark(IsolationLevels.SNAPSHOT, repository, true, true, blackhole);
	}

	@Benchmark
	public void shaclSerializable(Blackhole blackhole) throws Exception {
		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("shaclDatatype.trig"));

		runBenchmark(IsolationLevels.SERIALIZABLE, repository, true, false, blackhole);
	}

	@Benchmark
	public void shaclSerializableMixedReadWrite(Blackhole blackhole) throws Exception {
		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("shaclDatatype.trig"));

		runBenchmark(IsolationLevels.SERIALIZABLE, repository, true, true, blackhole);
	}

	@Benchmark
	public void shaclSerializableNotParallel(Blackhole blackhole) throws Exception {
		SailRepository repository = new SailRepository(Utils.getInitializedShaclSail("shaclDatatype.trig"));

		runBenchmark(IsolationLevels.SERIALIZABLE, repository, false, false, blackhole);
	}

	@Benchmark
	public void nativeStoreShaclSerializable(Blackhole blackhole) throws Exception {
		File file = Files.newTemporaryFolder();

		try {
			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSail(new NativeStore(file, "spoc,ospc,psoc"), "shaclDatatype.trig"));
			runBenchmark(IsolationLevels.SERIALIZABLE, repository, true, false, blackhole);

		} finally {
			FileUtils.deleteDirectory(file);
		}

	}

	@Benchmark
	public void nativeStoreShaclSerializableNotParallel(Blackhole blackhole) throws Exception {
		File file = Files.newTemporaryFolder();

		try {
			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSail(new NativeStore(file, "spoc,ospc,psoc"), "shaclDatatype.trig"));
			runBenchmark(IsolationLevels.SERIALIZABLE, repository, false, false, blackhole);

		} finally {
			FileUtils.deleteDirectory(file);
		}

	}

	public static void main(String[] args) throws Exception {
		ParallelBenchmark parallelBenchmark = new ParallelBenchmark();
		parallelBenchmark.setUp();
		while (true) {
			System.out.println("Here: " + System.currentTimeMillis());
			parallelBenchmark.nativeStoreShaclSnapshot(new Blackhole(
					"Today's password is swordfish. I understand instantiating Blackholes directly is dangerous."));
		}
	}

	@Benchmark
	public void nativeStoreShaclSnapshot(Blackhole blackhole) throws Exception {
		File file = Files.newTemporaryFolder();

		try {
			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSail(new NativeStore(file, "spoc,ospc,psoc"), "shaclDatatype.trig"));
			runBenchmark(IsolationLevels.SNAPSHOT, repository, true, false, blackhole);

		} finally {
			FileUtils.deleteDirectory(file);
		}

	}

	@Benchmark
	public void nativeStoreShaclSnapshotWithoutSerializableValidation(Blackhole blackhole) throws Exception {
		File file = Files.newTemporaryFolder();

		try {
			SailRepository repository = new SailRepository(
					Utils.getInitializedShaclSail(new NativeStore(file, "spoc,ospc,psoc"), "shaclDatatype.trig"));
			((ShaclSail) repository.getSail()).setSerializableValidation(false);

			runBenchmark(IsolationLevels.SNAPSHOT, repository, true, false, blackhole);

		} finally {
			FileUtils.deleteDirectory(file);
		}

	}

	private void runBenchmark(IsolationLevels isolationLevel, SailRepository repository, boolean parallel,
			boolean mixedReadWrite, Blackhole blackhole) {

		ArrayList<List<Statement>> allStatements = new ArrayList<>(this.allStatements);

		if (mixedReadWrite) {
			for (int i = 0; i < this.allStatements.size() * 10; i++) {
				allStatements.add(Collections.emptyList());
			}
		}

		Collections.shuffle(allStatements);

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
					if (!statements.isEmpty()) {
						connection.add(statements);
					} else {
						// read operation instead of write
						try (Stream<Statement> stream = connection.getStatements(null, RDF.TYPE, null, false)
								.stream()) {
							long count = stream.count();
							blackhole.consume(count);
						}
					}
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
