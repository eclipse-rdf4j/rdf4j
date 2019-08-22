/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.spin.benchmarks;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.inferencer.fc.DedupingInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.ForwardChainingRDFSInferencer;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.spin.SpinSail;
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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC"})
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures",
		"-XX:StartFlightRecording=delay=5s,duration=30s,filename=recording.jfr,settings=profile",
		"-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions",
		"-XX:+DebugNonSafepoints" })
@Measurement(iterations = 20)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class BasicBenchmarks {

	private static final SimpleValueFactory vf = SimpleValueFactory.getInstance();

	private static final Resource bob = vf.createBNode();
	private static final Resource alice = vf.createBNode();

	private static final IRI name = FOAF.NAME;

	private static final Value nameAlice = vf.createLiteral("Alice");
	private static final Value nameBob = vf.createLiteral("Bob");

	@Setup(Level.Invocation)
	public void setUp() {
		System.gc();

	}

	@TearDown(Level.Iteration)
	public void tearDown() {
	}

	@Benchmark
	public void spinSailInit() {

		SpinSail spinSail = new SpinSail(new MemoryStore());
		spinSail.initialize();
	}

	@Benchmark
	public void memoryStoreInit() {

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();
	}

	@Benchmark
	public void cantInvoke() throws IOException {
		NotifyingSail baseSail = new MemoryStore();
		DedupingInferencer deduper = new DedupingInferencer(baseSail);
		NotifyingSail rdfsInferencer = new SchemaCachingRDFSInferencer(deduper);
		SpinSail spinSail = new SpinSail(rdfsInferencer);
		SailRepository repo = new SailRepository(spinSail);
		repo.initialize();
		try (SailRepositoryConnection conn = repo.getConnection()) {

			loadRDF("/schema/spif.ttl", conn);
			BooleanQuery bq = conn.prepareBooleanQuery(QueryLanguage.SPARQL,
					"prefix spif: <http://spinrdf.org/spif#> "
							+ "ask where {filter(spif:canInvoke(spif:indexOf, 'foobar', 2))}");
			assertFalse(bq.evaluate());

		}
	}

	@Benchmark
	public void cantInvokeOldReasoner() throws IOException {
		NotifyingSail baseSail = new MemoryStore();
		DedupingInferencer deduper = new DedupingInferencer(baseSail);
		NotifyingSail rdfsInferencer = new ForwardChainingRDFSInferencer(deduper);
		SpinSail spinSail = new SpinSail(rdfsInferencer);
		SailRepository repo = new SailRepository(spinSail);
		repo.initialize();
		try (SailRepositoryConnection conn = repo.getConnection()) {

			loadRDF("/schema/spif.ttl", conn);
			BooleanQuery bq = conn.prepareBooleanQuery(QueryLanguage.SPARQL,
					"prefix spif: <http://spinrdf.org/spif#> "
							+ "ask where {filter(spif:canInvoke(spif:indexOf, 'foobar', 2))}");
			assertFalse(bq.evaluate());

		}
	}

	private void loadRDF(String path, SailRepositoryConnection conn)
			throws IOException {
		URL url = getClass().getResource(path);
		try (InputStream in = url.openStream()) {
			conn.add(in, url.toString(), RDFFormat.TURTLE);
		}
	}

	@Benchmark
	public void addRemove() {
		SailRepository spinSail = new SailRepository(new SpinSail(new MemoryStore()));
		spinSail.init();

		try (SailRepositoryConnection connection = spinSail.getConnection()) {
			connection.begin();
			connection.add(bob, name, nameBob);
			connection.add(alice, name, nameAlice);
			connection.commit();

			connection.remove(bob, name, nameBob);

			connection.remove(alice, null, null);
		}

	}

	@Benchmark
	public void remove() {
		SailRepository spinSail = new SailRepository(new SpinSail(new MemoryStore()));
		spinSail.init();

		try (SailRepositoryConnection connection = spinSail.getConnection()) {
			connection.remove(bob, name, nameBob);
			connection.remove(alice, null, null);
		}

	}

	@Benchmark
	public void addRemoveSingleTransaction() {
		SailRepository spinSail = new SailRepository(new SpinSail(new MemoryStore()));
		spinSail.init();

		try (SailRepositoryConnection connection = spinSail.getConnection()) {
			connection.begin();
			connection.add(bob, name, nameBob);
			connection.add(alice, name, nameAlice);

			connection.remove(bob, name, nameBob);

			connection.remove(alice, null, null);
			connection.commit();

		}

	}

	@Benchmark
	public void addRemoveTwoTransactions() {
		SailRepository spinSail = new SailRepository(new SpinSail(new MemoryStore()));
		spinSail.init();

		try (SailRepositoryConnection connection = spinSail.getConnection()) {
			connection.begin();
			connection.add(bob, name, nameBob);
			connection.add(alice, name, nameAlice);

			connection.commit();
			connection.begin();

			connection.remove(bob, name, nameBob);

			connection.remove(alice, null, null);
			connection.commit();

		}

	}

	@Benchmark
	public void addRemoveAll() {
		SailRepository spinSail = new SailRepository(new SpinSail(new MemoryStore()));
		spinSail.init();

		try (SailRepositoryConnection connection = spinSail.getConnection()) {
			connection.begin();
			connection.add(bob, name, nameBob);
			connection.add(alice, name, nameAlice);

			connection.commit();

			connection.remove((Resource) null, null, null);

		}

	}

}
