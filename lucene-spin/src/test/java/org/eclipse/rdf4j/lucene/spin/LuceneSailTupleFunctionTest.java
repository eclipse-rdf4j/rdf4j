/**
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.lucene.spin;

import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.MATCHES;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.QUERY;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.rdf4j.common.iteration.Iterations;
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
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.ALL_MATCHES;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SCORE;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SEARCH;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This brings all tests for new property function -based implementation of lucene search request.
 *
 * @see <a href="https://github.com/eclipse/rdf4j/issues/739">issue #739</a>
 */
public class LuceneSailTupleFunctionTest {

	private Repository repository;

	private static final String DATA = "org/eclipse/rdf4j/sail/220-example.ttl";

	private RepositoryConnection connection;

	@ClassRule
	public static TemporaryFolder tempDir = new TemporaryFolder();

	private static Logger log = LoggerFactory.getLogger(LuceneSailTupleFunctionTest.class);

	/**
	 * Hierarchy of classes: <br/>
	 * <ul>
	 * <li>{@link MemoryStore}
	 * <li>{@link LuceneSail}
	 * <li>{@link SailRepository}
	 * </ul>
	 * First item on the list is the most base whereas the last one is the abstraction provider. The SPARQL
	 * request is evaluated by <code>LuceneSail</code> so that class is the abstraction provider. <br/>
	 *
	 * @throws Exception
	 */
	@Before
	public void setUp()
		throws Exception
	{
		// load data into memory store
		MemoryStore store = new MemoryStore();

		// activate Lucene index
		LuceneSail lucene = new LuceneSail();

		configure(lucene);
		lucene.setBaseSail(store);

		repository = new SailRepository(lucene);
		repository.initialize();

		connection = repository.getConnection();
		populate(connection);

		// validate population
		int count = countStatements(connection);
		log.trace("storage contains {} triples", count);
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
			if (repository != null) {
				repository.shutDown();
			}
		}
	}

	protected void configure(LuceneSail sail)
		throws IOException
	{
		sail.setParameter(LuceneSail.INDEX_CLASS_KEY, LuceneSail.DEFAULT_INDEX_CLASS);
		sail.setParameter(LuceneSail.LUCENE_DIR_KEY, tempDir.newFolder().getAbsolutePath());
	}

	protected void populate(RepositoryConnection connection)
		throws Exception
	{
		// process transaction
		try {
			// load resources
			URL resourceURL = LuceneSailTupleFunctionTest.class.getClassLoader().getResource(DATA);
			log.info("Resource URL: {}", resourceURL.toString());
			connection.begin();

			assert resourceURL instanceof URL;
			connection.add(resourceURL.openStream(), resourceURL.toString(), RDFFormat.TURTLE,
					new Resource[] {});

		}
		catch (Exception e) {
			connection.rollback();
			throw e;
		}
		finally {
			connection.commit();
		}

	}

	/**
	 * Positive control test: ie valid SPARQL query.
	 *
	 * @throws Exception
	 */
	@Test
	public void simpleTest()
		throws Exception
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append("select ?s ?p ?o where { ?s ?p ?o } limit 10");
		try {
			connection.begin();

			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, buffer.toString());
			try (TupleQueryResult res = query.evaluate()) {
				int count = countTupleResults(res);
				log.info("count statements: {}", count);
				Assert.assertTrue(count > 0);
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

	/**
	 * Valid search query. SPRQL query: <code>
	 * select ?pred ?score ?label where {
	 *    ?pred search:matches[
	 *       search:query "ornare";
	 *       search:score ?score
	 *       ] .
	 *    ?pred rdfs:label ?label .
	 * }
	 * </code>
	 *
	 * @throws Exception
	 */
	@Test
	public void simpleSearchTest()
		throws Exception
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append("select ?pred ?score ?label where {\n");
		buffer.append("  ?pred <" + MATCHES + "> [\n");
		buffer.append("    <" + QUERY + ">  " + "\"ornare\" ;\n");
		buffer.append("    <" + SCORE + "> ?score \n");
		buffer.append("  ] .\n");
		buffer.append("  ?pred rdfs:label ?label .\n");
		buffer.append("}\n");
		log.info("Request query: \n{}\n", buffer.toString());

		try {
			connection.begin();

			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, buffer.toString());
			log.debug("query \n{}", query);
			printTupleResult(query);
			try (TupleQueryResult res = query.evaluate()) {
				int count = countTupleResults(res);
				log.info("count statements: {}", count);
				Assert.assertTrue(count == 1);
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

	/**
	 * #220 exmaple was reproduced with query: <code>
	 * select ?pred ?score ?query ?label where {
	 *   bind(str("ornare") as ?query) .
	 *   ?pred search:matches [
	 *        search:query ?query;
	 *        search:score ?score
	 *        ] .
	 *   ?pred rdfs:label ?label .
	 * }
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
		buffer.append("  ?pred <" + MATCHES + "> [\n");
		buffer.append("    <" + QUERY + ">  " + " ?query ;\n");
		buffer.append("    <" + SCORE + "> ?score \n");
		buffer.append("  ] .\n");
		buffer.append("  ?pred rdfs:label ?label .\n");
		buffer.append("}\n");
		log.info("Request query: \n==================\n{}\n=======================\n", buffer.toString());

		try {
			connection.begin();

			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, buffer.toString());
			printTupleResult(query);
			try (TupleQueryResult res = query.evaluate()) {
				int count = countTupleResults(res);
				log.info("count statements: {}", count);
				Assert.assertTrue(count == 1);
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

	/**
	 * Reproduce #235 with following query: <code>
	 * construct {
	 *   ?pred a <urn:ontology/Phrase> .
	 *   ?pred <urn:ontology/label> ?label .
	     *   ?pred <urn:ontology/score> ?score .
	 * } where {
	 *    ?pred search:matches[
	 *       search:query "ornare";
	 *       search:score ?score
	 *       ] .
	 *    ?pred rdfs:label ?label .
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
		buffer.append("  ?pred <urn:ontology/label> ?label .\n");
		buffer.append("  ?pred <urn:ontology/score> ?score . \n");
		buffer.append(" } where {\n");
		//buffer.append("select * where {\n");
		buffer.append("  ?pred <" + MATCHES + "> [\n");
		buffer.append("     <" + QUERY + "> \"ornare\";\n");
		buffer.append("     <" + SCORE + "> ?score \n");
		buffer.append("     ] .\n");
		buffer.append("  ?pred rdfs:label ?label .\n");
		buffer.append(" }");
		log.info("Request query: \n{}\n", buffer.toString());

		try {
			connection.begin();

			GraphQuery query = connection.prepareGraphQuery(QueryLanguage.SPARQL, buffer.toString());
			printGraphResult(query);
			try (GraphQueryResult res = query.evaluate()) {
				int cnt = countGraphResults(res);
				log.info("+++++    count triples: {}", cnt);
				Assert.assertTrue(String.format("count triples: {}", cnt), cnt == 3);
			}
			/*
			 * TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, buffer.toString());
			 * ByteArrayOutputStream resultoutput = new ByteArrayOutputStream(); query.evaluate(new
			 * SPARQLResultsCSVWriter(resultoutput)); log.info("tuple response: "); log.info(new
			 * String(resultoutput.toByteArray()));
			 */
			/*
			 * try (TupleQueryResult res = query.evaluate()) { int count = countTupleResults(res); log.info(
			 * "count statements: {}", count); Assert.assertTrue(count == 2); }
			 */
		}
		catch (Exception e) {
			connection.rollback();
			throw e;
		}
		finally {
			connection.commit();
		}
	}

	@Test
	public void testDistanceFunction()
		throws Exception
	{
		String queryStr = "prefix geo:  <" + GEO.NAMESPACE + ">" + "prefix geof: <" + GEOF.NAMESPACE + ">"
				+ "select ?toUri ?fromUri ?dist where {?toUri a <urn:geo/Landmark>; geo:asWKT ?to. ?fromUri geo:asWKT ?from; <urn:geo/maxDistance> ?range."
				+ " bind(geof:distance(?from, ?to, ?units) as ?dist)" + " filter(?dist < ?range)" + " }";
		try {
			connection.begin();
			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
			query.setBinding("units", GEOF.UOM_METRE);

			printTupleResult(query);
			try (TupleQueryResult result = query.evaluate()) {
				int count = countTupleResults(result);
				assertThat(count).isEqualTo(2);
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
		try {
			connection.begin();

			RepositoryResult<Statement> sts = connection.getStatements(null, null, null, new Resource[] {});
			return Iterations.asList(sts).size();
		}
		catch (Exception e) {
			connection.rollback();
			throw e;
		}
		finally {
			connection.commit();
		}
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

	public void printGraphResult(GraphQuery query) {
		ByteArrayOutputStream resultoutput = new ByteArrayOutputStream();
		query.evaluate(new TurtleWriter(resultoutput));
		log.info("graph result:");
		log.info("\n=============\n" + new String(resultoutput.toByteArray()) + "\n=============");
	}

	public void printTupleResult(TupleQuery query) {
		ByteArrayOutputStream resultoutput = new ByteArrayOutputStream();
		query.evaluate(new SPARQLResultsCSVWriter(resultoutput));
		log.info("tuple result:");
		log.info("\n=============\n" + new String(resultoutput.toByteArray()) + "\n=============");
	}
}
