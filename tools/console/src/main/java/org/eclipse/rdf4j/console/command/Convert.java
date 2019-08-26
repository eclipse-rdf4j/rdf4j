/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.console.Util;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convert RDF file from one format to another
 * 
 * @author Bart Hanssens
 */
public class Convert extends ConsoleCommand {
	private static final Logger LOGGER = LoggerFactory.getLogger(Convert.class);

	@Override
	public String getName() {
		return "convert";
	}

	@Override
	public String getHelpShort() {
		return "Converts a file from one RDF format to another";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE + "convert <fileFrom> <fileTo>   Converts a file from one RDF format to another\n";
	}

	@Override
	public void execute(String... tokens) {
		if (tokens.length < 3) {
			consoleIO.writeln(getHelpLong());
			return;
		}

		convert(tokens[1], tokens[2]);
	}

	/**
	 * Convert a file
	 * 
	 * @param fileFrom file name
	 * @param fileTo   file name
	 */
	private void convert(String fileFrom, String fileTo) {
		// check from
		Path pathFrom = Util.getPath(fileFrom);
		if (pathFrom == null) {
			consoleIO.writeError("Invalid file name (from) " + fileFrom);
			return;
		}
		if (Files.notExists(pathFrom)) {
			consoleIO.writeError("File not found (from) " + fileFrom);
			return;
		}
		Optional<RDFFormat> fmtFrom = Rio.getParserFormatForFileName(fileFrom);
		if (!fmtFrom.isPresent()) {
			consoleIO.writeError("No RDF parser for " + fileFrom);
			return;
		}

		// check to
		Path pathTo = Util.getPath(fileTo);
		if (pathTo == null) {
			consoleIO.writeError("Invalid file name (to) " + pathTo);
			return;
		}
		Optional<RDFFormat> fmtTo = Rio.getWriterFormatForFileName(fileTo);
		if (!fmtTo.isPresent()) {
			consoleIO.writeError("No RDF writer for " + fileTo);
			return;
		}
		if (Files.exists(pathTo)) {
			try {
				boolean overwrite = consoleIO.askProceed("File exists, continue ?", false);
				if (!overwrite) {
					consoleIO.writeln("Conversion aborted");
					return;
				}
			} catch (IOException ioe) {
				consoleIO.writeError("I/O error " + ioe.getMessage());
			}
		}

		RDFParser parser = Rio.createParser(fmtFrom.get());
		String baseURI = pathFrom.toUri().toString();

		try (BufferedInputStream r = new BufferedInputStream(Files.newInputStream(pathFrom));
				BufferedWriter w = Files.newBufferedWriter(pathTo)) {
			RDFWriter writer = Rio.createWriter(fmtTo.get(), w);
			parser.setRDFHandler(writer);

			long startTime = System.nanoTime();
			consoleIO.writeln("Converting file ...");

			parser.parse(r, baseURI);

			long diff = (System.nanoTime() - startTime) / 1_000_000;
			consoleIO.writeln("Data has been written to file (" + diff + " ms)");
		} catch (IOException | RDFParseException | RDFHandlerException e) {
			consoleIO.writeError("Failed to convert data: " + e.getMessage());
		}
	}

	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 */
	public Convert(ConsoleIO consoleIO, ConsoleState state) {
		super(consoleIO, state);
	}
}
