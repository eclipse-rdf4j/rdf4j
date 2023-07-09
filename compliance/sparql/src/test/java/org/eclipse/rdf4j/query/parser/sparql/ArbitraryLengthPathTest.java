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
package org.eclipse.rdf4j.query.parser.sparql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author james
 */
public class ArbitraryLengthPathTest {

	private Repository repo;

	private RepositoryConnection con;

	@BeforeEach
	public void setUp() {
		repo = new SailRepository(new MemoryStore());
		con = repo.getConnection();
	}

	@AfterEach
	public void tearDown() {
		con.close();
		repo.shutDown();
	}

	@Test
	public void test10() {
		populate(10);
		String sparql = "ASK { <urn:test:root> <urn:test:hasChild>* <urn:test:node-end> }";
		assertTrue(con.prepareBooleanQuery(QueryLanguage.SPARQL, sparql).evaluate());
	}

	@Test
	public void test100() {
		populate(100);
		String sparql = "ASK { <urn:test:root> <urn:test:hasChild>* <urn:test:node-end> }";
		assertTrue(con.prepareBooleanQuery(QueryLanguage.SPARQL, sparql).evaluate());
	}

	@Test
	public void test1000() {
		populate(1000);
		String sparql = "ASK { <urn:test:root> <urn:test:hasChild>* <urn:test:node-end> }";
		assertTrue(con.prepareBooleanQuery(QueryLanguage.SPARQL, sparql).evaluate());
	}

	@Test
	public void test10000() {
		populate(10000);
		String sparql = "ASK { <urn:test:root> <urn:test:hasChild>* <urn:test:node-end> }";
		assertTrue(con.prepareBooleanQuery(QueryLanguage.SPARQL, sparql).evaluate());
	}

	@Test
	public void test100000() {
		populate(100000);
		String sparql = "ASK { <urn:test:root> <urn:test:hasChild>* <urn:test:node-end> }";
		assertTrue(con.prepareBooleanQuery(QueryLanguage.SPARQL, sparql).evaluate());
	}

	@Test
	public void testDirection() {
		ValueFactory vf = con.getValueFactory();
		con.add(vf.createIRI("urn:test:a"), vf.createIRI("urn:test:rel"), vf.createIRI("urn:test:b"));
		con.add(vf.createIRI("urn:test:b"), vf.createIRI("urn:test:rel"), vf.createIRI("urn:test:a"));
		String sparql = "ASK { <urn:test:a> <urn:test:rel>* <urn:test:b> . <urn:test:b> <urn:test:rel>* <urn:test:a> }";
		assertTrue(con.prepareBooleanQuery(QueryLanguage.SPARQL, sparql).evaluate());
	}

	@Test
	public void testSimilarPatterns() {
		ValueFactory vf = con.getValueFactory();
		con.add(vf.createIRI("urn:test:a"), RDF.TYPE, vf.createIRI("urn:test:c"));
		con.add(vf.createIRI("urn:test:b"), RDF.TYPE, vf.createIRI("urn:test:d"));
		con.add(vf.createIRI("urn:test:c"), RDFS.SUBCLASSOF, vf.createIRI("urn:test:e"));
		con.add(vf.createIRI("urn:test:d"), RDFS.SUBCLASSOF, vf.createIRI("urn:test:f"));
		String sparql = "ASK { \n"
				+ "   values (?expectedTargetClass55555 ?expectedTargetClass5544T) {(<urn:test:e> <urn:test:f>)}.\n"
				+ "   <urn:test:a> a ?linkTargetClass55555 .\n"
				+ "   ?linkTargetClass55555 rdfs:subClassOf* ?expectedTargetClass55555 .\n"
				+ "   <urn:test:b> a ?linkTargetClass55556 .\n"
				+ "   ?linkTargetClass55556 rdfs:subClassOf* ?expectedTargetClass5544T . }";
		assertTrue(con.prepareBooleanQuery(QueryLanguage.SPARQL, sparql).evaluate());
	}

	private void populate(int n) throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		for (int i = 0; i < n; i++) {
			con.add(vf.createIRI("urn:test:root"), vf.createIRI("urn:test:hasChild"),
					vf.createIRI("urn:test:node" + i));
		}
		con.add(vf.createIRI("urn:test:root"), vf.createIRI("urn:test:hasChild"), vf.createIRI("urn:test:node-end"));
	}

}
