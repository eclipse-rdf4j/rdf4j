/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.eclipse.rdf4j.common.exception.ValidationException;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.Util;
import org.eclipse.rdf4j.console.VerificationListener;
import org.eclipse.rdf4j.console.setting.ConsoleSetting;
import org.eclipse.rdf4j.console.setting.WorkDir;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;

/**
 * Verify command
 *
 * @author Dale Visser
 * @author Bart Hanssens
 */
public class Verify extends ConsoleCommand {
	@Override
	public String getName() {
		return "verify";
	}

	@Override
	public String getHelpShort() {
		return "Verifies the syntax of an RDF data file, takes a file path or URL as argument";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE + "verify <location> [<shacl-location> <report.ttl>]\n"
				+ "  <location>                               The file path or URL identifying the data file\n"
				+ "  <location> <shacl-location> <report.ttl> Validate using shacl file and create a report\n"
				+ "Verifies the validity of the specified data file\n";
	}

	@Override
	public String[] usesSettings() {
		return new String[] { WorkDir.NAME };
	}

	/**
	 * Constructor
	 *
	 * @param consoleIO
	 * @param settings
	 */
	public Verify(ConsoleIO consoleIO, Map<String, ConsoleSetting> settings) {
		super(consoleIO, null, settings);
	}

	@Override
	public void execute(String... tokens) {
		if (tokens.length != 2 && tokens.length != 4) {
			writeln(getHelpLong());
			return;
		}

		String dataPath = parseDataPath(tokens[1]);
		verify(dataPath);

		if (tokens.length == 4) {
			String shaclPath = parseDataPath(tokens[2]);
			String reportFile = tokens[3];

			shacl(dataPath, shaclPath, reportFile);
		}
	}

	/**
	 * Get working dir setting.
	 *
	 * @return path of working dir
	 */
	private Path getWorkDir() {
		return ((WorkDir) settings.get(WorkDir.NAME)).get();
	}

	/**
	 * Verify an RDF file, either a local file or URL.
	 *
	 * @param tokens parameters
	 */
	private void verify(String dataPath) {
		try {
			URL dataURL = new URL(dataPath);
			RDFFormat format = Rio.getParserFormatForFileName(dataPath).orElseThrow(Rio.unsupportedFormat(dataPath));
			RDFParser parser = Rio.createParser(format);

			writeln("RDF Format is " + parser.getRDFFormat().getName());

			VerificationListener listener = new VerificationListener(consoleIO);

			parser.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
			parser.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);

			parser.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, true);
			parser.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);

			parser.set(BasicParserSettings.VERIFY_RELATIVE_URIS, true);
			parser.set(BasicParserSettings.VERIFY_URI_SYNTAX, true);

			parser.setParseErrorListener(listener);
			parser.setRDFHandler(listener);
			writeln("Verifying data...");

			try (InputStream dataStream = dataURL.openStream()) {
				parser.parse(dataStream, "urn://openrdf.org/RioVerifier/");
			}

			int warnings = listener.getWarnings();
			int errors = listener.getErrors();

			if (warnings + errors > 0) {
				writeError("Found " + warnings + " warnings and " + errors + " errors");
			} else {
				writeln("Data verified, no errors were found");
			}
			if (errors == 0) {
				writeln("File contains " + listener.getStatements() + " statements");
			}
		} catch (MalformedURLException e) {
			writeError("Malformed URL: " + dataPath);
		} catch (IOException e) {
			writeError("Failed to load data", e);
		} catch (UnsupportedRDFormatException e) {
			writeError("No parser available for this RDF format", e);
		} catch (RDFParseException e) {
			writeError("Unexpected RDFParseException", e);
		} catch (RDFHandlerException e) {
			writeError("Unable to verify", e);
		}
	}

	/**
	 * Validate an RDF data source using a SHACL file or URL, writing the report to a file.
	 *
	 * @param dataPath   file or URL of the data to be validated
	 * @param shaclPath  file or URL of the SHACL
	 * @param reportFile file to write validation report to
	 */
	private void shacl(String dataPath, String shaclPath, String reportFile) {
		SailRepository repo = new SailRepository(new ShaclSail(new MemoryStore()));
		repo.init();

		try {

			// load shapes first from a file or URL, defaults to turtle, so one can use .shacl as file extension
			boolean loaded = false;
			try {
				writeln("Loading shapes from " + shaclPath);

				URL shaclURL = new URL(shaclPath);
				RDFFormat format = Rio.getParserFormatForFileName(reportFile).orElse(RDFFormat.TURTLE);

				try (SailRepositoryConnection conn = repo.getConnection()) {
					conn.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
					conn.add(shaclURL, "", format, RDF4J.SHACL_SHAPE_GRAPH);
					conn.commit();
				}
				loaded = true;
			} catch (MalformedURLException e) {
				writeError("Malformed URL: " + shaclPath, e);
			} catch (IOException e) {
				writeError("Failed to load shacl shapes", e);
			}

			if (!loaded) {
				writeError("No shapes found");
				return;
			}

			try {
				URL dataURL = new URL(dataPath);
				RDFFormat format = Rio.getParserFormatForFileName(dataPath)
						.orElseThrow(Rio.unsupportedFormat(dataPath));

				try (SailRepositoryConnection conn = repo.getConnection()) {
					conn.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Disabled);
					conn.add(dataURL, "", format);
					conn.commit();
				}

			} catch (MalformedURLException e) {
				writeError("Malformed URL: " + dataPath);
			} catch (IOException e) {
				writeError("Failed to load data", e);
			} catch (RepositoryException e) {
				Throwable cause = e.getCause();
			}

			try {

				try (SailRepositoryConnection conn = repo.getConnection()) {
					// Bulk validation forces a full revalidation!
					conn.begin(IsolationLevels.NONE, ShaclSail.TransactionSettings.ValidationApproach.Bulk);
					conn.commit();
				}

				writeln("SHACL validation OK");
			} catch (RepositoryException e) {
				Throwable cause = e.getCause();
				if (cause instanceof ValidationException) {
					writeError("SHACL validation failed, writing report to " + reportFile);
					ValidationException sv = (ValidationException) cause;
					writeReport(sv.validationReportAsModel(), reportFile);
				}
			}
		} finally {
			repo.shutDown();
		}

	}

	/**
	 * Parse URL or path to local file. Files will be prefixed with "file:" scheme
	 *
	 * @param str
	 * @return URL path as string
	 */
	private String parseDataPath(String str) {
		String path = str;
		try {
			new URL(str);
			// dataPath is a URI
		} catch (MalformedURLException e) {
			// File path specified, convert to URL
			path = "file:" + Util.getNormalizedPath(getWorkDir(), str);
		}
		return path;
	}

	/**
	 * Write SHACL validation report to a file. File extension is used to select the serialization format, TTL is used
	 * as default.
	 *
	 * @param model      report
	 * @param reportFile file name
	 */
	private void writeReport(Model model, String reportFile) {
		WriterConfig cfg = new WriterConfig();
		cfg.set(BasicWriterSettings.PRETTY_PRINT, true);
		cfg.set(BasicWriterSettings.INLINE_BLANK_NODES, true);

		RDFFormat format = Rio.getParserFormatForFileName(reportFile).orElse(RDFFormat.TURTLE);

		try (Writer w = Files.newBufferedWriter(Paths.get(reportFile))) {
			Rio.write(model, w, format, cfg);
		} catch (IOException ex) {
			writeError("Could not write report to " + reportFile, ex);
		}
	}
}
