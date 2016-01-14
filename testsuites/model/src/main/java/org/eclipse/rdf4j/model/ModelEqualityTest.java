/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.RDFParser.DatatypeHandling;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.Test;

/**
 * @author Arjohn Kampman
 */
public abstract class ModelEqualityTest {

	public static final String TESTCASES_DIR = "/testcases/model/equality/";

	@Test
	public void testTest001()
		throws Exception
	{
		testFilesEqual("test001a.ttl", "test001b.ttl");
	}

	@Test
	public void testFoafExampleAdvanced()
		throws Exception
	{
		testFilesEqual("foaf-example-advanced.rdf", "foaf-example-advanced.rdf");
	}

	@Test
	public void testSparqlGraph11()
		throws Exception
	{
		testFilesEqual("sparql-graph-11.ttl", "sparql-graph-11.ttl");
	}

	@Test
	public void testBlankNodeGraphs()
		throws Exception
	{
		testFilesEqual("toRdf-0061-out.nq", "toRdf-0061-out.nq");
	}

	// public void testSparqlGraph11Shuffled()
	// throws Exception
	// {
	// testFilesEqual("sparql-graph-11.ttl", "sparql-graph-11-shuffled.ttl");
	// }

	// public void testSparqlGraph11Shuffled2()
	// throws Exception
	// {
	// testFilesEqual("sparql-graph-11-shuffled.ttl", "sparql-graph-11.ttl");
	// }

	// public void testPhotoData()
	// throws Exception
	// {
	// testFilesEqual("photo-data.rdf", "photo-data.rdf");
	// }

	private void testFilesEqual(String file1, String file2)
		throws Exception
	{
		Set<Statement> model1 = loadModel(file1);
		Set<Statement> model2 = loadModel(file2);

		// long startTime = System.currentTimeMillis();
		boolean modelsEqual = Models.isomorphic(model1, model2);
		// long endTime = System.currentTimeMillis();
		// System.out.println("Model equality checked in " + (endTime - startTime)
		// + "ms (" + file1 + ", " + file2
		// + ")");

		assertTrue(modelsEqual);
	}

	private Model loadModel(String fileName)
		throws Exception
	{
		URL modelURL = this.getClass().getResource(TESTCASES_DIR + fileName);
		assertNotNull("Test file not found: " + fileName, modelURL);

		Model model = createEmptyModel();
		
		Optional<RDFFormat> rdfFormat = Rio.getParserFormatForFileName(fileName);
		assertTrue("Unable to determine RDF format for file: " + fileName, rdfFormat.isPresent());

		RDFParser parser = Rio.createParser(rdfFormat.get());
		parser.setDatatypeHandling(DatatypeHandling.IGNORE);
		parser.setPreserveBNodeIDs(true);
		parser.setRDFHandler(new StatementCollector(model));

		InputStream in = modelURL.openStream();
		try {
			parser.parse(in, modelURL.toString());
			return model;
		}
		finally {
			in.close();
		}
	}

	protected abstract Model createEmptyModel();
}
