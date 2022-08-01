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

package org.eclipse.rdf4j.sail.shacl.benchmark;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * This runs (almost) all the benchmarks during integration testing so that we know that we haven't broken any of the
 * benchmarks.
 *
 * @author Håvard Ottestad
 */
@Tag("slow")
@Isolated
public class BenchmarkIT {

	@Test
	public void test() throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("")
				.exclude(ComplexLargeBenchmark.class.getSimpleName())
				.exclude(ComplexLargeTransactionalBenchmark.class.getSimpleName())
				.exclude(NativeStoreBenchmark.class.getSimpleName())
				.measurementBatchSize(1)
				.measurementTime(TimeValue.NONE)
				.measurementIterations(1)
				.warmupIterations(0)
				.forks(0)
				.shouldFailOnError(true)
				.build();

		new Runner(opt).run();
	}

}
