/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.console.Util;
import org.eclipse.rdf4j.console.setting.ConsoleSetting;
import org.eclipse.rdf4j.console.setting.WorkDir;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;

/**
 * Convert RDF file from one format to another
 *
 * @author Bart Hanssens
 */
public class Convert extends ConsoleCommand {
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
	public String[] usesSettings() {
		return new String[] { WorkDir.NAME };
	}

	/**
	 * Constructor
	 *
	 * @param consoleIO
	 * @param state
	 * @param settings
	 */
	public Convert(ConsoleIO consoleIO, ConsoleState state, Map<String, ConsoleSetting> settings) {
		super(consoleIO, state, settings);
	}

	@Override
	public void execute(String... tokens) {
		if (tokens.length < 3) {
			writeln(getHelpLong());
			return;
		}

		convert(tokens[1], tokens[2]);
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
	 * Convert a file
	 *
	 * @param fileFrom file name
	 * @param fileTo   file name
	 */
	private void convert(String fileFrom, String fileTo) {
		// check from
		Path pathFrom = Util.getNormalizedPath(getWorkDir(), fileFrom);
		if (pathFrom == null) {
			writeError("Invalid file name (from) " + fileFrom);
			return;
		}
		if (Files.notExists(pathFrom)) {
			writeError("File not found (from) " + fileFrom);
			return;
		}
		Optional<RDFFormat> fmtFrom = Rio.getParserFormatForFileName(fileFrom);
		if (!fmtFrom.isPresent()) {
			writeError("No RDF parser for " + fileFrom);
			return;
		}

		// check to
		Path pathTo = Util.getNormalizedPath(getWorkDir(), fileTo);
		if (pathTo == null) {
			writeError("Invalid file name (to) " + pathTo);
			return;
		}
		Optional<RDFFormat> fmtTo = Rio.getWriterFormatForFileName(fileTo);
		if (!fmtTo.isPresent()) {
			writeError("No RDF writer for " + fileTo);
			return;
		}
		if (Files.exists(pathTo)) {
			boolean overwrite = askProceed("File exists, continue ?", false);
			if (!overwrite) {
				writeln("Conversion aborted");
				return;
			}
		}

		RDFParser parser = Rio.createParser(fmtFrom.get());
		String baseURI = pathFrom.toUri().toString();

		try (BufferedInputStream r = new BufferedInputStream(Files.newInputStream(pathFrom));
				BufferedWriter w = Files.newBufferedWriter(pathTo)) {
			RDFWriter writer = Rio.createWriter(fmtTo.get(), w);
			parser.setRDFHandler(writer);

			long startTime = System.nanoTime();
			writeln("Converting file ...");

			parser.parse(r, baseURI);

			long diff = (System.nanoTime() - startTime) / 1_000_000;
			writeln("Data has been written to file (" + diff + " ms)");
		} catch (IOException | RDFParseException | RDFHandlerException e) {
			writeError("Failed to convert data", e);
		}
	}
}
