/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Distribution License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sparql;

import junit.framework.TestCase;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Jeen Broekstra
 */
//public class SPARQLRepositorySparqlUpdateTest extends SPARQLUpdateTest {
//
//	private HTTPMemServer server;
//
//	@Override
//	public void setUp()
//		throws Exception
//	{
//		server = new HTTPMemServer();
//		
//		try {
//			server.start();
//			super.setUp();
//		}
//		catch (Exception e) {
//			server.stop();
//			throw e;
//		}
//	}
//
//	@Override
//	public void tearDown()
//		throws Exception
//	{
//		super.tearDown();
//		server.stop();
//	}
//
//	@Override
//	protected Repository newRepository()
//		throws Exception
//	{
//		return new SPARQLRepository(HTTPMemServer.REPOSITORY_URL, HTTPMemServer.REPOSITORY_URL + "/statements");
//	}
//
//	@Ignore
//	@Test
//	@Override
//	public void testAutoCommitHandling() 
//	{
//		// transaction isolation is not supported for HTTP connections. disabling test.
//		System.err.println("temporarily disabled testAutoCommitHandling() for HTTPRepository");
//	}
//}
public class SPARQLRepositorySparqlUpdateTest extends TestCase {

	private Repository m_repository;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		m_repository = new SailRepository(new MemoryStore());
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		m_repository.shutDown();
	}

	// @Test (expected = org.eclipse.rdf4j.rio.RDFParseException.class)
	@Test
	public void testInvalidUpdate() {
		RepositoryConnection connection = m_repository.getConnection();
		try {
			Update update = connection.prepareUpdate(QueryLanguage.SPARQL, "insert data { ?s ?p ?o }");
		} catch (RDFParseException rdfpe) {
			Assert.assertEquals(7, rdfpe.getLineNumber());
		}

	}

	// @Test (expected = org.eclipse.rdf4j.rio.RDFParseException.class)
	@Test
	public void testInvalidDeleteUpdate() {
		RepositoryConnection connection = m_repository.getConnection();
		try {
			Update update = connection.prepareUpdate(QueryLanguage.SPARQL, "delete data { ?s ?p ?o }");
		} catch (RDFParseException rdfpe) {
			Assert.assertEquals(7, rdfpe.getLineNumber());
		}
	}
}