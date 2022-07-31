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
package org.eclipse.rdf4j.query.algebra.evaluation.util.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
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

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=5s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ValueComparatorBenchmark {

	List<Value> subjects;
	List<Value> predicates;
	List<Value> objects;
	List<Value> manyPointerEquals;
	List<Value> manyDeepEquals;

	@Setup(Level.Invocation)
	public void setUp() throws InterruptedException, IOException {

		try (InputStream resourceAsStream = ValueComparatorBenchmark.class.getClassLoader()
				.getResourceAsStream("benchmarkFiles/bsbm-100.ttl")) {

			Model parse = Rio.parse(resourceAsStream, "", RDFFormat.TURTLE);

			subjects = parse.subjects().stream().limit(1000).collect(Collectors.toList());
			predicates = parse.predicates().stream().limit(1000).collect(Collectors.toList());
			objects = parse.objects().stream().limit(1000).collect(Collectors.toList());

			manyPointerEquals = new ArrayList<>();

			manyPointerEquals.addAll(subjects);
			manyPointerEquals.addAll(predicates);
			manyPointerEquals.addAll(objects);

			Collections.shuffle(manyPointerEquals, new Random(468202874));
			manyPointerEquals = manyPointerEquals.subList(0, 100);
			for (int i = 0; i < 5; i++) {
				manyPointerEquals.addAll(manyPointerEquals);
			}

			manyPointerEquals = manyPointerEquals.stream().limit(1000).collect(Collectors.toList());

			SimpleValueFactory vf = SimpleValueFactory.getInstance();
			BNode bNode = vf.createBNode();
			manyDeepEquals = new ArrayList<>();
			for (int i = 0; i < 100; i++) {
				manyDeepEquals.add(vf.createLiteral(true));
				manyDeepEquals.add(vf.createLiteral(33835273));
				manyDeepEquals.add(vf.createLiteral(new Date(4526183)));
				manyDeepEquals.add(vf.createLiteral("jkldsjfl", "fe"));
				manyDeepEquals.add(vf.createLiteral("fjewkhfoi183e 31hf8h2 equh f8311u hdeuf8013 fqe!#$"));
				manyDeepEquals.add(vf.createLiteral(634278732L));
				manyDeepEquals.add(vf.createLiteral(0.465792));
				manyDeepEquals.add(vf.createLiteral(17.465792F));
				manyDeepEquals.add(vf.createBNode("fjdsklk31R"));
				manyDeepEquals
						.add(vf.createIRI("http://example.com/main/ontology/something/item32784y83rh8193ey81rfehw"));
			}

		}

		System.gc();
		Thread.sleep(100);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder().include("ValueComparatorBenchmark.*")
				.build();

		new Runner(opt).run();
	}

	@Benchmark
	public int sortSubjects() {

		ValueComparator valueComparator = new ValueComparator();
		int compare = 0;
		for (Value v1 : subjects) {
			for (Value v2 : subjects) {
				compare += valueComparator.compare(v1, v2);
			}
		}

		return compare;
	}

	@Benchmark
	public int sortPredicates() {

		ValueComparator valueComparator = new ValueComparator();
		int compare = 0;
		for (Value v1 : predicates) {
			for (Value v2 : predicates) {
				compare += valueComparator.compare(v1, v2);
			}
		}

		return compare;
	}

	@Benchmark
	public int sortObjects() {

		ValueComparator valueComparator = new ValueComparator();
		int compare = 0;
		for (Value v1 : objects) {
			for (Value v2 : objects) {
				compare += valueComparator.compare(v1, v2);
			}
		}

		return compare;
	}

	@Benchmark
	public int sortManyPointerEquals() {

		ValueComparator valueComparator = new ValueComparator();
		int compare = 0;
		for (Value v1 : manyPointerEquals) {
			for (Value v2 : manyPointerEquals) {
				compare += valueComparator.compare(v1, v2);
			}
		}

		return compare;
	}

	@Benchmark
	public int sortManyDeepEquals() {

		ValueComparator valueComparator = new ValueComparator();
		int compare = 0;
		for (Value v1 : manyDeepEquals) {
			for (Value v2 : manyDeepEquals) {
				compare += valueComparator.compare(v1, v2);
			}
		}

		return compare;
	}

}
