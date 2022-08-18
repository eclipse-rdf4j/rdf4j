/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

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

package org.eclipse.rdf4j.repository.sparql;
