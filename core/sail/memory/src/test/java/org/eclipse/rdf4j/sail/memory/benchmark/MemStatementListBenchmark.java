/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.memory.benchmark;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.memory.model.MemStatement;
import org.eclipse.rdf4j.sail.memory.model.MemStatementList;
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
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.google.common.collect.Lists;

/**
 * This class is a benchmark for testing the performance of various data loading scenarios using the MemStatement class.
 *
 * @author Håvard Mikkelsen Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
//@Fork(value = 1, jvmArgs = {"-Xms4G", "-Xmx4G", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=5s,duration=60s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Fork(value = 3, jvmArgs = { "-Xms4G", "-Xmx4G" })
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MemStatementListBenchmark extends BaseConcurrentBenchmark {

	private static final List<MemStatement> statementList = getStatements();
	private static final List<MemStatement> realData = getRealData();
	private static final int CHUNKS = 32;

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("MemStatementListBenchmark.*") // adapt to run other benchmark tests
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Trial)
	public void setup() throws Exception {
		super.setup();
	}

	@TearDown(Level.Trial)
	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Benchmark
	public int loadSynthetic() throws InterruptedException {

		MemStatementList memStatementList = new MemStatementList();
		for (MemStatement statement : statementList) {
			memStatementList.add(statement);
		}
		return memStatementList.size();

	}

	@Benchmark
	public int loadRealData() throws InterruptedException {

		MemStatementList memStatementList = new MemStatementList();
		for (MemStatement statement : realData) {
			memStatementList.add(statement);
		}
		return memStatementList.size();

	}

	@Benchmark
	public int loadRealDataArrayList() {

		ArrayList<MemStatement> memStatementList = new ArrayList<>();
		for (MemStatement statement : realData) {
			memStatementList.add(statement);
		}
		return memStatementList.size();

	}

	@Benchmark
	public int loadRealDataSynchronizedArrayList() {

		List<MemStatement> memStatementList = Collections.synchronizedList(new ArrayList<>());
		for (MemStatement statement : realData) {
			memStatementList.add(statement);
		}
		return memStatementList.size();

	}

	@Benchmark
	public int loadRealDataConcurrentLinkedQueue() {

		ConcurrentLinkedQueue<MemStatement> memStatementList = new ConcurrentLinkedQueue<>();
		for (MemStatement statement : realData) {
			memStatementList.add(statement);
		}
		return memStatementList.size();

	}

	@Benchmark
	public int loadRealDataConcurrently() throws InterruptedException {

		List<List<MemStatement>> partition = Lists.partition(realData, CHUNKS);

		MemStatementList memStatementList = new MemStatementList();

		runMultiWorkload(partition, memStatementList, (s, m) -> {
			for (MemStatement memStatement : s) {
				try {
					m.add(memStatement);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});

		return memStatementList.size();
	}

	@Benchmark
	public int loadRealDataConcurrentLinkedQueueConcurrently() throws InterruptedException {

		List<List<MemStatement>> partition = Lists.partition(realData, CHUNKS);

		ConcurrentLinkedQueue<MemStatement> memStatementList = new ConcurrentLinkedQueue<>();

		runMultiWorkload(partition, memStatementList, (s, m) -> {
			for (MemStatement memStatement : s) {
				m.add(memStatement);
			}
		});

		return memStatementList.size();

	}

	@Benchmark
	public int loadRealDataSynchronizedArrayListConcurrently() throws InterruptedException {

		List<List<MemStatement>> partition = Lists.partition(realData, CHUNKS);

		List<MemStatement> memStatementList = Collections.synchronizedList(new ArrayList<>());

		runMultiWorkload(partition, memStatementList, (s, m) -> {
			for (MemStatement memStatement : s) {
				m.add(memStatement);
			}
		});

		return memStatementList.size();

	}

	@Benchmark
	public int loadRealDataIntoSeparateMemStatementLists() throws InterruptedException {

		List<MemStatementList> lists = new ArrayList<>(realData.size() + 1);

		for (MemStatement statement : realData) {
			MemStatementList memStatementList = new MemStatementList();
			memStatementList.add(statement);
			lists.add(memStatementList);
		}

		int i = 0;
		for (MemStatementList list : lists) {
			i += list.size();
		}

		return i;
	}

	@Benchmark
	public int loadRealDataIntoSeparateMemStatementListsConcurrently() throws InterruptedException {

		List<List<MemStatement>> partition = Lists.partition(realData, CHUNKS);

		ConcurrentLinkedQueue<MemStatementList> memStatementLists = new ConcurrentLinkedQueue<>();

		runMultiWorkload(partition, memStatementLists, (s, m) -> {
			try {
				for (MemStatement statement : s) {
					MemStatementList memStatementList = new MemStatementList();
					memStatementList.add(statement);
					m.add(memStatementList);
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		});

		int i = 0;
		for (MemStatementList list : memStatementLists) {
			i += list.size();
		}

		return i;
	}

	private <T, S> void runMultiWorkload(List<T> partition, S memStatementList, BiConsumer<T, S> biConsumer)
			throws InterruptedException {
		CountDownLatch startSignal = new CountDownLatch(1);

		List<? extends Future<?>> collect = partition.stream()
				.map(statements -> getRunnable(startSignal, statements, memStatementList, biConsumer))
				.map(this::submit)
				.collect(Collectors.toList());

		startSignal.countDown();

		for (Future<?> future : collect) {
			try {
				future.get();
			} catch (ExecutionException e1) {
				throw new RuntimeException(e1);
			}
		}
	}

	private static List<MemStatement> getStatements() {

		Random random = new Random(34435325);

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

		MemoryStore memoryStore = new MemoryStore();
		memoryStore.init();

		try (NotifyingSailConnection connection = memoryStore.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			statementList.forEach(
					st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext()));
			connection.commit();
			return connection.getStatements(null, null, null, false)
					.stream()
					.map(s -> ((MemStatement) s))
					.collect(Collectors.toList());
		}
	}

	private static List<MemStatement> getRealData() {
		try {
			try (InputStream inputStream = new BufferedInputStream(MemStatementListBenchmark.class.getClassLoader()
					.getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl"))) {
				Model parse = Rio.parse(inputStream, RDFFormat.TURTLE);

				MemoryStore memoryStore = new MemoryStore();
				memoryStore.init();

				try (NotifyingSailConnection connection = memoryStore.getConnection()) {
					connection.begin(IsolationLevels.NONE);
					parse.forEach(st -> connection.addStatement(st.getSubject(), st.getPredicate(), st.getObject(),
							st.getContext()));
					connection.commit();
					return connection.getStatements(null, null, null, false)
							.stream()
							.map(s -> ((MemStatement) s))
							.collect(Collectors.toList());
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (NullPointerException e) {
			throw new RuntimeException("Could not load file: benchmarkFiles/datagovbe-valid.ttl", e);
		}
	}

}
