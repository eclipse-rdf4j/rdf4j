/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dale Visser
 */
public class Verify implements Command {

	private static final Logger LOGGER = LoggerFactory.getLogger(Verify.class);

	private final ConsoleIO consoleIO;

	Verify(ConsoleIO consoleIO) {
		this.consoleIO = consoleIO;
	}

	public void execute(String... tokens) {
		if (tokens.length != 2) {
			consoleIO.writeln(PrintHelp.VERIFY);
			return;
		}
		String dataPath = parseDataPath(tokens);
		try {
			final URL dataURL = new URL(dataPath);
			final RDFFormat format = Rio.getParserFormatForFileName(dataPath).orElseThrow(
					Rio.unsupportedFormat(dataPath));
			consoleIO.writeln("RDF Format is " + format.getName());
			final RDFParser parser = Rio.createParser(format);
			final VerificationListener listener = new VerificationListener(consoleIO);
			parser.setDatatypeHandling(RDFParser.DatatypeHandling.VERIFY);
			parser.setVerifyData(true);
			parser.setParseErrorListener(listener);
			parser.setRDFHandler(listener);
			consoleIO.writeln("Verifying data...");
			final InputStream dataStream = dataURL.openStream();
			try {
				parser.parse(dataStream, "urn://openrdf.org/RioVerifier/");
			}
			finally {
				dataStream.close();
			}
			final int warnings = listener.getWarnings();
			final int errors = listener.getErrors();
			if (warnings + errors > 0) {
				consoleIO.writeln("Found " + warnings + " warnings and " + errors + " errors");
			}
			else {
				consoleIO.writeln("Data verified, no errors were found");
			}
			if (errors == 0) {
				consoleIO.writeln("File contains " + listener.getStatements() + " statements");
			}
		}
		catch (MalformedURLException e) {
			consoleIO.writeError("Malformed URL: " + dataPath);
		}
		catch (IOException e) {
			consoleIO.writeError("Failed to load data: " + e.getMessage());
		}
		catch (UnsupportedRDFormatException e) {
			consoleIO.writeError("No parser available for this RDF format");
		}
		catch (RDFParseException e) {
			LOGGER.error("Unexpected RDFParseException", e);
		}
		catch (RDFHandlerException e) {
			consoleIO.writeError("Unable to verify : " + e.getMessage());
			LOGGER.error("Unable to verify data file", e);
		}
	}

	private String parseDataPath(String... tokens) {
		StringBuilder dataPath = new StringBuilder(tokens[1]);
		try {
			new URL(dataPath.toString());
			// dataPath is a URI
		}
		catch (MalformedURLException e) {
			// File path specified, convert to URL
			dataPath.insert(0, "file:");
		}
		return dataPath.toString();
	}

}
