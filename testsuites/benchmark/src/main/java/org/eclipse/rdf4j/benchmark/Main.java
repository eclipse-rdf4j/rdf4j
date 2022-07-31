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
package org.eclipse.rdf4j.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class Main {

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder().include("")
				.forks(1)
				.build();

		new Runner(opt).run();
	}

//	public static void main(String[] args) throws RunnerException {
//		Options opt = new OptionsBuilder().include(
//				"org.eclipse.rdf4j.benchmark.ReasoningBenchmark.forwardChainingSchemaCachingRDFSInferencerMultipleTransactions$")
//				.param("param", "moreRdfs::12180")
//				.forks(1)
//				.build();
//
//		new Runner(opt).run();
//	}

}
