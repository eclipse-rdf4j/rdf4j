package org.eclipse.rdf4j.repository.sail.rdbms;
/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
// SES-1071 disabling rdbms-based tests
//package org.eclipse.rdf4j.repository.sail.rdbms;
//
//import java.io.IOException;
//
//import org.eclipse.rdf4j.repository.Repository;
//import org.eclipse.rdf4j.repository.RepositoryConnectionTest;
//import org.eclipse.rdf4j.repository.sail.SailRepository;
//import org.eclipse.rdf4j.sail.rdbms.mysql.MySqlStore;
//
//public class MySqlStoreConnectionTest extends RepositoryConnectionTest {
//
//	public MySqlStoreConnectionTest(String name) {
//		super(name);
//	}
//
//	@Override
//	protected Repository createRepository()
//		throws IOException
//	{
//		MySqlStore sail = new MySqlStore("sesame_test");
//		sail.setUser("sesame");
//		sail.setPassword("opensesame");
//		return new SailRepository(sail);
//	}
//
//	@Override
//	public void testOrderByQueriesAreInterruptable() {
//		System.err.println("temporarily disabled testOrderByQueriesAreInterruptable() for RDBMS store");
//	}
//}
