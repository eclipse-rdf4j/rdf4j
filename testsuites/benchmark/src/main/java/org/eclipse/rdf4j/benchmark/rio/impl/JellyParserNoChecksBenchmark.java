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
package org.eclipse.rdf4j.benchmark.rio.impl;

import org.eclipse.rdf4j.benchmark.rio.ParserBenchmark;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import eu.neverblink.jelly.convert.rdf4j.rio.JellyParser;
import eu.neverblink.jelly.convert.rdf4j.rio.JellyParserSettings;

/**
 * Version of {@link JellyParserBenchmark} with disabled validations.
 */
public class JellyParserNoChecksBenchmark extends ParserBenchmark {

	@Override
	public RDFParser getParser() {
		var parser = new JellyParser();
		parser.set(JellyParserSettings.CHECKING, false);
		parser.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
		return parser;
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(JellyParserNoChecksBenchmark.class.getSimpleName())
				.build();
		new Runner(opt).run();
	}
}
