/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.query.parser.sparql;

import org.eclipse.rdf4j.query.parser.ParsedQuery;
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
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Warmup(iterations = 10)
@BenchmarkMode({ Mode.AverageTime })
//@Fork(value = 1, jvmArgs = {"-Xms4G", "-Xmx4G", "-Xmn2G", "-XX:+UseSerialGC", "-XX:+UnlockCommercialFeatures", "-XX:StartFlightRecording=delay=5s,duration=60s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Fork(value = 1, jvmArgs = { "-Xms4G", "-Xmx4G", "-Xmn2G", "-XX:+UseSerialGC" })
@Measurement(iterations = 40)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SPARQLParseBenchmark {

	@Setup(Level.Iteration)
	public void setUp() {
		System.gc();
	}

	@Benchmark
	public int selectValuesQuery() {

		int temp = 0;
		for (int i = 0; i < 1000; i++) {
			String simpleSparqlQuery = "select * where { VALUES (?a) {(<http://a" + i + ">)} ?a ?P ?Y } order by ?a";

			SPARQLParser parser = new SPARQLParser();

			ParsedQuery q = parser.parseQuery(simpleSparqlQuery, null);

			temp += q.hashCode();
		}

		return temp;

	}

	public static void main(String[] args) throws RunnerException {
		String regexp = SPARQLParseBenchmark.class.getName() + "\\.*";

		new Runner(new OptionsBuilder().include(regexp).build()).run();
	}

}
