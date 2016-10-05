/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.MATCHES;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.QUERY;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SCORE;

import java.io.IOException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.text.csv.SPARQLResultsCSVWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;
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
public abstract class AbstractLuceneSailSPARQLTest {

	private Repository repository;

	private RepositoryConnection connection;

	private static Logger log = LoggerFactory.getLogger(AbstractLuceneSailSPARQLTest.class);

	@Before
	public void setUp()
		throws Exception
	{
		// load data into memory store
		MemoryStore store = new MemoryStore();

		// add Support for SPIN function
		SpinSail spin = new SpinSail(store);

		// prepare sLucene wrapper
		LuceneSail sail = new LuceneSail();
		configure(sail);
		sail.setBaseSail(spin);
		repository = new SailRepository(sail);
		repository.initialize();

		connection = repository.getConnection();
		populate(connection);

		// validate population
		int count = countStatements(connection);
		log.info("storage contains {} triples", count);
		assert count > 0;
	}

	@After
	public void tearDown()
		throws IOException, RepositoryException
	{
		connection.close();
		repository.shutDown();
	}

	protected abstract void configure(LuceneSail sail);

	protected abstract void populate(RepositoryConnection connection)
		throws Exception;

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
	 * select ?pred ?score ?id where {
	 *    ?pred search:matches[
	 *       search:query "Abf1";
	 *       search:score ?score 
	 *       ] .
	 *    ?pred <urn:raw:yeastract#Yeast_id> ?id .
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
		buffer.append("select ?pred ?score ?id where {\n");
		buffer.append("  ?pred <" + MATCHES + "> [\n");
		buffer.append("    <" + QUERY + ">  " + "\"Abf1\" ;\n");
		buffer.append("    <" + SCORE + "> ?score \n");
		buffer.append("  ] .\n");
		buffer.append("  ?pred <urn:raw:yeastract#Yeast_id> ?id .\n");
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
	 * #220 exmaple was reproduced with query: <code>
	 * select ?pred ?score ?query ?id where { 
	 *   bind(str("Abf1") as ?query) . 
	 *   ?pred search:matches [ 
	 *        search:query ?query; 
	 *        search:score ?score 
	 *        ] . 
	 *   ?pred <urn:raw:yeastract#Yeast_id> ?id . 
	 * }
	 * </code>
	 * 
	 * @throws Exception
	 */
	@Test(expected = QueryEvaluationException.class)
	public void test220Issue()
		throws Exception
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append("select ?pred ?score ?query ?id where {\n");
		buffer.append("  bind(str(\"Abf1\") as ?query) .\n");
		buffer.append("  ?pred <" + MATCHES + "> [\n");
		buffer.append("    <" + QUERY + ">  " + " ?query ;\n");
		buffer.append("    <" + SCORE + "> ?score \n");
		buffer.append("  ] .\n");
		buffer.append("  ?pred <urn:raw:yeastract#Yeast_id> ?id .\n");
		buffer.append("}\n");
		log.info("Request query: \n==================\n{}\n=======================\n", buffer.toString());

		try {
			connection.begin();

			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, buffer.toString());
			printTupleResult(query);
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
	 * Reproduce #235 with following query: <code>
	 * construct {
	 *   ?pred a <urn:ontology/Gene> .
	 *   ?pred <urn:ontology/id> ?id2 .
	 * } where {
	 *    ?pred search:matches[
	 *       search:query "Abf1";
	 *       search:score ?score 
	 *       ] .
	 *    ?pred <urn:raw:yeastract#Yeast_id> ?id .
	 *    bind(str(?id) as ?id2)
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
		buffer.append("  ?pred a <urn:ontology/Gene> .\n");
		buffer.append("  ?pred <urn:ontology/id> ?id2 .\n");
		buffer.append(" } where {\n");
		//buffer.append("select * where {\n");
		buffer.append("  ?pred <" + MATCHES + "> [\n");
		buffer.append("     <" + QUERY + "> \"Abf1\";\n");
		buffer.append("     <" + SCORE + "> ?score \n");
		buffer.append("     ] .\n");
		buffer.append("  ?pred <urn:raw:yeastract#Yeast_id> ?id .\n");
		buffer.append("  bind(str(?id) as ?id2)\n");
		buffer.append(" }");
		log.info("Request query: \n{}\n", buffer.toString());

		try {
			connection.begin();

			GraphQuery query = connection.prepareGraphQuery(QueryLanguage.SPARQL, buffer.toString());
			printGraphResult(query);
			try (GraphQueryResult res = query.evaluate()) {
				int cnt = countGraphResults(res);
				log.info("+++++    count triples: {}", cnt);
				Assert.assertTrue(String.format("count triples: {}", cnt), cnt == 2);
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
