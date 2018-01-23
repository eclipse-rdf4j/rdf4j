/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

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
public class Export implements Command {
	private static final Logger LOGGER = LoggerFactory.getLogger(Export.class);

	private final ConsoleIO consoleIO;
	private final ConsoleState state;

	// TODO: move this util class, could be reused by Clear and other commands
	private static Resource getContext(Repository repository, String ctxID) {
		if (ctxID.equalsIgnoreCase("null")) {
			return null;
		}
		if (ctxID.startsWith("_:")) {
			return repository.getValueFactory().createBNode(ctxID.substring(2));
		}
		return repository.getValueFactory().createIRI(ctxID);
	}
	

	@Override
	public void execute(String... tokens) {
		Repository repository = state.getRepository();

		if (repository == null) {
			consoleIO.writeUnopenedError();
			return;
		}

		if (tokens.length < 2) {
			consoleIO.writeln(PrintHelp.EXPORT);
			return;
		} 
		
		String file = tokens[1];
		Resource[] contexts = new Resource[]{};

		if (tokens.length > 2) {
			contexts = new Resource[tokens.length - 2];
			for (int i = 2; i < tokens.length; i++) {
				try {
					contexts[i - 2] = getContext(repository, tokens[i]);
				} catch (IllegalArgumentException ioe) {
					consoleIO.writeError("Illegal URI: " + tokens[i]);
					consoleIO.writeln(PrintHelp.EXPORT);
					return;
				}
			}
		}
		export(repository, file, contexts);
	}

	/**
	 * 
	 * @param repository
	 * @param baseURI
	 * @param context
	 * @param tokens 
	 * @throws UnsupportedRDFormatException
	 */
	private void export(Repository repository, String file, Resource...contexts) {
		try (	RepositoryConnection conn = repository.getConnection();
				Writer w = Files.newBufferedWriter(Paths.get(file), StandardCharsets.UTF_8, 
															StandardOpenOption.TRUNCATE_EXISTING)) {
			
			RDFFormat fmt = Rio.getWriterFormatForFileName(file).orElseThrow(() ->
								new UnsupportedRDFormatException("No RDF parser for " + file));
			RDFWriter writer = Rio.createWriter(fmt, w);
			
			long startTime = System.nanoTime();
			consoleIO.writeln("Exporting data...");

			conn.export(writer, contexts);

			long diff = (System.nanoTime() - startTime) / 1_000_000;
			consoleIO.writeln("Data has been written to file (" + diff + " ms)");
		} catch (IOException e) {
			consoleIO.writeError("Failed to export data: " + e.getMessage());
		}
	}

	/**
	 * Constructor
	 * 
	 * @param consoleIO
	 * @param state
	 * @param lockRemover
	 */
	Export(ConsoleIO consoleIO, ConsoleState state) {
		this.consoleIO = consoleIO;
		this.state = state;
	}
}
