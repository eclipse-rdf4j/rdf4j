/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class StoreSerializationTest {

	private File dataDir;

	@BeforeEach
	protected void setUp() throws Exception {
		dataDir = Files.createTempDirectory("memorystore").toFile();
	}

	@AfterEach
	protected void tearDown() throws Exception {
		FileUtil.deleteDir(dataDir);
	}

	@Test
	public void testShortLiterals() {
		MemoryStore store = new MemoryStore(dataDir);
		store.init();

		ValueFactory factory = store.getValueFactory();
		IRI foo = factory.createIRI("http://www.foo.example/foo");

		Literal longLiteral = factory.createLiteral("a".repeat(4));

		SailConnection con = store.getConnection();
		con.begin();
		con.addStatement(foo, RDF.VALUE, longLiteral);
		con.commit();

		con.close();
		store.shutDown();

		store = new MemoryStore(dataDir);
		store.init();

		con = store.getConnection();

		CloseableIteration<? extends Statement, SailException> iter = con.getStatements(foo, RDF.VALUE, null, false);
		assertTrue(iter.hasNext());
		iter.next();
		iter.close();

		con.close();
		store.shutDown();
	}

	@Test
	public void testSerialization() {
		MemoryStore store = new MemoryStore(dataDir);
		store.init();

		ValueFactory factory = store.getValueFactory();
		IRI foo = factory.createIRI("http://www.foo.example/foo");
		IRI bar = factory.createIRI("http://www.foo.example/bar");

		SailConnection con = store.getConnection();
		con.begin();
		con.addStatement(foo, RDF.TYPE, bar);
		con.commit();

		ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"SELECT ?X ?P ?Y WHERE { ?X ?P ?Y }",
				null);
		TupleExpr tupleExpr = query.getTupleExpr();

		CloseableIteration<? extends BindingSet, QueryEvaluationException> iter = con.evaluate(tupleExpr, null,
				EmptyBindingSet.getInstance(), false);

		BindingSet bindingSet = iter.next();

		assertEquals(bindingSet.getValue("X"), foo);
		assertEquals(bindingSet.getValue("P"), RDF.TYPE);
		assertEquals(bindingSet.getValue("Y"), bar);
		iter.close();
		con.close();

		store.shutDown();

		store = new MemoryStore(dataDir);
		store.init();

		factory = store.getValueFactory();
		foo = factory.createIRI("http://www.foo.example/foo");
		bar = factory.createIRI("http://www.foo.example/bar");

		con = store.getConnection();

		iter = con.evaluate(tupleExpr, null, EmptyBindingSet.getInstance(), false);

		bindingSet = iter.next();

		assertEquals(bindingSet.getValue("X"), foo);
		assertEquals(bindingSet.getValue("P"), RDF.TYPE);
		assertEquals(bindingSet.getValue("Y"), bar);

		iter.close();
		con.begin();
		con.addStatement(bar, RDF.TYPE, foo);
		con.commit();
		con.close();

		store.shutDown();
	}

	@Test
	public void testLongLiterals() {
		MemoryStore store = new MemoryStore(dataDir);
		store.init();

		ValueFactory factory = store.getValueFactory();
		IRI foo = factory.createIRI("http://www.foo.example/foo");

		Literal longLiteral = factory.createLiteral("a".repeat(66000));

		SailConnection con = store.getConnection();
		con.begin();
		con.addStatement(foo, RDF.VALUE, longLiteral);
		con.commit();

		con.close();
		store.shutDown();

		store = new MemoryStore(dataDir);
		store.init();

		con = store.getConnection();

		CloseableIteration<? extends Statement, SailException> iter = con.getStatements(foo, RDF.VALUE, null, false);
		assertTrue(iter.hasNext());
		iter.next();
		iter.close();

		con.close();
		store.shutDown();
	}
}
