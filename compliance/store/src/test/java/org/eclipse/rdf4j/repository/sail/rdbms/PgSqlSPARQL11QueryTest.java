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
//import junit.framework.Test;
//
//import org.eclipse.rdf4j.query.Dataset;
//import org.eclipse.rdf4j.query.parser.sparql.SPARQL11ManifestTest;
//import org.eclipse.rdf4j.query.parser.sparql.SPARQLQueryTest;
//import org.eclipse.rdf4j.repository.Repository;
//import org.eclipse.rdf4j.repository.dataset.DatasetRepository;
//import org.eclipse.rdf4j.repository.sail.SailRepository;
//import org.eclipse.rdf4j.sail.memory.MemoryStore;
//import org.eclipse.rdf4j.sail.rdbms.postgresql.PgSqlStore;
//
//public class PgSqlSPARQL11QueryTest extends SPARQLQueryTest {
//
//	public static Test suite()
//		throws Exception
//	{
//		return SPARQL11ManifestTest.suite(new Factory() {
//
//			public PgSqlSPARQL11QueryTest createSPARQLQueryTest(String testURI, String name,
//					String queryFileURL, String resultFileURL, Dataset dataSet, boolean laxCardinality)
//			{
//				return createSPARQLQueryTest(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, false);
//			}
//			
//			public PgSqlSPARQL11QueryTest createSPARQLQueryTest(String testURI, String name,
//					String queryFileURL, String resultFileURL, Dataset dataSet, boolean laxCardinality, boolean checkOrder)
//			{
//				return new PgSqlSPARQL11QueryTest(testURI, name, queryFileURL, resultFileURL, dataSet,
//						laxCardinality, checkOrder);
//			}
//		});
//	}
//
//	protected PgSqlSPARQL11QueryTest(String testURI, String name, String queryFileURL, String resultFileURL,
//			Dataset dataSet, boolean laxCardinality)
//	{
//		this(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, false);
//	}
//
//	protected PgSqlSPARQL11QueryTest(String testURI, String name, String queryFileURL, String resultFileURL,
//			Dataset dataSet, boolean laxCardinality, boolean checkOrder)
//	{
//		super(testURI, name, queryFileURL, resultFileURL, dataSet, laxCardinality, checkOrder);
//	}
//	
//	protected Repository newRepository() {
//		PgSqlStore sail = new PgSqlStore("sesame_test");
//		sail.setUser("sesame");
//		sail.setPassword("opensesame");
//		return new DatasetRepository(new SailRepository(sail));
//	}
//}
