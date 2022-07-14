/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.benchmark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleIRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
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
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G" })
//@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=5s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class InstanceofBenchmark {

	List<Value> subjects;

	@Setup(Level.Iteration)
	public void setUp() throws InterruptedException, IOException {

		subjects = new ArrayList<>();
		Random random = new Random(89439204);
		ValueFactory vf = new CustomValueFactory();
		for (int i = 0; i < 10000; i++) {
			subjects.add(vf.createLiteral(random.nextBoolean()));
			subjects.add(vf.createLiteral(random.nextInt()));
			subjects.add(vf.createLiteral(random.nextInt() + "fjfdskl"));
			subjects.add(vf.createLiteral(random.nextInt() + "fjfdskl", "en"));
			subjects.add(vf.createBNode(random.nextInt() + ""));
			subjects.add(vf.createIRI("http://example.com/" + random.nextInt()));
		}

		Collections.shuffle(subjects, random);

	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder().include("InstanceofBenchmark.*")
				.build();

		new Runner(opt).run();
	}

	@Benchmark
	public int instanceOf() {

		int count = 0;

		for (Value subject : subjects) {
			if (subject instanceof IRI) {
				count += 1;
			}
			if (subject instanceof Literal) {
				count += 2;
			}
			if (subject instanceof BNode) {
				count += 3;
			}
			if (subject instanceof Triple) {
				count += 4;
			}
			if (subject instanceof Resource) {
				count += 5;
			}
		}

		return count;

	}

	@Benchmark
	public int helperMethod() {

		int count = 0;

		for (Value subject : subjects) {
			if (subject.isIRI()) {
				count += 1;
			}
			if (subject.isLiteral()) {
				count += 2;
			}
			if (subject.isBNode()) {
				count += 3;
			}
			if (subject.isTriple()) {
				count += 4;
			}
			if (subject.isResource()) {
				count += 5;
			}
		}

		return count;

	}

	public static class CustomValueFactory extends SimpleValueFactory {

		@Override
		public IRI createIRI(String iri) {
			return new CustomIRI(iri);
		}

		protected CustomValueFactory() {
			super();
		}

		public static class CustomIRI extends SimpleIRI {

			int counter;
			static int staticCounter = 0;

			public CustomIRI() {
				counter = staticCounter++;
			}

			public CustomIRI(String iriString) {
				super(iriString);
				counter = staticCounter++;
			}
		}

	}

}
