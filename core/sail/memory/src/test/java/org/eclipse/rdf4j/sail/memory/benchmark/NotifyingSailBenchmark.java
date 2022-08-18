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
package org.eclipse.rdf4j.sail.memory.benchmark;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailConnectionListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailConnectionWrapper;
import org.eclipse.rdf4j.sail.helpers.NotifyingSailWrapper;
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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
//@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=5s,duration=60s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Fork(value = 1, jvmArgs = { "-Xms2G", "-Xmx2G" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class NotifyingSailBenchmark {

	@Param({ "NONE", "SNAPSHOT", "SERIALIZABLE" })
	public String isolationLevel;

	public static final int REAL_DATA_SIZE = 50000;

	private static final List<Statement> statementList = getStatements();
	private static final List<Statement> realData = getRealData();
	private static final List<Statement> realDataRandom = getRealDataRandom();
	private static final List<Statement> realDataSmall = getSmallList(realData);

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("NotifyingSailBenchmark.load") // adapt to run other benchmark tests
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Benchmark
	public long load() {
		long count = 0;

		Sail memoryStore = new TestNotifyingSail(new MemoryStore());
		memoryStore.init();

		try (SailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));
			statementList.forEach(getStatementConsumer(connection));

			connection.commit();
			count += getCount(connection);
		}
		try (SailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));
			realData.forEach(getStatementConsumer(connection));

			connection.commit();
		}

		try (SailConnection connection = memoryStore.getConnection()) {

			connection.begin(IsolationLevels.valueOf(isolationLevel));

			realDataSmall.forEach(getStatementConsumer(connection));

			ValueFactory vf = memoryStore.getValueFactory();
			connection.addStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral("label"));

			for (int i = 0; i < 10; i++) {
				count += getCount(connection);
			}

			connection.commit();

		}

		try (SailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.valueOf(isolationLevel));
			realDataRandom.forEach(getStatementConsumer(connection));

			connection.commit();
		}

		return count;

	}

	private long getCount(SailConnection connection) {
		long count;
		try (Stream<? extends Statement> stream = connection.getStatements(null, null, null, false).stream()) {
			count = stream.count();
		}
		return count;
	}

	private static List<Statement> getStatements() {
		Random random = new Random(43256523);

		ValueFactory vf = SimpleValueFactory.getInstance();

		List<Statement> statementList = new ArrayList<>();

		int size = 5000;
		for (int i = 0; i < size; i++) {

			IRI subject = vf.createIRI("http://ex/" + i);
			statementList.add(vf.createStatement(subject, RDF.TYPE, FOAF.PERSON));
			statementList.add(vf.createStatement(subject, FOAF.AGE, vf.createLiteral(i % 80 + 1)));
			statementList.add(vf.createStatement(subject, FOAF.NAME, vf.createLiteral("fjeiwojf kldsfjewif " + i)));
			statementList
					.add(vf.createStatement(subject, FOAF.KNOWS, vf.createIRI("http://ex/" + random.nextInt(size))));
			statementList
					.add(vf.createStatement(subject, FOAF.KNOWS, vf.createIRI("http://ex/" + random.nextInt(size))));
			statementList
					.add(vf.createStatement(subject, FOAF.KNOWS, vf.createIRI("http://ex/" + random.nextInt(size))));
		}

		Collections.shuffle(statementList, new Random(4628462));

		return statementList;
	}

	private static List<Statement> getSmallList(List<Statement> statements) {
		List<Statement> ret = new ArrayList<>(statements.subList(0, statements.size() / 4));
		Collections.shuffle(ret, new Random(4629472));
		return ret;
	}

	private static List<Statement> getRealData() {
		try {
			try (InputStream inputStream = new BufferedInputStream(NotifyingSailBenchmark.class.getClassLoader()
					.getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl"))) {
				return Rio.parse(inputStream, RDFFormat.TURTLE)
						.stream()
						.limit(REAL_DATA_SIZE)
						.collect(Collectors.toList());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (NullPointerException e) {
			throw new RuntimeException("Could not load file: benchmarkFiles/datagovbe-valid.ttl", e);
		}
	}

	private static List<Statement> getRealDataRandom() {
		try {
			try (InputStream inputStream = new BufferedInputStream(NotifyingSailBenchmark.class.getClassLoader()
					.getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl"))) {
				List<Statement> collect = Rio.parse(inputStream, RDFFormat.TURTLE)
						.stream()
						.skip(REAL_DATA_SIZE)
						.limit(REAL_DATA_SIZE)
						.collect(Collectors.toList());
				Collections.shuffle(collect, new Random(449583));
				return collect;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (NullPointerException e) {
			throw new RuntimeException("Could not load file: benchmarkFiles/datagovbe-valid.ttl", e);
		}
	}

	private static Consumer<Statement> getStatementConsumer(SailConnection connection) {
		return st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
	}
}

class TestNotifyingSail extends NotifyingSailWrapper {

	public TestNotifyingSail(NotifyingSail baseSail) {
		super(baseSail);
	}

	@Override
	public NotifyingSailConnection getConnection() throws SailException {
		return new TestNotifyingSailConnection(super.getConnection());
	}
}

class TestNotifyingSailConnection extends NotifyingSailConnectionWrapper implements SailConnectionListener {

	int added;
	int removed;

	public TestNotifyingSailConnection(NotifyingSailConnection wrappedCon) {
		super(wrappedCon);
		addConnectionListener(this);
	}

	@Override
	public void statementAdded(Statement statement) {
		added++;
	}

	@Override
	public void statementRemoved(Statement statement) {
		removed++;
	}

}
