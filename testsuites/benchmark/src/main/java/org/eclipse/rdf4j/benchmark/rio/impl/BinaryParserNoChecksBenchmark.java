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
import org.eclipse.rdf4j.rio.binary.BinaryRDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Version of {@link BinaryParserBenchmark} with disabled validations.
 */
public class BinaryParserNoChecksBenchmark extends ParserBenchmark {

	@Override
	public RDFParser getParser() {
		var parser = new BinaryRDFParser();
		parser.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, false);
		parser.set(BasicParserSettings.VERIFY_RELATIVE_URIS, false);
		parser.set(BasicParserSettings.VERIFY_URI_SYNTAX, false);
		parser.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
		return parser;
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(BinaryParserNoChecksBenchmark.class.getSimpleName())
				.build();
		new Runner(opt).run();
	}
}
