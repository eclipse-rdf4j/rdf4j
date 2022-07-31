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

package org.eclipse.rdf4j.benchmark.rio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.benchmark.rio.util.BlackHoleRDFHandler;
import org.eclipse.rdf4j.benchmark.rio.util.DataSetGenerator;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
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
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmark for parsing a file created through {@link DataSetGenerator} with {@link BlackHoleRDFHandler}.
 *
 * @author Tomas Kovachev t.kovachev1996@gmail.com
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3)
@Measurement(iterations = 3)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms4g", "-Xmx4g", "-XX:+UseSerialGC" })
public abstract class ParserBenchmark {
	public static final int TOTAL_STATEMENTS = 1_000_000;
	public static final int MIN_STRING_LENGTH = 100;
	public static final int MAX_STRING_LENGTH = 300;
	public static final int PERCENT_BNODE = 30;
	public static final int PERCENT_LITERALS = 50;
	public static final boolean TEXT_ONLY = false;
	private File toReadFrom;
	private BlackHoleRDFHandler rdfHandler;
	private RDFParser parser;

	@Setup(Level.Trial)
	public void setup() throws IOException {
		parser = getParser();
		RDFFormat format = parser.getRDFFormat();
		rdfHandler = new BlackHoleRDFHandler();

		if (toReadFrom == null) {
			// If format supports graphs, they will be included in the dataset
			DataSetGenerator generator = new DataSetGenerator();
			toReadFrom = Files.createTempFile(
					"rdf4j-parser-benchmark", "." + format.getDefaultFileExtension()).toFile();
			toReadFrom.deleteOnExit();

			try (FileOutputStream out = new FileOutputStream(toReadFrom)) {
				RDFWriter writer = Rio.createWriter(format, out);
				generator.generateStatements(writer, PERCENT_BNODE, PERCENT_LITERALS,
						MIN_STRING_LENGTH, MAX_STRING_LENGTH, TOTAL_STATEMENTS, TEXT_ONLY, true);
			}
		}
	}

	@Benchmark
	@BenchmarkMode(Mode.AverageTime)
	public void parseStream(Blackhole blackhole) throws IOException {
		try (FileInputStream stream = new FileInputStream(toReadFrom)) {
			parser.setRDFHandler(rdfHandler);
			rdfHandler.setBlackHoleConsumer((blackhole::consume));
			parser.parse(stream, DataSetGenerator.NAMESPACE);
		}
	}

	@Benchmark
	@BenchmarkMode(Mode.AverageTime)
	public void parseBufferedReader(Blackhole blackhole) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(toReadFrom)))) {
			parser.setRDFHandler(rdfHandler);
			rdfHandler.setBlackHoleConsumer((blackhole::consume));
			parser.parse(reader, DataSetGenerator.NAMESPACE);
		}
	}

	public abstract RDFParser getParser();
}
