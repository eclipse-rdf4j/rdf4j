/**
 * *****************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************
 */
package org.eclipse.rdf4j.sail.lucene;

import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SEARCH;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.ALL_MATCHES;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SCORE;

import java.io.IOException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
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
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.spin.SpinSail;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This brings all tests for new property function -based implementation of lucene search request.
 */
public abstract class AbstractFunctionLuceneSailSPARQLTest {

	private Repository repository;

	private RepositoryConnection connection;

	private static Logger log = LoggerFactory.getLogger(AbstractFunctionLuceneSailSPARQLTest.class);

	/**
	 * Hierarchy of classes: <br/>
	 * <ul>
	 * <li>{@link MemoryStore}
	 * <li>{@link LuceneSail}
	 * <li>{@link SpinSail}
	 * </ul>
	 * First item on the list is the most base whereas the last one is the abstraction provider. The SPARQL
	 * request is evaluated by <code>SpinSail</code> so that class is the abstraction provider. <br/>
	 * <p>
	 * TODO: SPIN's property function support should be provided by the {@code LuceneSail}. In the final
	 * version it is expected that {@code SpinSail} work will be done within {@code LuceneSail}.
	 * </p>
	 *
	 * @throws Exception
	 */
	@Before
	public void setUp()
		throws Exception
	{
		// load data into memory store
		MemoryStore store = new MemoryStore();

		LuceneSail lucene = new LuceneSail();
		configure(lucene);
		lucene.setBaseSail(store);

		// add support of spin functions
		SpinSail spin = new SpinSail(lucene);
		repository = new SailRepository(spin);
		repository.initialize();

		connection = repository.getConnection();
		populate(connection);

		// validate population
		int count = countStatements(connection);
		log.debug("storage contains {} triples", count);
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
	 * Positive control test: ie valid SPARQL query.
	 *
	 * @throws Exception
	 */
	@Test
	@Ignore
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
	 *    (?pred ?score) search:search("Abf1" search:allMatches search:score) .
	 *    ?pred <urn:raw:yeastract#Yeast_id> ?id . }
	 * </code>
	 *
	 * @throws Exception
	 */
	@Test
	public void simpleSearchTest()
		throws Exception
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append("select * where {\n");
		buffer.append("  (?a ?b) <" + SEARCH + "> (\"Abf1\" <" + ALL_MATCHES + ">) . \n");
		//buffer.append("  ?pred <urn:raw:yeastract#Yeast_id> ?id .\n");
		buffer.append("}\n");
		log.info("Request query: \n====================\n{}\n======================\n", buffer.toString());

		try {
			connection.begin();

			TupleQuery query = connection.prepareTupleQuery(QueryLanguage.SPARQL, buffer.toString());
			log.debug("query class: {}", query.getClass());
			//log.debug("query representation: \n{}", query);
			//printTupleResult(query);
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
	 *   (?pred ?score) search:search(?query search:allMatches search:score) .
	 *   ?pred <urn:raw:yeastract#Yeast_id> ?id . }
	 * </code>
	 *
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void test220Issue()
		throws Exception
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append("select ?pred ?score ?query ?id where {\n");
		buffer.append("  bind(str(\"Abf1\") as ?query) .\n");
		buffer.append(
				"  (?pred ?score) <" + SEARCH + "> (?query <" + ALL_MATCHES + "> <" + SCORE + ">) . \n");
		buffer.append("  ?pred <urn:raw:yeastract#Yeast_id> ?id .\n");
		buffer.append("}\n");
		log.info("Request query: \n====================\n{}\n======================\n", buffer.toString());

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
	 *   ?pred a <urn:ontology/Gene> . ?pred <urn:ontology/id> ?id2 . } where {
	 * (?pred ?score) search:search(?query search:allMatches search:score) .
	 * ?pred <urn:raw:yeastract#Yeast_id> ?id . bind(str(?id) as ?id2) }
	 * </code>
	 *
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void test235Issue()
		throws Exception
	{
		StringBuilder buffer = new StringBuilder();
		buffer.append(" construct {\n");
		buffer.append("  ?pred a <urn:ontology/Gene> .\n");
		buffer.append("  ?pred <urn:ontology/id> ?id2 .\n");
		buffer.append(" } where {\n");
		//buffer.append("select * where {\n");
		buffer.append(
				"  (?pred ?score) <" + SEARCH + "> (?query <" + ALL_MATCHES + "> <" + SCORE + ">) . \n");
		buffer.append("  ?pred <urn:raw:yeastract#Yeast_id> ?id .\n");
		buffer.append("  bind(str(?id) as ?id2)\n");
		buffer.append(" }");
		log.info("Request query: \n====================\n{}\n======================\n", buffer.toString());

		try {
			connection.begin();

			GraphQuery query = connection.prepareGraphQuery(QueryLanguage.SPARQL, buffer.toString());
			printGraphResult(query);
			try (GraphQueryResult res = query.evaluate()) {
				int cnt = countGraphResults(res);
				Assert.assertTrue(String.format("count triples: ", cnt), cnt == 2);
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
