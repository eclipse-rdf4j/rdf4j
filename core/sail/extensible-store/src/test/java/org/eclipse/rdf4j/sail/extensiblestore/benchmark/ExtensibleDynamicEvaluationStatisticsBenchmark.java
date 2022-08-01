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

package org.eclipse.rdf4j.sail.extensiblestore.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics.ExtensibleDynamicEvaluationStatistics;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatementHelper;
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
@Fork(value = 1, jvmArgs = { "-Xms64M", "-Xmx64M" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=60s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ExtensibleDynamicEvaluationStatisticsBenchmark {

	Model parse;

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("ExtensibleDynamicEvaluationStatisticsBenchmark") // adapt to control which benchmark tests to
																			// run
				.forks(1)
				.build();

		new Runner(opt).run();
	}

	@Setup(Level.Iteration)
	public void beforeClassIteration() throws IOException, InterruptedException {

		System.gc();
		Thread.sleep(100);
		System.gc();
		Thread.sleep(100);

	}

	@Setup(Level.Trial)
	public void beforeClass() throws IOException, InterruptedException {

		parse = Rio.parse(getResourceAsStream("bsbm-100.ttl"), "", RDFFormat.TURTLE);
		System.gc();

	}

	private static InputStream getResourceAsStream(String name) {
		return ExtensibleDynamicEvaluationStatisticsBenchmark.class.getClassLoader().getResourceAsStream(name);
	}

	@Benchmark
	public ExtensibleDynamicEvaluationStatistics addStatements() throws IOException, InterruptedException {
		ExtensibleDynamicEvaluationStatistics extensibleDynamicEvaluationStatistics = new ExtensibleDynamicEvaluationStatistics(
				null);

		parse.forEach(s -> extensibleDynamicEvaluationStatistics
				.add(ExtensibleStatementHelper.getDefaultImpl().fromStatement(s, false)));

		extensibleDynamicEvaluationStatistics.waitForQueue();

		return extensibleDynamicEvaluationStatistics;
	}

	@Benchmark
	public ExtensibleDynamicEvaluationStatistics instantiate() throws IOException {
		ExtensibleDynamicEvaluationStatistics extensibleDynamicEvaluationStatistics = new ExtensibleDynamicEvaluationStatistics(
				null);

		return extensibleDynamicEvaluationStatistics;
	}

}
