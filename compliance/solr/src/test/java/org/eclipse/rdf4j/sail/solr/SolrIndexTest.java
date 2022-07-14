/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.solr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.SearchDocument;
import org.eclipse.rdf4j.sail.lucene.SearchFields;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrIndexTest {

	private static final Logger logger = LoggerFactory.getLogger(SolrIndexTest.class);

	private static final String DATA_DIR = "target/test-data";

	private static final SimpleValueFactory fac = SimpleValueFactory.getInstance();

	public static final IRI CONTEXT_1 = fac.createIRI("urn:context1");
	public static final IRI CONTEXT_2 = fac.createIRI("urn:context2");
	public static final IRI CONTEXT_3 = fac.createIRI("urn:context3");

	// create some objects that we will use throughout this test
	IRI subject = fac.createIRI("urn:subj");
	IRI subject2 = fac.createIRI("urn:subj2");

	IRI predicate1 = fac.createIRI("urn:pred1");
	IRI predicate2 = fac.createIRI("urn:pred2");

	Literal object1 = fac.createLiteral("object1");
	Literal object2 = fac.createLiteral("object2");
	Literal object3 = fac.createLiteral("cats");
	Literal object4 = fac.createLiteral("dogs");
	Literal object5 = fac.createLiteral("chicken");

	Statement statement11 = fac.createStatement(subject, predicate1, object1);
	Statement statement12 = fac.createStatement(subject, predicate2, object2);
	Statement statement21 = fac.createStatement(subject2, predicate1, object3);
	Statement statement22 = fac.createStatement(subject2, predicate2, object4);
	Statement statement23 = fac.createStatement(subject2, predicate2, object5);

	Statement statementContext111 = fac.createStatement(subject, predicate1, object1, CONTEXT_1);
	Statement statementContext121 = fac.createStatement(subject, predicate2, object2, CONTEXT_1);
	Statement statementContext211 = fac.createStatement(subject2, predicate1, object3, CONTEXT_1);
	Statement statementContext222 = fac.createStatement(subject2, predicate2, object4, CONTEXT_2);
	Statement statementContext232 = fac.createStatement(subject2, predicate2, object5, CONTEXT_2);

	SolrIndex index;
	SolrClient client;

	private static String toRestoreSolrHome = null;

	@BeforeClass
	public static void setUpClass() throws Exception {
		toRestoreSolrHome = System.getProperty("solr.solr.home");
		PropertiesReader reader = new PropertiesReader("maven-config.properties");
		String testSolrHome = reader.getProperty("test.solr.home");
		logger.debug("setting solr home to {}", testSolrHome);
		System.setProperty("solr.solr.home", testSolrHome);
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		System.setProperty("solr.solr.home", toRestoreSolrHome == null ? "" : toRestoreSolrHome);
		toRestoreSolrHome = null;
	}

	@Before
	public void setUp() throws Exception {
		index = new SolrIndex();
		Properties sailProperties = new Properties();
		sailProperties.put(SolrIndex.SERVER_KEY, "embedded:");
		index.initialize(sailProperties);
		client = index.getClient();
	}

	@After
	public void tearDown() throws Exception {
		index.shutDown();

		FileUtils.deleteDirectory(new File(DATA_DIR));
		org.eclipse.rdf4j.common.concurrent.locks.Properties.setLockTrackingEnabled(false);

	}

	@Test
	public void testAddStatement() throws IOException, SolrServerException {
		// add a statement to an index
		index.begin();
		index.addStatement(statement11);
		index.commit();

		// check that it arrived properly
		long count = client.query(new SolrQuery("*:*").setRows(0)).getResults().getNumFound();
		assertEquals(1, count);

		QueryResponse response = client
				.query(new SolrQuery(SolrIndex.termQuery(SearchFields.URI_FIELD_NAME, subject.toString())));
		Iterator<SolrDocument> docs = response.getResults().iterator();
		assertTrue(docs.hasNext());

		SolrDocument doc = docs.next();
		assertEquals(subject.toString(), doc.get(SearchFields.URI_FIELD_NAME));
		assertEquals(1, doc.getFieldValues(predicate1.toString()).size());
		assertEquals(object1.getLabel(), doc.getFirstValue(predicate1.toString()));

		assertFalse(docs.hasNext());

		// add another statement
		index.begin();
		index.addStatement(statement12);
		index.commit();

		// See if everything remains consistent. We must create a new IndexReader
		// in order to be able to see the updates
		count = client.query(new SolrQuery("*:*").setRows(0)).getResults().getNumFound();
		assertEquals(1, count); // #docs should *not* have increased

		response = client.query(new SolrQuery(SolrIndex.termQuery(SearchFields.URI_FIELD_NAME, subject.toString())));
		docs = response.getResults().iterator();
		assertTrue(docs.hasNext());

		doc = docs.next();
		assertEquals(subject.toString(), doc.get(SearchFields.URI_FIELD_NAME));
		assertEquals(1, doc.getFieldValues(predicate1.toString()).size());
		assertEquals(object1.getLabel(), doc.getFirstValue(predicate1.toString()));
		assertEquals(1, doc.getFieldValues(predicate2.toString()).size());
		assertEquals(object2.getLabel(), doc.getFirstValue(predicate2.toString()));

		assertFalse(docs.hasNext());

		// see if we can query for these literals
		count = client
				.query(new SolrQuery(SolrIndex.termQuery(SearchFields.TEXT_FIELD_NAME, object1.getLabel())).setRows(0))
				.getResults()
				.getNumFound();
		assertEquals(1, count);

		count = client
				.query(new SolrQuery(SolrIndex.termQuery(SearchFields.TEXT_FIELD_NAME, object2.getLabel())).setRows(0))
				.getResults()
				.getNumFound();
		assertEquals(1, count);

		// remove the first statement
		index.begin();
		index.removeStatement(statement11);
		index.commit();

		// check that that statement is actually removed and that the other still
		// exists
		count = client.query(new SolrQuery("*:*").setRows(0)).getResults().getNumFound();
		assertEquals(1, count);

		response = client.query(new SolrQuery(SolrIndex.termQuery(SearchFields.URI_FIELD_NAME, subject.toString())));
		docs = response.getResults().iterator();
		assertTrue(docs.hasNext());

		doc = docs.next();
		assertEquals(subject.toString(), doc.get(SearchFields.URI_FIELD_NAME));
		assertNull(doc.get(predicate1.toString()));
		assertEquals(1, doc.getFieldValues(predicate2.toString()).size());
		assertEquals(object2.getLabel(), doc.getFirstValue(predicate2.toString()));

		assertFalse(docs.hasNext());

		// remove the other statement
		index.begin();
		index.removeStatement(statement12);
		index.commit();

		// check that there are no documents left (i.e. the last Document was
		// removed completely, rather than its remaining triple removed)
		count = client.query(new SolrQuery("*:*").setRows(0)).getResults().getNumFound();
		assertEquals(0, count);
	}

	@Test
	public void testAddMultiple() throws Exception {
		// add a statement to an index
		HashSet<Statement> added = new HashSet<>();
		HashSet<Statement> removed = new HashSet<>();
		added.add(statement11);
		added.add(statement12);
		added.add(statement21);
		added.add(statement22);
		index.begin();
		index.addRemoveStatements(added, removed);
		index.commit();

		// check that it arrived properly
		long count = client.query(new SolrQuery("*:*").setRows(0)).getResults().getNumFound();
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
		Set<String> texts = new HashSet<>();
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
	 * Contexts can only be tested in combination with a sail, as the triples have to be retrieved from the sail
	 *
	 * @throws Exception
	 */
	@Test
	public void testContexts() throws Exception {
		// add a sail
		MemoryStore memoryStore = new MemoryStore();
		// enable lock tracking
		org.eclipse.rdf4j.common.concurrent.locks.Properties.setLockTrackingEnabled(true);
		LuceneSail sail = new LuceneSail();
		sail.setBaseSail(memoryStore);
		sail.setLuceneIndex(index);

		// create a Repository wrapping the LuceneSail
		SailRepository repository = new SailRepository(sail);

		try ( // now add the statements through the repo
				// add statements with context
				SailRepositoryConnection connection = repository.getConnection()) {
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
		} finally {
			// close repo
			repository.shutDown();
		}
	}

	/**
	 * Contexts can only be tested in combination with a sail, as the triples have to be retrieved from the sail
	 *
	 * @throws Exception
	 */
	@Test
	public void testContextsRemoveContext2() throws Exception {
		// add a sail
		MemoryStore memoryStore = new MemoryStore();
		// enable lock tracking
		org.eclipse.rdf4j.common.concurrent.locks.Properties.setLockTrackingEnabled(true);
		LuceneSail sail = new LuceneSail();
		sail.setBaseSail(memoryStore);
		sail.setLuceneIndex(index);

		// create a Repository wrapping the LuceneSail
		SailRepository repository = new SailRepository(sail);

		try ( // now add the statements through the repo
				// add statements with context
				SailRepositoryConnection connection = repository.getConnection()) {
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
		} finally {
			// close repo
			repository.shutDown();
		}
	}

	@Test
	public void testRejectedDatatypes() {
		Literal literal1 = fac.createLiteral("hi there");
		Literal literal2 = fac.createLiteral("hi there, too", XSD.STRING);
		Literal literal3 = fac.createLiteral("1.0");
		Literal literal4 = fac.createLiteral("1.0", XSD.FLOAT);

		assertEquals("Is the first literal accepted?", true, index.accept(literal1));
		assertEquals("Is the second literal accepted?", true, index.accept(literal2));
		assertEquals("Is the third literal accepted?", true, index.accept(literal3));
		assertEquals("Is the fourth literal accepted?", false, index.accept(literal4));
	}

	@Test
	public void testRejectedCoreDatatypes() {
		Literal literal1 = fac.createLiteral("hi there");
		Literal literal2 = fac.createLiteral("hi there, too", CoreDatatype.XSD.STRING);
		Literal literal3 = fac.createLiteral("1.0");
		Literal literal4 = fac.createLiteral("1.0", CoreDatatype.XSD.FLOAT);

		assertEquals("Is the first literal accepted?", true, index.accept(literal1));
		assertEquals("Is the second literal accepted?", true, index.accept(literal2));
		assertEquals("Is the third literal accepted?", true, index.accept(literal3));
		assertEquals("Is the fourth literal accepted?", false, index.accept(literal4));
	}

	private void assertStatement(Statement statement) throws Exception {
		SearchDocument document = index.getDocument(statement.getSubject(), statement.getContext());
		if (document == null) {
			fail("Missing document " + statement.getSubject());
		}
		assertStatement(statement, document);
	}

	private void assertNoStatement(Statement statement) throws Exception {
		SearchDocument document = index.getDocument(statement.getSubject(), statement.getContext());
		if (document == null) {
			return;
		}
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
			if (((Literal) statement.getObject()).getLabel().equals(f)) {
				return;
			}
		}
		fail("Statement not found in document " + statement);
	}

	/**
	 * @param statement112
	 * @param document
	 */
	private void assertNoStatement(Statement statement, SearchDocument document) {
		List<String> fields = document.getProperty(SearchFields.getPropertyField(statement.getPredicate()));
		if (fields == null) {
			return;
		}
		for (String f : fields) {
			if (((Literal) statement.getObject()).getLabel().equals(f)) {
				fail("Statement should not be found in document " + statement);
			}
		}

	}

	static class PropertiesReader {
		private final Properties properties;

		public PropertiesReader(String propertyFileName) throws IOException {
			InputStream is = getClass().getClassLoader()
					.getResourceAsStream(propertyFileName);
			this.properties = new Properties();
			this.properties.load(is);
		}

		public String getProperty(String propertyName) {
			return this.properties.getProperty(propertyName);
		}
	}
}
