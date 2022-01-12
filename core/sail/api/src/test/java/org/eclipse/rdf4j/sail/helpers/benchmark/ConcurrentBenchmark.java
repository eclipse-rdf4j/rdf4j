/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.helpers.benchmark;

import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.helpers.impl.SailImpl;
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
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author Håvard M. Ottestad
 */
@State(Scope.Benchmark)
@Warmup(iterations = 2)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms1G", "-Xmx1G", })
//@Fork(value = 1, jvmArgs = {"-Xms1G", "-Xmx1G", "-XX:StartFlightRecording=delay=60s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 2)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ConcurrentBenchmark extends BaseConcurrentBenchmark {

	private final Sail sail = new SailImpl();

	public static void main(String[] args) throws Exception {
		Options opt = new OptionsBuilder()
				.include("ConcurrentBenchmark.hasStatementSharedConnection") // adapt to run other benchmark tests
				.forks(0)
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
	public void hasStatement(Blackhole blackhole) throws Exception {
		threads(100, () -> {
			try (SailConnection connection = sail.getConnection()) {
				for (int i = 0; i < 1000; i++) {
					boolean b = connection.hasStatement(null, null, null, true);
					blackhole.consume(b);
				}
			}
		});
	}

	@Benchmark
	public void hasStatementSharedConnection(Blackhole blackhole) throws Exception {
		try (SailConnection connection = sail.getConnection()) {
			threads(100, () -> {
				for (int i = 0; i < 1000; i++) {
					boolean b = connection.hasStatement(null, null, null, true);
					blackhole.consume(b);
				}
			});
		}
	}

}
