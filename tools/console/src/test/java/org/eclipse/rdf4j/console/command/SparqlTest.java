/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test SPARQL command
 *
 * @author Bart Hanssens
 */
public class SparqlTest extends AbstractCommandTest {
	private static final String MEMORY_MEMBER = "alien";

	private Sparql cmd;

	@BeforeEach
	public void setUp() throws IOException, RDF4JException {
		manager = new LocalRepositoryManager(locationFile);

		addRepositories("sparql", MEMORY_MEMBER);
		TupleAndGraphQueryEvaluator tqe = new TupleAndGraphQueryEvaluator(mockConsoleIO, mockConsoleState,
				defaultSettings);
		when(mockConsoleState.getRepository()).thenReturn(manager.getRepository(MEMORY_MEMBER));
		when(mockConsoleIO.askProceed("File exists, continue ?", false)).thenReturn(Boolean.TRUE);

		cmd = new Sparql(tqe);
	}

	@Test
	public final void testSelectError() throws IOException {
		cmd.executeQuery("select ?s ?p ?o where { ?s ?p ?o }", "select");
		verify(mockConsoleIO, never()).writeError(anyString());
	}

	@Test
	public final void testSelectMissingBindings() throws IOException {
		cmd.executeQuery("select ?s ?p ?o where { ?s a foaf:Organization }", "select");
		verify(mockConsoleIO, never()).writeError(anyString());
	}

	@Test
	public final void testInputFile() throws IOException {
		File f = new File(locationFile, "select.qr");
		copyFromResource("sparql/select.qr", f);

		cmd.executeQuery("sparql INFILE=\"" + f.getAbsolutePath() + "\"", "sparql");
		verify(mockConsoleIO, never()).writeError(anyString());
	}

	@Test
	public final void testInputFileWorkdir() throws IOException {
		setWorkingDir(cmd);

		File f = new File(locationFile, "select.qr");
		copyFromResource("sparql/select.qr", f);

		cmd.executeQuery("sparql INFILE=\"select.qr\"", "sparql");
		verify(mockConsoleIO, never()).writeError(anyString());
	}

	@Test
	public final void testOutputFileConstruct() throws IOException {
		File f = new File(locationFile, "out.ttl");

		cmd.executeQuery("sparql OUTFILE=\"" + f.getAbsolutePath() + "\" construct { ?s ?p ?o } where { ?s ?p ?o }",
				"sparql");
		verify(mockConsoleIO, never()).writeError(anyString());

		assertTrue(f.exists(), "File does not exist");
		assertTrue(f.length() > 0, "Empty file");
		Model m;
		try (Reader reader = new FileReader(f)) {
			m = Rio.parse(reader, "", RDFFormat.TURTLE);
		}
		assertNotNull(m);
		assertTrue(m.size() > 0, "Empty model");
	}

	@Test
	public final void testOutputFileConstructWorkdir() throws IOException {
		setWorkingDir(cmd);

		File f = new File(locationFile, "out.ttl");

		cmd.executeQuery("sparql OUTFILE=\"out.ttl\" construct { ?s ?p ?o } where { ?s ?p ?o }", "sparql");
		verify(mockConsoleIO, never()).writeError(anyString());

		assertTrue(f.exists(), "File does not exist");
		assertTrue(f.length() > 0, "Empty file");
		Model m;
		try (Reader reader = new FileReader(f)) {
			m = Rio.parse(reader, "", RDFFormat.TURTLE);
		}
		assertNotNull(m);
		assertTrue(m.size() > 0, "Empty model");
	}

	@Test
	public final void testOutputFileWrongFormat() throws IOException {
		File f = new File(locationFile, "out.ttl");

		// SELECT should use sparql result format, not a triple file format
		cmd.executeQuery("sparql OUTFILE=\"" + f.getAbsolutePath() + "\" select ?s ?p ?o where { ?s ?p ?o }",
				"sparql");

		verify(mockConsoleIO).writeError("No suitable result writer found");
	}

	@Test
	public final void testInputOutputFile() throws IOException {
		File fin = new File(locationFile, "select.qr");
		copyFromResource("sparql/select.qr", fin);

		File fout = new File(locationFile, "out.srj");

		cmd.executeQuery("sparql infile=\"" + fin.getAbsolutePath() + "\"" +
				" outfile=\"" + fout.getAbsolutePath() + "\"", "sparql");

		verify(mockConsoleIO, never()).writeError(anyString());
		assertFalse(mockConsoleIO.wasErrorWritten());

		assertTrue(fout.exists(), "File does not exist");
		assertTrue(fout.length() > 0, "Empty file");
	}

	@Test
	public final void testInputOutputFilePrefix() throws IOException {
		File fin = new File(locationFile, "select-prefix.qr");
		copyFromResource("sparql/select-prefix.qr", fin);

		File fout = new File(locationFile, "out.srj");

		cmd.executeQuery("sparql infile=\"" + fin.getAbsolutePath() + "\"" +
				" outfile=\"" + fout.getAbsolutePath() + "\"", "sparql");

		verify(mockConsoleIO, never()).writeError(anyString());
		assertFalse(mockConsoleIO.wasErrorWritten());

		assertTrue(fout.exists(), "File does not exist");
		assertTrue(fout.length() > 0, "Empty file");
	}
}
