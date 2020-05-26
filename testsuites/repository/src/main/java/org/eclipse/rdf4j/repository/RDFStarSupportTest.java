/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

/**
 * Test cases for RDF* support in the Repository.
 *
 * @author Jeen Broekstra
 *
 */
public abstract class RDFStarSupportTest {
	/**
	 * Timeout all individual tests after 10 minutes.
	 */
	@Rule
	public Timeout to = new Timeout(10, TimeUnit.MINUTES);

	private Repository testRepository;

	private RepositoryConnection testCon;

	private ValueFactory vf;

	private BNode bob;

	private BNode alice;

	private BNode alexander;

	private Literal nameAlice;

	private Literal nameBob;

	private Literal mboxAlice;

	private Literal mboxBob;

	private IRI context1;

	private IRI context2;

	@Before
	public void setUp() throws Exception {
		testRepository = createRepository();

		testCon = testRepository.getConnection();
		testCon.clear();
		testCon.clearNamespaces();

		vf = testRepository.getValueFactory();

		// Initialize values
		bob = vf.createBNode();
		alice = vf.createBNode();
		alexander = vf.createBNode();

		nameAlice = vf.createLiteral("Alice");
		nameBob = vf.createLiteral("Bob");

		mboxAlice = vf.createLiteral("alice@example.org");
		mboxBob = vf.createLiteral("bob@example.org");

		context1 = vf.createIRI("urn:x-local:graph1");
		context2 = vf.createIRI("urn:x-local:graph2");
	}

	@After
	public void tearDown() throws Exception {
		try {
			testCon.close();
		} finally {
			testRepository.shutDown();
		}
	}

	@Test
	public void testAddRDFStarSubject() throws Exception {
		Triple rdfStarTriple = vf.createTriple(bob, FOAF.NAME, nameBob);

		testCon.add(rdfStarTriple, RDF.TYPE, RDF.ALT);

		assertThat(testCon.hasStatement(rdfStarTriple, RDF.TYPE, RDF.ALT, false)).isTrue();
	}

	@Test
	public void testAddRDFStarObject() throws Exception {
		Triple rdfStarTriple = vf.createTriple(bob, FOAF.NAME, nameBob);

		testCon.add(RDF.ALT, RDF.TYPE, rdfStarTriple);

		assertThat(testCon.hasStatement(RDF.ALT, RDF.TYPE, rdfStarTriple, false)).isTrue();
	}

	@Test
	public void testAddRDFStarContext() throws Exception {
		Triple rdfStarTriple = vf.createTriple(bob, FOAF.NAME, nameBob);

		try {
			testCon.add(RDF.ALT, RDF.TYPE, RDF.ALT, rdfStarTriple);
			fail("RDF* triple value should not be allowed by store as context identifier");
		} catch (UnsupportedOperationException e) {
			// fall through, expected behavior
			testCon.rollback();
		}

	}

	@Test
	public void testSparqlStar() {
		Triple rdfStarTriple = vf.createTriple(bob, FOAF.NAME, nameBob);

		testCon.add(rdfStarTriple, RDF.TYPE, RDF.ALT);

		String query = "PREFIX foaf: <" + FOAF.NAMESPACE + ">\nSELECT DISTINCT * WHERE { <<?s foaf:name ?o>> ?b ?c. }";

		List<BindingSet> result = QueryResults.asList(testCon.prepareTupleQuery(query).evaluate());
		assertThat(result).hasSize(1);

		BindingSet bs = result.get(0);
		assertThat(bs.getValue("s")).isEqualTo(bob);
		assertThat(bs.getValue("o")).isEqualTo(nameBob);
		assertThat(bs.getValue("b")).isEqualTo(RDF.TYPE);
		assertThat(bs.getValue("c")).isEqualTo(RDF.ALT);

	}

	protected abstract Repository createRepository();
}
