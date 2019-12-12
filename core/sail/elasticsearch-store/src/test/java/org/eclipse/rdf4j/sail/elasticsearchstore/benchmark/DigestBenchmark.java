/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.elasticsearchstore.benchmark;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchDataStructure;
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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Håvard Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 20)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseG1GC" })
//@Fork(value = 1, jvmArgs = {"-Xms8G", "-Xmx8G", "-Xmn4G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=60s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class DigestBenchmark {

	List<Statement> statements;

	@Setup(Level.Trial)
	public void beforeClass() throws IOException, InterruptedException {

		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		statements = IntStream
				.range(0, 100000)
				.mapToObj(i -> vf.createStatement(vf.createBNode(), RDFS.LABEL,
						vf.createLiteral("fjuefhru8f49ru3ue 0ji fh84 h2uh esfh83 2r9u389 hrefuh2398r32r" + i)))
				.collect(Collectors.toList());

	}

	@Benchmark
	public void clearAndAddLargeFile() throws IOException {

		for (Statement statement : statements) {

			String s = ElasticsearchDataStructure.sha256(statement);
			if (s.length() > 1000) {
				System.out.println("This never happens");
			}

		}

	}

	@Benchmark
	public void statementToString() throws IOException {

		for (Statement statement : statements) {

			String s = statement.toString();
			if (s.length() > 1000) {
				System.out.println("This never happens");
			}

		}

	}

}
