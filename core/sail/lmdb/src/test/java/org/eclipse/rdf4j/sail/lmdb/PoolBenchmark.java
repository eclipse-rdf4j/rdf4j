/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.lwjgl.util.lmdb.MDBVal;
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
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
@Threads(8)
public class PoolBenchmark {

	private Pool pool;

	@Setup(Level.Trial)
	public void setUp() {
		pool = new Pool();
	}

	@TearDown(Level.Trial)
	public void tearDown() {
		if (pool != null) {
			pool.close();
		}
	}

	@Benchmark
	public void borrowAndReturnVal() {
		MDBVal value = pool.getVal();
		pool.free(value);
	}

	@Benchmark
	public void borrowAndReturnKeyBuffer() {
		ByteBuffer keyBuffer = pool.getKeyBuffer();
		pool.free(keyBuffer);
	}

	@Benchmark
	public void borrowAndReturnBoth() {
		MDBVal value = pool.getVal();
		ByteBuffer keyBuffer = pool.getKeyBuffer();
		pool.free(value);
		pool.free(keyBuffer);
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(PoolBenchmark.class.getSimpleName())
				.forks(1)
				.build();
		new Runner(opt).run();
	}
}
