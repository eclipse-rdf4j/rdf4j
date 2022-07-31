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

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.console.Util;
import org.eclipse.rdf4j.console.setting.ConsoleSetting;
import org.eclipse.rdf4j.console.setting.WorkDir;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;

/**
 * Export triples to file
 *
 * @author Bart Hanssens
 */
public class Export extends ConsoleCommand {
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
	public Export(ConsoleIO consoleIO, ConsoleState state, Map<String, ConsoleSetting> settings) {
		super(consoleIO, state, settings);
	}

	@Override
	public void execute(String... tokens) {
		Repository repository = state.getRepository();

		if (repository == null) {
			writeUnopenedError();
			return;
		}
		if (tokens.length < 2) {
			writeln(getHelpLong());
			return;
		}

		String fileName = tokens[1];

		Resource[] contexts;
		try {
			contexts = Util.getContexts(tokens, 2, repository);
		} catch (IllegalArgumentException ioe) {
			writeError(ioe.getMessage());
			return;
		}
		export(repository, fileName, contexts);
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
	 * Export to a file
	 *
	 * @param repository repository to export
	 * @param fileName   file name
	 * @param context    context(s) (if any)
	 * @throws UnsupportedRDFormatException
	 */
	private void export(Repository repository, String fileName, Resource... contexts) {
		Path path = Util.getNormalizedPath(getWorkDir(), fileName);
		if (path == null) {
			writeError("Invalid file name " + fileName);
			return;
		}

		if (path.toFile().exists()) {
			boolean overwrite = askProceed("File exists, continue ?", false);
			if (!overwrite) {
				writeln("Export aborted");
				return;
			}
		}

		try (RepositoryConnection conn = repository.getConnection();
				Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING)) {

			RDFFormat fmt = Rio.getWriterFormatForFileName(fileName)
					.orElseThrow(() -> new UnsupportedRDFormatException("No RDF parser for " + fileName));
			RDFWriter writer = Rio.createWriter(fmt, w);

			long startTime = System.nanoTime();
			writeln("Exporting data...");

			conn.export(writer, contexts);

			long diff = (System.nanoTime() - startTime) / 1_000_000;
			writeln("Data has been written to file (" + diff + " ms)");
		} catch (IOException | UnsupportedRDFormatException e) {
			writeError("Failed to export data", e);
		}
	}
}
