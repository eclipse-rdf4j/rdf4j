/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import java.io.File;

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
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import junit.framework.TestCase;

public class StoreSerializationTest extends TestCase {

	/*-----------*
	 * Variables *
	 *-----------*/

	private File dataDir;

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected void setUp()
		throws Exception
	{
		super.setUp();
		dataDir = FileUtil.createTempDir("memorystore");
	}

	@Override
	protected void tearDown()
		throws Exception
	{
		super.tearDown();
		FileUtil.deleteDir(dataDir);
	}

	public void testShortLiterals()
		throws Exception
	{
		MemoryStore store = new MemoryStore(dataDir);
		store.initialize();

		ValueFactory factory = store.getValueFactory();
		IRI foo = factory.createIRI("http://www.foo.example/foo");

		StringBuilder sb = new StringBuilder(4);
		for (int i = 0; i < 4; i++) {
			sb.append('a');
		}

		Literal longLiteral = factory.createLiteral(sb.toString());

		SailConnection con = store.getConnection();
		con.begin();
		con.addStatement(foo, RDF.VALUE, longLiteral);
		con.commit();

		con.close();
		store.shutDown();

		store = new MemoryStore(dataDir);
		store.initialize();

		con = store.getConnection();

		CloseableIteration<? extends Statement, SailException> iter = con.getStatements(foo, RDF.VALUE, null,
				false);
		assertTrue(iter.hasNext());
		iter.next();
		iter.close();

		con.close();
		store.shutDown();		
	}

	public void testSerialization()
		throws Exception
	{
		MemoryStore store = new MemoryStore(dataDir);
		store.initialize();

		ValueFactory factory = store.getValueFactory();
		IRI foo = factory.createIRI("http://www.foo.example/foo");
		IRI bar = factory.createIRI("http://www.foo.example/bar");

		SailConnection con = store.getConnection();
		con.begin();
		con.addStatement(foo, RDF.TYPE, bar);
		con.commit();

		ParsedTupleQuery query = QueryParserUtil.parseTupleQuery(QueryLanguage.SERQL,
				"SELECT X, P, Y FROM {X} P {Y}", null);
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
		store.initialize();

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

	public void testLongLiterals()
		throws Exception
	{
		MemoryStore store = new MemoryStore(dataDir);
		store.initialize();

		ValueFactory factory = store.getValueFactory();
		IRI foo = factory.createIRI("http://www.foo.example/foo");

		StringBuilder sb = new StringBuilder(66000);
		for (int i = 0; i < 66000; i++) {
			sb.append('a');
		}

		Literal longLiteral = factory.createLiteral(sb.toString());

		SailConnection con = store.getConnection();
		con.begin();
		con.addStatement(foo, RDF.VALUE, longLiteral);
		con.commit();

		con.close();
		store.shutDown();

		store = new MemoryStore(dataDir);
		store.initialize();

		con = store.getConnection();

		CloseableIteration<? extends Statement, SailException> iter = con.getStatements(foo, RDF.VALUE, null,
				false);
		assertTrue(iter.hasNext());
		iter.next();
		iter.close();

		con.close();
		store.shutDown();		
	}
}
