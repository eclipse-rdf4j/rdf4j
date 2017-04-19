/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.eclipse.rdf4j.lucene.spin;

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
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;
import org.eclipse.rdf4j.sail.lucene.LuceneSailSchema;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.ALL_MATCHES;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SCORE;
import static org.eclipse.rdf4j.sail.lucene.LuceneSailSchema.SEARCH;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test class reproduces errors described in issues #220 and #235 with simplified IDE (#739).
 * 
 * @see <a href="https://github.com/eclipse/rdf4j/issues/220">issue #220</a>
 * @see <a href="https://github.com/eclipse/rdf4j/issues/235">issue #235</a>
 * @see <a href="https://github.com/eclipse/rdf4j/issues/739">issue #739</a>
 * @author Jacek Grzebyta
 * @author Mark Hale
 */
public abstract class AbstractLuceneSailSpinTest {

	private static final Logger log = LoggerFactory.getLogger(AbstractLuceneSailSpinTest.class);

	public abstract RepositoryConnection getConnection();

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
		TupleQuery query = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, buffer.toString());
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

		TupleQuery query = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, buffer.toString());
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

		TupleQuery query = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, buffer.toString());
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

		GraphQuery query = getConnection().prepareGraphQuery(QueryLanguage.SPARQL, buffer.toString());
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
		RepositoryConnection connection = getConnection();
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
