/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Bart Hanssens
 */
public class ExportTest extends AbstractCommandTest {

	private static final String MEMORY_MEMBER = "alienquads";

	private Export cmd;

	@Before
	public void setUp() throws IOException, RDF4JException {
		manager = new LocalRepositoryManager(LOCATION.getRoot());

		addRepositories("export", MEMORY_MEMBER);

		when(mockConsoleIO.askProceed("File exists, continue ?", false)).thenReturn(Boolean.TRUE);
		when(mockConsoleState.getManager()).thenReturn(manager);
		when(mockConsoleState.getRepository()).thenReturn(manager.getRepository(MEMORY_MEMBER));

		cmd = new Export(mockConsoleIO, mockConsoleState, defaultSettings);
	}

	@Test
	public final void testExportAll() throws RepositoryException, IOException {
		File nq = LOCATION.newFile("all.nq");
		cmd.execute("export", nq.getAbsolutePath());
		Model exp = Rio.parse(Files.newReader(nq, StandardCharsets.UTF_8), "http://example.com", RDFFormat.NQUADS);

		assertTrue("File is empty", nq.length() > 0);
		assertEquals("Number of contexts incorrect", 3, exp.contexts().size());

		nq.delete();
	}

	@Test
	public final void testExportWorkDir() throws RepositoryException, IOException {
		setWorkingDir(cmd);

		File nq = LOCATION.newFile("all.nq");
		cmd.execute("export", nq.getName());
		Model exp = Rio.parse(Files.newReader(nq, StandardCharsets.UTF_8), "http://example.com", RDFFormat.NQUADS);

		assertTrue("File is empty", nq.length() > 0);
		assertEquals("Number of contexts incorrect", 3, exp.contexts().size());
	}

	@Test
	public final void testExportContexts() throws RepositoryException, IOException {
		File nq = LOCATION.newFile("default.nq");
		cmd.execute("export", nq.getAbsolutePath(), "null", "http://example.org/ns/context/resurrection");
		Model exp = Rio.parse(Files.newReader(nq, StandardCharsets.UTF_8), "http://example.com", RDFFormat.NQUADS);

		assertTrue("File is empty", nq.length() > 0);

		assertEquals("Number of contexts incorrect", 2, exp.contexts().size());
		assertEquals("Number of triples incorrect", 4, exp.size());
	}
}
