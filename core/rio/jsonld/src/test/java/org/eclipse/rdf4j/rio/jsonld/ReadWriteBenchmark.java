/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.rio.jsonld;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
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

@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode({ Mode.AverageTime })
@Fork(value = 1, jvmArgs = { "-Xms2G", "-Xmx2G" })
//@Fork(value = 1, jvmArgs = {"-Xms2G", "-Xmx2G",   "-XX:StartFlightRecording=delay=5s,duration=120s,filename=recording.jfr,settings=profile", "-XX:FlightRecorderOptions=samplethreads=true,stackdepth=1024", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints"})
@Measurement(iterations = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ReadWriteBenchmark {

	Model parsed;
	private String parsedAsString;

	@Setup(Level.Trial)
	public void setUp() throws IOException {
		try (InputStream resourceAsStream = ReadWriteBenchmark.class.getClassLoader()
				.getResourceAsStream("benchmark/datagovbe-valid.jsonld")) {
			parsed = Rio.parse(resourceAsStream, "", RDFFormat.JSONLD);
		}

		parsedAsString = FileUtils.readFileToString(new File(
				ReadWriteBenchmark.class.getClassLoader().getResource("benchmark/datagovbe-valid.jsonld").getFile()),
				StandardCharsets.UTF_8);
	}

	@Benchmark
	public Writer writeToStringWriter() throws IOException {
		try (Writer writer = new StringWriter()) {
			Rio.write(parsed, writer, RDFFormat.JSONLD,
					new WriterConfig().set(BasicWriterSettings.PRETTY_PRINT, false));
			return writer;
		}
	}

	@Benchmark
	public Model parseFromString() throws IOException {
		return Rio.parse(new StringReader(parsedAsString), "", RDFFormat.JSONLD);
	}

}
