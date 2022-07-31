/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.benchmark.rio.impl;

import org.eclipse.rdf4j.benchmark.rio.ParserBenchmark;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.n3.N3Parser;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 */
public class N3ParserBenchmark extends ParserBenchmark {

	@Override
	public RDFParser getParser() {
		return new N3Parser();
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(N3ParserBenchmark.class.getSimpleName())
				.build();
		new Runner(opt).run();
	}
}
