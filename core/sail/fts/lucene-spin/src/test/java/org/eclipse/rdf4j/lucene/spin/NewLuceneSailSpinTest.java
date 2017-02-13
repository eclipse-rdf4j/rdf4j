/**
 * Copyright (c) 2017 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.lucene.spin;

import com.google.common.io.Files;
import java.io.File;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.ALL_MATCHES;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SCORE;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SEARCH;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import org.apache.commons.io.FileUtils;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.text.csv.SPARQLResultsCSVWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.LuceneSailSchema;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.spin.SpinSail;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test class reproduces errors described in issues #220 and #235 with simplified IDE (#739).
 * 
 * @see <a href="https://github.com/eclipse/rdf4j/issues/220">issue #220</a>
 * @see <a href="https://github.com/eclipse/rdf4j/issues/235">issue #235</a>
 * @see <a href="https://github.com/eclipse/rdf4j/issues/739">issue #739</a>
 */
public class NewLuceneSailSpinTest {

	private static final String DATA = "org/eclipse/rdf4j/sail/220-example.ttl";

	private static Logger log = LoggerFactory.getLogger(NewLuceneSailSpinTest.class);

	private File tempDir;

	private Repository repository;

	private RepositoryConnection connection;

	@Before
	public void setUp()
		throws Exception
	{
		// load data into memory store
		MemoryStore store = new MemoryStore();

		// add Support for SPIN function
		SpinSail spin = new SpinSail(store);

		// add Lucene Spin Sail support
		LuceneSpinSail luc = new LuceneSpinSail(spin);
		tempDir = Files.createTempDir();
		log.debug("data file: {}", tempDir.getAbsolutePath());
		luc.setDataDir(tempDir);
		repository = new SailRepository(luc);

		// set up parameters
		configure(luc.getParameters());

		repository.initialize();
		// local connection used only for population
		try (RepositoryConnection localConn = repository.getConnection()) {
			localConn.begin();
			populate(localConn);
			localConn.commit();
		}

		// local connection for verification only
		try (RepositoryConnection localConn = repository.getConnection()) {
			// validate population
			//localConn.begin();
			int count = countStatements(localConn);
			log.info("storage contains {} triples", count);
			assert count > 0;
			//localConn.commit();
			localConn.close();
		}

		// testing connection
		connection = repository.getConnection();
		connection.begin();
		assert connection.isActive() : "connection is not active";
	}

	@After
	public void tearDown()
		throws RepositoryException, IOException
	{
		if (connection != null) {
			connection.close();
		}
		FileUtils.deleteDirectory(tempDir);
	}

	protected void populate(RepositoryConnection repoConn)
		throws Exception
	{
		// load resources
		assert repoConn.isActive();
		URL resourceURL = NewLuceneSailSpinTest.class.getClassLoader().getResource(DATA);
		log.info("Resource URL: {}", resourceURL.toString());
		Model model = Rio.parse(resourceURL.openStream(), resourceURL.toString(), RDFFormat.TURTLE);
		for (Statement stmt : model) {
			repoConn.add(stmt);
		}
	}

