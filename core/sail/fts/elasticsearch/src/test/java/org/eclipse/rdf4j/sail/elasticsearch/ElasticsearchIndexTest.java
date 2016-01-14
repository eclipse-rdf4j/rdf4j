/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.elasticsearch.ElasticsearchIndex;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.SearchDocument;
import org.eclipse.rdf4j.sail.lucene.SearchFields;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.FileSystemUtils;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ElasticsearchIndexTest {

	private static final String DATA_DIR = "target/test-data";

	private static final ValueFactory vf = SimpleValueFactory.getInstance();
	
	public static final IRI CONTEXT_1 = vf.createIRI("urn:context1");

	public static final IRI CONTEXT_2 = vf.createIRI("urn:context2");

	public static final IRI CONTEXT_3 = vf.createIRI("urn:context3");

	// create some objects that we will use throughout this test
	IRI subject = vf.createIRI("urn:subj");

	IRI subject2 = vf.createIRI("urn:subj2");

	IRI predicate1 = vf.createIRI("urn:pred1");

	IRI predicate2 = vf.createIRI("urn:pred2");

	Literal object1 = vf.createLiteral("object1");

	Literal object2 = vf.createLiteral("object2");

	Literal object3 = vf.createLiteral("cats");

	Literal object4 = vf.createLiteral("dogs");

	Literal object5 = vf.createLiteral("chicken");

	Statement statement11 = vf.createStatement(subject, predicate1, object1);

	Statement statement12 = vf.createStatement(subject, predicate2, object2);

	Statement statement21 = vf.createStatement(subject2, predicate1, object3);

	Statement statement22 = vf.createStatement(subject2, predicate2, object4);

	Statement statement23 = vf.createStatement(subject2, predicate2, object5);

	Statement statementContext111 = vf.createStatement(subject, predicate1, object1,
			CONTEXT_1);

	Statement statementContext121 = vf.createStatement(subject, predicate2, object2,
			CONTEXT_1);

	Statement statementContext211 = vf.createStatement(subject2, predicate1, object3,
			CONTEXT_1);

	Statement statementContext222 = vf.createStatement(subject2, predicate2, object4,
			CONTEXT_2);

	Statement statementContext232 = vf.createStatement(subject2, predicate2, object5,
			CONTEXT_2);

	Node node;

	Client client;

	ElasticsearchIndex index;

	@Before
	public void setUp()
		throws Exception
	{
		index = new ElasticsearchIndex();
		Properties sailProperties = new Properties();
		sailProperties.put(LuceneSail.LUCENE_DIR_KEY, DATA_DIR);
		index.initialize(sailProperties);
		node = NodeBuilder.nodeBuilder().loadConfigSettings(false).client(true).local(true).clusterName(
				index.getClusterName()).node();
		client = node.client();
	}

	@After
	public void tearDown()
		throws Exception
	{
		client.close();
		node.close();
		index.shutDown();
		FileSystemUtils.deleteRecursively(new File(DATA_DIR));
	}

	@Test
	public void testAddStatement()
		throws IOException
	{
		// add a statement to an index
		index.begin();
		index.addStatement(statement11);
		index.commit();

		// check that it arrived properly
		long count = client.prepareCount(index.getIndexName()).setTypes(index.getTypes()).execute().actionGet().getCount();
		assertEquals(1, count);

		SearchHits hits = client.prepareSearch(index.getIndexName()).setTypes(index.getTypes()).setQuery(
				QueryBuilders.termQuery(SearchFields.URI_FIELD_NAME, subject.toString())).execute().actionGet().getHits();
		Iterator<SearchHit> docs = hits.iterator();
		assertTrue(docs.hasNext());

		SearchHit doc = docs.next();
		Map<String, Object> fields = client.prepareGet(doc.getIndex(), doc.getType(), doc.getId()).execute().actionGet().getSource();
		assertEquals(subject.toString(), fields.get(SearchFields.URI_FIELD_NAME));
		assertEquals(object1.getLabel(), fields.get(predicate1.toString()));

		assertFalse(docs.hasNext());

		// add another statement
		index.begin();
		index.addStatement(statement12);
		index.commit();

		// See if everything remains consistent. We must create a new IndexReader
		// in order to be able to see the updates
		count = client.prepareCount(index.getIndexName()).setTypes(index.getTypes()).execute().actionGet().getCount();
		assertEquals(1, count); // #docs should *not* have increased

		hits = client.prepareSearch(index.getIndexName()).setTypes(index.getTypes()).setQuery(
				QueryBuilders.termQuery(SearchFields.URI_FIELD_NAME, subject.toString())).execute().actionGet().getHits();
		docs = hits.iterator();
		assertTrue(docs.hasNext());

		doc = docs.next();
		fields = client.prepareGet(doc.getIndex(), doc.getType(), doc.getId()).execute().actionGet().getSource();
		assertEquals(subject.toString(), fields.get(SearchFields.URI_FIELD_NAME));
		assertEquals(object1.getLabel(), fields.get(predicate1.toString()));
		assertEquals(object2.getLabel(), fields.get(predicate2.toString()));

		assertFalse(docs.hasNext());

		// see if we can query for these literals
		count = client.prepareCount(index.getIndexName()).setTypes(index.getTypes()).setQuery(
				QueryBuilders.queryStringQuery(object1.getLabel())).execute().actionGet().getCount();
		assertEquals(1, count);

		count = client.prepareCount(index.getIndexName()).setTypes(index.getTypes()).setQuery(
				QueryBuilders.queryStringQuery(object2.getLabel())).execute().actionGet().getCount();
		assertEquals(1, count);

		// remove the first statement
		index.begin();
		index.removeStatement(statement11);
		index.commit();

		// check that that statement is actually removed and that the other still
		// exists
		count = client.prepareCount(index.getIndexName()).setTypes(index.getTypes()).execute().actionGet().getCount();
		assertEquals(1, count);

		hits = client.prepareSearch(index.getIndexName()).setTypes(index.getTypes()).setQuery(
				QueryBuilders.termQuery(SearchFields.URI_FIELD_NAME, subject.toString())).execute().actionGet().getHits();
		docs = hits.iterator();
		assertTrue(docs.hasNext());

		doc = docs.next();
		fields = client.prepareGet(doc.getIndex(), doc.getType(), doc.getId()).execute().actionGet().getSource();
		assertEquals(subject.toString(), fields.get(SearchFields.URI_FIELD_NAME));
		assertNull(fields.get(predicate1.toString()));
		assertEquals(object2.getLabel(), fields.get(predicate2.toString()));

		assertFalse(docs.hasNext());

		// remove the other statement
		index.begin();
		index.removeStatement(statement12);
		index.commit();

		// check that there are no documents left (i.e. the last Document was
		// removed completely, rather than its remaining triple removed)
		count = client.prepareCount(index.getIndexName()).setTypes(index.getTypes()).execute().actionGet().getCount();
		assertEquals(0, count);
	}

	@Test
	public void testAddMultiple()
		throws Exception
	{
		// add a statement to an index
		HashSet<Statement> added = new HashSet<Statement>();
		HashSet<Statement> removed = new HashSet<Statement>();
		added.add(statement11);
		added.add(statement12);
		added.add(statement21);
		added.add(statement22);
		index.begin();
		index.addRemoveStatements(added, removed);
		index.commit();

		// check that it arrived properly
		long count = client.prepareCount(index.getIndexName()).setTypes(index.getTypes()).execute().actionGet().getCount();
		assertEquals(2, count);

		// check the documents
		SearchDocument document = index.getDocuments(subject).iterator().next();
		assertEquals(subject.toString(), document.getResource());
		assertStatement(statement11, document);
		assertStatement(statement12, document);

		document = index.getDocuments(subject2).iterator().next();
		assertEquals(subject2.toString(), document.getResource());
		assertStatement(statement21, document);
		assertStatement(statement22, document);

		// check if the text field stores all added string values
		Set<String> texts = new HashSet<String>();
		texts.add("cats");
		texts.add("dogs");
		// FIXME
		// assertTexts(texts, document);

		// add/remove one
		added.clear();
		removed.clear();
		added.add(statement23);
		removed.add(statement22);
		index.begin();
		index.addRemoveStatements(added, removed);
		index.commit();

		// check doc 2
		document = index.getDocuments(subject2).iterator().next();
		assertEquals(subject2.toString(), document.getResource());
		assertStatement(statement21, document);
		assertStatement(statement23, document);
		assertNoStatement(statement22, document);

		// check if the text field stores all added and no deleted string values
		texts.remove("dogs");
		texts.add("chicken");
		// FIXME
		// assertTexts(texts, document);

		// TODO: check deletion of the rest

	}

	/**
	 * Contexts can only be tested in combination with a sail, as the triples
	 * have to be retrieved from the sail
	 *
	 * @throws Exception
	 */
	@Test
	public void testContexts()
		throws Exception
	{
		// add a sail
		MemoryStore memoryStore = new MemoryStore();
		// enable lock tracking
		org.eclipse.rdf4j.common.concurrent.locks.Properties.setLockTrackingEnabled(true);
		LuceneSail sail = new LuceneSail();
		sail.setBaseSail(memoryStore);
		sail.setLuceneIndex(index);

		// create a Repository wrapping the LuceneSail
		SailRepository repository = new SailRepository(sail);
		repository.initialize();

		// now add the statements through the repo
		// add statements with context
		SailRepositoryConnection connection = repository.getConnection();
		try {
			connection.begin();
			connection.add(statementContext111, statementContext111.getContext());
			connection.add(statementContext121, statementContext121.getContext());
			connection.add(statementContext211, statementContext211.getContext());
			connection.add(statementContext222, statementContext222.getContext());
			connection.add(statementContext232, statementContext232.getContext());
			connection.commit();

			// check if they are there
			assertStatement(statementContext111);
			assertStatement(statementContext121);
			assertStatement(statementContext211);
			assertStatement(statementContext222);
			assertStatement(statementContext232);

			// delete context 1
			connection.begin();
			connection.clear(new Resource[] { CONTEXT_1 });
			connection.commit();
			assertNoStatement(statementContext111);
			assertNoStatement(statementContext121);
			assertNoStatement(statementContext211);
			assertStatement(statementContext222);
			assertStatement(statementContext232);
		}
		finally {
			// close repo
			connection.close();
			repository.shutDown();
		}
	}

	/**
	 * Contexts can only be tested in combination with a sail, as the triples
	 * have to be retrieved from the sail
	 *
	 * @throws Exception
	 */
	@Test
	public void testContextsRemoveContext2()
		throws Exception
	{
		// add a sail
		MemoryStore memoryStore = new MemoryStore();
		// enable lock tracking
		org.eclipse.rdf4j.common.concurrent.locks.Properties.setLockTrackingEnabled(true);
		LuceneSail sail = new LuceneSail();
		sail.setBaseSail(memoryStore);
		sail.setLuceneIndex(index);

		// create a Repository wrapping the LuceneSail
		SailRepository repository = new SailRepository(sail);
		repository.initialize();

		// now add the statements through the repo
		// add statements with context
		SailRepositoryConnection connection = repository.getConnection();
		try {
			connection.begin();
			connection.add(statementContext111, statementContext111.getContext());
			connection.add(statementContext121, statementContext121.getContext());
			connection.add(statementContext211, statementContext211.getContext());
			connection.add(statementContext222, statementContext222.getContext());
			connection.add(statementContext232, statementContext232.getContext());
			connection.commit();

			// check if they are there
			assertStatement(statementContext111);
			assertStatement(statementContext121);
			assertStatement(statementContext211);
			assertStatement(statementContext222);
			assertStatement(statementContext232);

			// delete context 2
			connection.begin();
			connection.clear(new Resource[] { CONTEXT_2 });
			connection.commit();
			assertStatement(statementContext111);
			assertStatement(statementContext121);
			assertStatement(statementContext211);
			assertNoStatement(statementContext222);
			assertNoStatement(statementContext232);
		}
		finally {
			// close repo
			connection.close();
			repository.shutDown();
		}
	}

	@Test
	public void testRejectedDatatypes() {
		IRI STRING = vf.createIRI("http://www.w3.org/2001/XMLSchema#string");
		IRI FLOAT = vf.createIRI("http://www.w3.org/2001/XMLSchema#float");
		Literal literal1 = vf.createLiteral("hi there");
		Literal literal2 = vf.createLiteral("hi there, too", STRING);
		Literal literal3 = vf.createLiteral("1.0");
		Literal literal4 = vf.createLiteral("1.0", FLOAT);
		assertEquals("Is the first literal accepted?", true, index.accept(literal1));
		assertEquals("Is the second literal accepted?", true, index.accept(literal2));
		assertEquals("Is the third literal accepted?", true, index.accept(literal3));
		assertEquals("Is the fourth literal accepted?", false, index.accept(literal4));
	}

	private void assertStatement(Statement statement)
		throws Exception
	{
		SearchDocument document = index.getDocument(statement.getSubject(), statement.getContext());
		if (document == null)
			fail("Missing document " + statement.getSubject());
		assertStatement(statement, document);
	}

	private void assertNoStatement(Statement statement)
		throws Exception
	{
		SearchDocument document = index.getDocument(statement.getSubject(), statement.getContext());
		if (document == null)
			return;
		assertNoStatement(statement, document);
	}

	/**
	 * @param statement112
	 * @param document
	 */
	private void assertStatement(Statement statement, SearchDocument document) {
		List<String> fields = document.getProperty(SearchFields.getPropertyField(statement.getPredicate()));
		assertNotNull("field " + statement.getPredicate() + " not found in document " + document, fields);
		for (String f : fields) {
			if (((Literal)statement.getObject()).getLabel().equals(f))
				return;
		}
		fail("Statement not found in document " + statement);
	}

	/**
	 * @param statement112
	 * @param document
	 */
	private void assertNoStatement(Statement statement, SearchDocument document) {
		List<String> fields = document.getProperty(SearchFields.getPropertyField(statement.getPredicate()));
		if (fields == null)
			return;
		for (String f : fields) {
			if (((Literal)statement.getObject()).getLabel().equals(f))
				fail("Statement should not be found in document " + statement);
		}

	}

	/*private void assertTexts(Set<String> texts, Document document) {
		Set<String> toFind = new HashSet<String>(texts);
		Set<String> found = new HashSet<String>();
		for(Field field : document.getFields(LuceneIndex.TEXT_FIELD_NAME)) {
			// is the field value expected and not yet been found?
			if(toFind.remove(field.stringValue())) {
				// add it to the found set
				// (it was already remove from the toFind list in the if clause)
				found.add(field.stringValue());
			} else {
				assertEquals("Was the text value '" + field.stringValue() + "' expected to exist?", false, true);
			}
		}

		for(String notFound : toFind) {
			assertEquals("Was the expected text value '" + notFound + "' found?", true, false);
		}
	}*/
}
