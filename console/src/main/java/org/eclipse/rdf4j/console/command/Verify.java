/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.VerificationListener;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verify command
 * 
 * @author Dale Visser
 */
public class Verify extends ConsoleCommand {
	private static final Logger LOGGER = LoggerFactory.getLogger(Verify.class);

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
		return PrintHelp.USAGE + "verify <file-or-url>\n"
				+ "  <file-or-url>   The path or URL identifying the data file\n"
				+ "Verifies the validity of the specified data file\n";
	}

	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 */
	public Verify(ConsoleIO consoleIO) {
		super(consoleIO);
	}

	@Override
	public void execute(String... tokens) {
		if (tokens.length != 2) {
			consoleIO.writeln(getHelpLong());
			return;
		}
		String dataPath = parseDataPath(tokens);
		try {
			URL dataURL = new URL(dataPath);
			RDFFormat format = Rio.getParserFormatForFileName(dataPath).orElseThrow(Rio.unsupportedFormat(dataPath));
			consoleIO.writeln("RDF Format is " + format.getName());

			RDFParser parser = Rio.createParser(format);
			VerificationListener listener = new VerificationListener(consoleIO);

			parser.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
			parser.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, true);

			parser.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, true);
			parser.set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);

			parser.set(BasicParserSettings.VERIFY_RELATIVE_URIS, true);
			parser.set(BasicParserSettings.VERIFY_URI_SYNTAX, true);

			parser.setParseErrorListener(listener);
			parser.setRDFHandler(listener);
			consoleIO.writeln("Verifying data...");

			try (InputStream dataStream = dataURL.openStream()) {
				parser.parse(dataStream, "urn://openrdf.org/RioVerifier/");
			}

			int warnings = listener.getWarnings();
			int errors = listener.getErrors();

			if (warnings + errors > 0) {
				consoleIO.writeError("Found " + warnings + " warnings and " + errors + " errors");
			} else {
				consoleIO.writeln("Data verified, no errors were found");
			}
			if (errors == 0) {
				consoleIO.writeln("File contains " + listener.getStatements() + " statements");
			}
		} catch (MalformedURLException e) {
			consoleIO.writeError("Malformed URL: " + dataPath);
		} catch (IOException e) {
			consoleIO.writeError("Failed to load data: " + e.getMessage());
		} catch (UnsupportedRDFormatException e) {
			consoleIO.writeError("No parser available for this RDF format");
		} catch (RDFParseException e) {
			consoleIO.writeError("Unexpected RDFParseException" + e.getMessage());
		} catch (RDFHandlerException e) {
			consoleIO.writeError("Unable to verify : " + e.getMessage());
			LOGGER.error("Unable to verify data file", e);
		}
	}

	/**
	 * Parse URL or path to local file. Only the second (index 1) token will be parsed, files will be prefixed with
	 * "file:" scheme
	 * 
	 * @param tokens array of tokens to be parsed
	 * @return URL path as string
	 */
	private String parseDataPath(String... tokens) {
		StringBuilder dataPath = new StringBuilder(tokens[1]);
		try {
			new URL(dataPath.toString());
			// dataPath is a URI
		} catch (MalformedURLException e) {
			// File path specified, convert to URL
			dataPath.insert(0, "file:");
		}
		return dataPath.toString();
	}
}
