/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.benchmark;

import org.junit.Test;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author Håvard Ottestad
 */
public class BenchmarkJunitTests {

	@Test
	public void test() throws RunnerException {
		Options opt = new OptionsBuilder()
				.include("")
				.exclude(ComplexLargeBenchmark.class.getSimpleName())
				.exclude(NativeStoreBenchmark.class.getSimpleName())
				.warmupBatchSize(1)
				.measurementBatchSize(1)
				.measurementIterations(1)
				.warmupIterations(1)
				.forks(1)
				.build();

		new Runner(opt).run();
	}

}
