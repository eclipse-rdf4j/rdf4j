/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.console.Util;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Export triples to file
 * 
 * @author Bart Hanssens
 */
public class Export extends ConsoleCommand {
	private static final Logger LOGGER = LoggerFactory.getLogger(Export.class);

	@Override
	public String getName() {
		return "export";
	}

	@Override
	public String getHelpShort() {
		return "Exports repository data to a file";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE + "export <file>                 Exports the entirey repository to a file\n"
				+ "export <file> (<uri>|null)... Exports the specified context(s) to a file\n";
	}

	@Override
	public void execute(String... tokens) {
		Repository repository = state.getRepository();

		if (repository == null) {
			consoleIO.writeUnopenedError();
			return;
		}
		if (tokens.length < 2) {
			consoleIO.writeln(getHelpLong());
			return;
		}

		String fileName = tokens[1];

		Resource[] contexts;
		try {
			contexts = Util.getContexts(tokens, 2, repository);
		} catch (IllegalArgumentException ioe) {
			consoleIO.writeError(ioe.getMessage());
			return;
		}
		export(repository, fileName, contexts);
	}

	/**
	 * Export to a file
	 * 
	 * @param repository repository to export
	 * @param fileName   file name
	 * @param context    context(s) (if any)
	 * @throws UnsupportedRDFormatException
	 */
	private void export(Repository repository, String fileName, Resource... contexts) {
		Path path = Util.getPath(fileName);
		if (path == null) {
			consoleIO.writeError("Invalid file name");
			return;
		}

		if (path.toFile().exists()) {
			try {
				boolean overwrite = consoleIO.askProceed("File exists, continue ?", false);
				if (!overwrite) {
					consoleIO.writeln("Export aborted");
					return;
				}
			} catch (IOException ioe) {
				consoleIO.writeError("I/O error " + ioe.getMessage());
			}
		}

		try (RepositoryConnection conn = repository.getConnection();
				Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING)) {

			RDFFormat fmt = Rio.getWriterFormatForFileName(fileName)
					.orElseThrow(() -> new UnsupportedRDFormatException("No RDF parser for " + fileName));
			RDFWriter writer = Rio.createWriter(fmt, w);

			long startTime = System.nanoTime();
			consoleIO.writeln("Exporting data...");

			conn.export(writer, contexts);

			long diff = (System.nanoTime() - startTime) / 1_000_000;
			consoleIO.writeln("Data has been written to file (" + diff + " ms)");
		} catch (IOException | UnsupportedRDFormatException e) {
			consoleIO.writeError("Failed to export data: " + e.getMessage());
		}
	}

	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 */
	public Export(ConsoleIO consoleIO, ConsoleState state) {
		super(consoleIO, state);
	}
}