	/**
	 * Positive control test: ie valid SPARQL query
	 * 
	 * @throws Exception
	 */
	@Test
	public void simpleTest()
		throws Exception
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append("select ?s ?p ?o where { ?s ?p ?o } limit 10");
		TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, buffer.toString());
		try (TupleQueryResult res = query.evaluate()) {
			int count = countTupleResults(res);
			log.info("count statements: {}", count);
			Assert.assertTrue(count > 0);
		}
	}

	/**
	 * Valid search query. SPRQL query: <code>
	 * select ?pred ?score ?subject where {
	 *    ("ornare" search:allMatches search:score) search:search  (?pred ?score) .
	 *    ?pred <urn:test.org/onto#number> ?subject . }
	 * </code>
	 *
	 * @throws Exception
	 */
	@Test
	public void simpleSearchTest()
		throws Exception
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append("select ?predicate ?score ?subject where {\n");
		buffer.append(
				"(\"ornare\" <" + ALL_MATCHES + "> <" + SCORE + ">) <" + SEARCH + ">  (?pred ?score) . \n");
		buffer.append("  ?pred <urn:test.org/onto#number> ?subject .\n");
		buffer.append("}\n");
		log.info("Request query: \n====================\n{}\n======================\n", buffer.toString());

		TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, buffer.toString());
		log.debug("query class: {}", query.getClass());
		//log.debug("query representation: \n{}", query);
		//printTupleResult(query);
		try (TupleQueryResult res = query.evaluate()) {
			int count = countTupleResults(res);
			log.info("count statements: {}", count);
			Assert.assertTrue("count statements: " + count, count > 0);
		}

	}

	/**
	 * #220 exmaple was reproduced with query: <code>
	 * select ?pred ?score ?query ?label where {
	 *   bind(str("ornare") as ?query) .
	 *   (?query search:allMatches search:score) search:search (?pred ?score) .
	 *   ?pred rdfs:label ?label . }
	 * </code>
	 *
	 * @throws Exception
	 */
	@Test
	public void test220Issue()
		throws Exception
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append("select ?pred ?score ?query ?label where {\n");
		buffer.append("  bind(str(\"ornare\") as ?query) .\n");
		buffer.append(
				"  (?query <" + ALL_MATCHES + "> <" + SCORE + "> ) <" + SEARCH + ">  (?pred ?score) . \n");
		buffer.append("  ?pred rdfs:label ?label .\n");
		buffer.append("}\n");
		log.info("Request query: \n====================\n{}\n======================\n", buffer.toString());

		TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, buffer.toString());
		printTupleResult(query);
		try (TupleQueryResult res = query.evaluate()) {
			int count = countTupleResults(res);
			log.info("count statements: {}", count);
			Assert.assertTrue(count > 0);
		}

	}

	/**
	 * Reproduce #235 with following query: <code>
	 * construct {
	 *   ?pred a <urn:ontology/Phrase> . 
	 *   ?pred <urn:ontology/label> ?label2 .
	     *   ?pred <urn:ontology/score> ?score
	 * } where {
	 *   bind(str("ornare") as ?query) .
	 *   (?query search:allMatches search:score) search:search (?pred ?score) .
	 *   ?pred rdfs:label ?label . 
	 *   bind(fn:upper-case(?label) as ?label2) ,
	 * }
	 * </code>
	 *
	 * @throws Exception
	 */
	@Test
	public void test235Issue()
		throws Exception
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append(" construct {\n");
		buffer.append("  ?pred a <urn:ontology/Phrase> .\n");
		buffer.append("  ?pred <urn:ontology/label> ?label2 .\n");
		buffer.append("  ?pred <urn:ontology/score> ?score .\n");
		buffer.append(" } where {\n");
		buffer.append("  bind(str(\"ornare\") as ?query) .\n");
		buffer.append(
				"  (?query <" + ALL_MATCHES + "> <" + SCORE + ">) <" + SEARCH + "> (?pred ?score) . \n");
		buffer.append("  ?pred rdfs:label ?label .\n");
		buffer.append("  bind(fn:upper-case(?label) as ?label2)\n");
		buffer.append(" }");
		log.info("Request query: \n====================\n{}\n======================\n", buffer.toString());

		GraphQuery query = connection.prepareGraphQuery(QueryLanguage.SPARQL, buffer.toString());
		printGraphResult(query);
		try (GraphQueryResult res = query.evaluate()) {
			int cnt = countGraphResults(res);
			Assert.assertTrue(String.format("count triples: ", cnt), cnt > 2);
		}
	}

	@Test
	public void testDistanceFunction()
		throws Exception
	{
		String queryStr = "prefix geo:  <" + GEO.NAMESPACE + ">" + "prefix geof: <" + GEOF.NAMESPACE + ">"
				+ "prefix search: <" + LuceneSailSchema.NAMESPACE + ">"
				+ "select ?toUri ?fromUri ?dist where {(?from ?range ?units geo:asWKT search:distance)"
				+ "search:withinDistance (?toUri ?to ?dist) ."
				+ "?toUri a <urn:geo/Landmark>. ?fromUri geo:asWKT ?from; <urn:geo/maxDistance> ?range.}";
		try {
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
			query.setBinding("units", GEOF.UOM_METRE);

			printTupleResult(query);
			try (TupleQueryResult result = query.evaluate()) {
				int count = countTupleResults(result);
				Assert.assertEquals(2, count);
			}
		}
		catch (Exception e) {
			connection.rollback();
			throw e;
		}
		finally {
			connection.commit();
		}
	}

	public int countStatements(RepositoryConnection connection)
		throws Exception
	{
		RepositoryResult<Statement> sts = connection.getStatements(null, null, null, new Resource[] {});
		return Iterations.asList(sts).size();
	}

	public int countTupleResults(TupleQueryResult results)
		throws Exception
	{
		return Iterations.asList(results).size();
	}

	public int countGraphResults(GraphQueryResult results)
		throws Exception
	{
		return Iterations.asList(results).size();
	}

	protected void printGraphResult(GraphQuery query) {
		ByteArrayOutputStream resultoutput = new ByteArrayOutputStream();
		query.evaluate(new TurtleWriter(resultoutput));
		log.info("graph result:");
		log.info("\n=============\n" + new String(resultoutput.toByteArray()) + "\n=============");
	}

	protected void printTupleResult(TupleQuery query) {
		ByteArrayOutputStream resultoutput = new ByteArrayOutputStream();
		query.evaluate(new SPARQLResultsCSVWriter(resultoutput));
		log.info("tuple result:");
		log.info("\n=============\n" + new String(resultoutput.toByteArray()) + "\n=============");
	}

	protected void configure(Properties parameters) {
		parameters.setProperty(LuceneSail.INDEX_CLASS_KEY, LuceneSail.DEFAULT_INDEX_CLASS);
	}
}
