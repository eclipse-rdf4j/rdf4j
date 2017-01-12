/**
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.sail.lucene;

import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.ALL_MATCHES;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SCORE;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SEARCH;
import static org.hamcrest.CoreMatchers.is;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

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
import org.eclipse.rdf4j.sail.evaluation.TupleFunctionEvaluationMode;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.spin.SpinSail;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test class reproduces errors described in issues #220 and #235.
 * 
 * @see <a href="https://github.com/eclipse/rdf4j/issues/220">issue #220</a>
 * @see <a href="https://github.com/eclipse/rdf4j/issues/235">issue #235</a>
 */
public abstract class AbstractLuceneSailSpinTest {

	private static final String DATA = "org/eclipse/rdf4j/sail/220-example.ttl";

	private static Logger log = LoggerFactory.getLogger(AbstractLuceneSailSpinTest.class);

	private Repository repository;

	private RepositoryConnection connection;

	private SearchIndex searchIndex;

	@Before
	public void setUp()
		throws Exception
	{
		// load data into memory store
		MemoryStore store = new MemoryStore();

		// add Support for SPIN function
		SpinSail spin = new SpinSail(store);
		spin.setEvaluationMode(TupleFunctionEvaluationMode.TRIPLE_SOURCE);

		// add Lucene support
		Properties parameters = new Properties();
		configure(parameters);
		searchIndex = LuceneSail.createSearchIndex(parameters);
		spin.addQueryContextInitializer(new SearchIndexQueryContextInitializer(searchIndex));
		repository = new SailRepository(spin);
		repository.initialize();

		connection = repository.getConnection();
		populate(connection, searchIndex);

		// validate population
		int count = countStatements(connection);
		log.info("storage contains {} triples", count);
		assert count > 0;
	}

	@After
	public void tearDown()
		throws IOException, RepositoryException
	{
		try {
			if (connection != null) {
				connection.close();
			}
		}
		finally {
			try {
				if (repository != null) {
					repository.shutDown();
				}
			}
			finally {
				if (searchIndex != null) {
					searchIndex.shutDown();
				}
			}
		}
	}

	protected abstract void configure(Properties parameters);

	protected void populate(RepositoryConnection repoConn, SearchIndex searchIndex)
		throws Exception
	{
		// load resources
		URL resourceURL = AbstractLuceneSailSpinTest.class.getClassLoader().getResource(DATA);
		log.info("Resource URL: {}", resourceURL.toString());
		Model model = Rio.parse(resourceURL.openStream(), resourceURL.toString(), RDFFormat.TURTLE);
		searchIndex.begin();
		for (Statement stmt : model) {
			repoConn.add(stmt);
			searchIndex.addStatement(stmt);
		}
		searchIndex.commit();
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
		try
		{
			connection.begin();
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
			query.setBinding("units", GEOF.UOM_METRE);
	
			printTupleResult(query);
			try(TupleQueryResult result = query.evaluate())
			{
				int count = countTupleResults(result);
				Assert.assertThat(count, is(2));
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

	public int countStatements(RepositoryConnection con)
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
}
