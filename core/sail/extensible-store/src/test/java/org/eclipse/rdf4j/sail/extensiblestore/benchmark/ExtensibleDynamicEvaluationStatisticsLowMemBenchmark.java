/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
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

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
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
@Fork(value = 1, jvmArgs = { "-Xms16M", "-Xmx16M" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=60s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ExtensibleDynamicEvaluationStatisticsLowMemBenchmark {

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("ExtensibleDynamicEvaluationStatisticsLowMemBenchmark") // adapt to control which benchmark
																					// tests to
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

	private static InputStream getResourceAsStream(String name) {
		return ExtensibleDynamicEvaluationStatisticsLowMemBenchmark.class.getClassLoader().getResourceAsStream(name);
	}

	@Benchmark
	public ExtensibleDynamicEvaluationStatistics addStatements() throws IOException, InterruptedException {
		ExtensibleDynamicEvaluationStatistics extensibleDynamicEvaluationStatistics = new ExtensibleDynamicEvaluationStatistics(
				null);

		RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
		parser.setRDFHandler(new RDFHandler() {
			@Override
			public void startRDF() throws RDFHandlerException {

			}

			@Override
			public void endRDF() throws RDFHandlerException {

			}

			@Override
			public void handleNamespace(String prefix, String uri) throws RDFHandlerException {

			}

			@Override
			public void handleStatement(Statement st) throws RDFHandlerException {
				extensibleDynamicEvaluationStatistics
						.add(ExtensibleStatementHelper.getDefaultImpl().fromStatement(st, false));
			}

			@Override
			public void handleComment(String comment) throws RDFHandlerException {

			}
		});

		parser.parse(getResourceAsStream("bsbm-100.ttl"), "");

		extensibleDynamicEvaluationStatistics.waitForQueue();
		return extensibleDynamicEvaluationStatistics;
	}

}
