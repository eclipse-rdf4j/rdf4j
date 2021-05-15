/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.benchmark.rio;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;

import static org.apache.commons.io.output.NullOutputStream.NULL_OUTPUT_STREAM;
import static org.eclipse.rdf4j.rio.helpers.BasicParserSettings.VERIFY_LANGUAGE_TAGS;
import static org.eclipse.rdf4j.rio.helpers.BasicParserSettings.VERIFY_RELATIVE_URIS;
import static org.eclipse.rdf4j.rio.helpers.BasicParserSettings.VERIFY_URI_SYNTAX;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.io.output.CountingOutputStream;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.binary.BinaryRDFWriter;

/**
 * <p>This class benchmarks {@link RDFWriter}s in terms of output size given a number of datasets (see
 * {@link RDFTestDataset}. The output of the benchmark is the output size in megabytes, the time for parsing the input
 * dataset (!) and writing the output and a description of the writer used for each of the datasets.</p>
 * <p>Please note that the datasets from {@link RDFTestDataset} are fairly large files</p>
 * 
 * @author Frens Jan Rumph
 */
public class RDFSizeBenchmarks {

	public static void main(String[] args) throws IOException {
		List<File> datasets = List.of(
				RDFTestDataset.SWDF.download(),
				RDFTestDataset.LEXVO.download(),
				RDFTestDataset.DATAGOVBE.download(),
				RDFTestDataset.FISHMARK.download(),
				RDFTestDataset.SP2BENCH.download(),
				RDFTestDataset.GENE2GO.download(),
				RDFTestDataset.BSBM.download()
		);

		Map<String, Function<OutputStream, RDFWriter>> writers = new LinkedHashMap<>();
		writers.put("binary, default settings", out -> new BinaryRDFWriter(out));
		writers.put("binary, buffer size = 8k", out -> new BinaryRDFWriter(out, 8 * 1024));

		for (File dataset : datasets) {
			for (Map.Entry<String, Function<OutputStream, RDFWriter>> writer : writers.entrySet()) {
				System.gc();
				reportSize(dataset, writer.getKey(), writer.getValue());
			}
		}
	}

	private static void reportSize(File path, String description, Function<OutputStream, RDFWriter> writer)
			throws IOException {
		String fileName = path.getName();
		RDFFormat format = Rio.getParserFormatForFileName(fileName)
				.orElseThrow(() -> new IllegalArgumentException("No format available for " + fileName));
		InputStream is = new BufferedInputStream(new FileInputStream(path));
		reportSize(fileName, is, description, format, writer);
	}

	private static void reportSize(String dataset, InputStream is, String description, RDFFormat format,
			Function<OutputStream, RDFWriter> writerConstructor) throws IOException {

		CountingOutputStream os = new CountingOutputStream(NULL_OUTPUT_STREAM);
		RDFWriter writer = writerConstructor.apply(os);

		RDFParser parser = Rio.createParser(format);
		parser.setRDFHandler(writer);
		// Verification of datasets is disabled because of encoding issues in the input data and it's not a critical
		// part of the benchmarking.
		parser.setParserConfig(new ParserConfig()
				.set(VERIFY_LANGUAGE_TAGS, false)
				.set(VERIFY_RELATIVE_URIS, false)
				.set(VERIFY_URI_SYNTAX, false));

		Instant start = Instant.now();
		try {
			parser.parse(is);
		} finally {
			is.close();
		}
		Instant end = Instant.now();
		Duration duration = Duration.between(start, end);

		long size = os.getByteCount();
		System.out.printf(
				"%20s %8.2f MB in %-14s - %s%n",
				dataset,
				size / 1024 / 1024f,
				duration,
				description
		);
	}

}
