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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
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

	@Override
	public  String getName() {
		return "export";
	}
	
	@Override
	public String getHelpShort() {
		return "Exports repository data to a file";
	}

	@Override
	public String getHelpLong() {
		return PrintHelp.USAGE
			+ "export <file>                 Exports the entirey repository to a file\n"
			+ "export <file> (<uri>|null)... Exports the specified context(s) to a file\n";
	}
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
	
	/**
	 * Get path from file or URI
	 * 
	 * @param file file name
	 * @return path or null
	 */
	private static Path getPath(String file) {
		Path path = null;
		try {
			path = Paths.get(file);
		} catch (InvalidPathException ipe) {
			try {
				path = Paths.get(new URI(file));
			} catch (URISyntaxException ex) { 
				//
			}
		}
		return path;
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
		Resource[] contexts = new Resource[]{};

		if (tokens.length > 2) {
			contexts = new Resource[tokens.length - 2];
			for (int i = 2; i < tokens.length; i++) {
				try {
					contexts[i - 2] = getContext(repository, tokens[i]);
				} catch (IllegalArgumentException ioe) {
					consoleIO.writeError("Illegal URI: " + tokens[i]);
					return;
				}
			}
		}
		export(repository, fileName, contexts);
	}

	/**
	 * Export to a file
	 * 
	 * @param repository repository to export
	 * @param fileName file name
	 * @param context context(s) (if any)
	 * @throws UnsupportedRDFormatException
	 */
	private void export(Repository repository, String fileName, Resource...contexts) {
		Path path = getPath(fileName);
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
		
		try (	RepositoryConnection conn = repository.getConnection();
				Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8, 
							StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			
			RDFFormat fmt = Rio.getWriterFormatForFileName(fileName).orElseThrow(() ->
								new UnsupportedRDFormatException("No RDF parser for " + fileName));
			RDFWriter writer = Rio.createWriter(fmt, w);

			long startTime = System.nanoTime();
			consoleIO.writeln("Exporting data...");

			conn.export(writer, contexts);

			long diff = (System.nanoTime() - startTime) / 1_000_000;
			consoleIO.writeln("Data has been written to file (" + diff + " ms)");
		} catch (IOException|UnsupportedRDFormatException e) {
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
