/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.io.Files;

/**
 * @author Bart Hanssens
 */
public class ExportTest extends AbstractCommandTest {

	private static final String MEMORY_MEMBER = "alienquads";

	private Export cmd;

	@BeforeEach
	public void setUp() throws IOException, RDF4JException {
		manager = new LocalRepositoryManager(locationFile);

		addRepositories("export", MEMORY_MEMBER);

		when(mockConsoleIO.askProceed("File exists, continue ?", false)).thenReturn(Boolean.TRUE);
		when(mockConsoleState.getManager()).thenReturn(manager);
		when(mockConsoleState.getRepository()).thenReturn(manager.getRepository(MEMORY_MEMBER));

		cmd = new Export(mockConsoleIO, mockConsoleState, defaultSettings);
	}

	@Test
	public final void testExportAll() throws RepositoryException, IOException {
		File nq = new File(locationFile, "all.nq");
		cmd.execute("export", nq.getAbsolutePath());
		Model exp;
		try (Reader reader = Files.newReader(nq, StandardCharsets.UTF_8)) {
			exp = Rio.parse(reader, "http://example.com", RDFFormat.NQUADS);
		}
		assertNotNull(exp);
		assertTrue(nq.length() > 0, "File is empty");
		assertEquals(3, exp.contexts().size(), "Number of contexts incorrect");

		nq.delete();
	}

	@Test
	public final void testExportWorkDir() throws RepositoryException, IOException {
		setWorkingDir(cmd);

		File nq = new File(locationFile, "all.nq");
		cmd.execute("export", nq.getName());
		Model exp;
		try (Reader reader = Files.newReader(nq, StandardCharsets.UTF_8)) {
			exp = Rio.parse(reader, "http://example.com", RDFFormat.NQUADS);
		}
		assertNotNull(exp);
		assertTrue(nq.length() > 0, "File is empty");
		assertEquals(3, exp.contexts().size(), "Number of contexts incorrect");
	}

	@Test
	public final void testExportContexts() throws RepositoryException, IOException {
		File nq = new File(locationFile, "default.nq");
		cmd.execute("export", nq.getAbsolutePath(), "null", "http://example.org/ns/context/resurrection");
		Model exp;
		try (Reader reader = Files.newReader(nq, StandardCharsets.UTF_8)) {
			exp = Rio.parse(reader, "http://example.com", RDFFormat.NQUADS);
		}
		assertNotNull(exp);
		assertTrue(nq.length() > 0, "File is empty");
		assertEquals(2, exp.contexts().size(), "Number of contexts incorrect");
		assertEquals(4, exp.size(), "Number of triples incorrect");
	}
}
