/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.console.setting.ConsoleSetting;
import org.eclipse.rdf4j.console.setting.WorkDir;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test SPARQL command
 * 
 * @author Bart Hanssens
 */
public class SparqlTest extends AbstractCommandTest {
	private static final String MEMORY_MEMBER = "alien";
	
	private Sparql sparql;
	
	@Rule
	public final TemporaryFolder LOCATION = new TemporaryFolder();

	@Before
	public void setUp() throws IOException, RDF4JException {
		manager = new LocalRepositoryManager(LOCATION.getRoot());
		manager.initialize();
		
		addRepositories("sparql", MEMORY_MEMBER);
		
		Map<String,ConsoleSetting> settings = new HashMap<>();
		settings.put(WorkDir.NAME, new WorkDir(LOCATION.getRoot().toPath()));
		
		TupleAndGraphQueryEvaluator tqe = 
			new TupleAndGraphQueryEvaluator(mockConsoleIO, mockConsoleState, settings);
		when(mockConsoleState.getRepository()).thenReturn(manager.getRepository(MEMORY_MEMBER));

		sparql = new Sparql(tqe);
	}

	@After
	@Override
	public void tearDown() {
		manager.shutDown();
	}
	
	@Test
	public final void testSelectError() throws IOException {
		sparql.executeQuery("select ?s ?p ?o where { ?s ?p ?o }", "select");
		verify(mockConsoleIO, never()).writeError(anyString());
	}
	
	@Test
	public final void testInputFile() throws IOException {
		File f = LOCATION.newFile("select.qr");
		copyFromResource("sparql/select.qr", f);
		
		sparql.executeQuery("sparql INFILE=\"selct.qr\"", "sparql");
		verify(mockConsoleIO, never()).writeError(anyString());
	}
	
	@Test
	public final void testOutputFileConstruct() throws IOException {
		sparql.executeQuery("sparql OUTFILE=\"out.ttl\" construct { ?s ?p ?o } where { ?s ?p ?o }", "sparql");
		verify(mockConsoleIO, never()).writeError(anyString());
		
		String dir = LOCATION.getRoot().toString();
		File f = Paths.get(dir, "out.ttl").toFile();
		
		assertTrue("File does not exist", f.exists());
		assertTrue("Empty file", f.length() > 0);
	}
	
	@Test
	public final void testOutputFileWrongFormat() throws IOException {
		// SELECT should use sparql result format, not triple file
		sparql.executeQuery("sparql OUTFILE=\"out.ttl\" select ?s ?p ?o where { ?s ?p ?o }", "sparql");
		
		verify(mockConsoleIO).writeError("No suitable result writer found");
	}
}
