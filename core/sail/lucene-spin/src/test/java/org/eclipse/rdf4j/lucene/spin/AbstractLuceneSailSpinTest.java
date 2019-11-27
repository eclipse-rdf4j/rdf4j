/**
 * Copyright (c) 2017 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.lucene.spin;

import com.google.common.collect.Lists;
import java.util.List;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.GEO;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.query.BindingSet;
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
import org.junit.Ignore;
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
	public void simpleTest() throws Exception {
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
	public void simpleSearchTest() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("select ?predicate ?score ?subject where {\n");
		buffer.append("(\"ornare\" <" + ALL_MATCHES + "> <" + SCORE + ">) <" + SEARCH + ">  (?pred ?score) . \n");
		buffer.append("  ?pred <urn:test.org/onto#number> ?subject .\n");
		buffer.append("}\n");
		log.info("Request query: \n====================\n{}\n======================\n", buffer.toString());

		TupleQuery query = getConnection().prepareTupleQuery(QueryLanguage.SPARQL, buffer.toString());
		log.debug("query class: {}", query.getClass());
		// log.debug("query representation: \n{}", query);
		// printTupleResult(query);
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
	@Ignore("Regression due to GH-1642 - no appetite pending deprecation of LuceneSpinSail")
	public void test220Issue() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append("select ?pred ?score ?query ?label where {\n");
		buffer.append("  bind(str(\"ornare\") as ?query) .\n");
		buffer.append("  (?query <" + ALL_MATCHES + "> <" + SCORE + "> ) <" + SEARCH + ">  (?pred ?score) . \n");
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
	 *   ?pred a <urn:ontology/Phrase> . ?pred <urn:ontology/label> ?label2 .
	 * ?pred <urn:ontology/score> ?score } where { bind(str("ornare") as ?query)
	 * . (?query search:allMatches search:score) search:search (?pred ?score) .
	 * ?pred rdfs:label ?label . bind(fn:upper-case(?label) as ?label2) , }
	 * </code>
	 *
	 * @throws Exception
	 */
	@Test
	@Ignore("Regression due to GH-1642 - no appetite pending deprecation of LuceneSpinSail")
	public void test235Issue() throws Exception {
		StringBuilder buffer = new StringBuilder();
		buffer.append(" construct {\n");
		buffer.append("  ?pred a <urn:ontology/Phrase> .\n");
		buffer.append("  ?pred <urn:ontology/label> ?label2 .\n");
		buffer.append("  ?pred <urn:ontology/score> ?score .\n");
		buffer.append(" } where {\n");
		buffer.append("  bind(str(\"ornare\") as ?query) .\n");
		buffer.append("  (?query <" + ALL_MATCHES + "> <" + SCORE + ">) <" + SEARCH + "> (?pred ?score) . \n");
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
	public void testDistanceFunction() throws Exception {
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
		} catch (Exception e) {
			connection.rollback();
			throw e;
		} finally {
			connection.commit();
		}
	}

	/**
	 * Example for issue #771. Complex query: <br\>
	 * <p>
	 * <h3>Scenario description:</h3> The target searched term is in a statement with using unknown predicate. The
	 * predicate might be found using a label with following statements:
	 * <code style='display: block;'> ?pred_map rdfs:label "keyWord" ; t:column
	 * ?pred .</code> This style of RDF file is made by Apache Any23.
	 * </p>
	 * <p>
	 * The query solving the problem might looks like: <div><code>
	 * prefix t: <urn:test.org/onto#>
	 * prefix kw: <urn:test.org/key-words/>
	 *
	 * select ?term_string ?sub ?score where { ?pred_map rdfs:label "keyWord" ;
	 * t:column ?pred . [] ?pred ?term . bind(str(?term) as ?term_string) .
	 * (?term_string search:allMaches search:score) search:search (?sub ?score)
	 * . ?sub a t:Data . }
	 * </code> </div> <br/>
	 * However that is not well managed by RDF4J so there should be a separation between binding value to `?term_string`
	 * and the magic property job: <br/>
	 * <code>
	 * prefix t: <urn:test.org/onto#>
	 * prefix kw: <urn:test.org/key-words/>
	 *
	 * select ?term_string ?sub ?score where { 
	 * (?term_string search:allMaches search:score) search:search (?sub ?score) . 
	 * ?sub a t:Data . 
	 *   { select ?term_string where 
	 *       { 
	 *       ?pred_map rdfs:label "keyWord" ; 
	 *       t:column ?pred . 
	 *       [] ?pred ?term . 
	 *       bind(str(?term) as ?term_string) . 
	 *       } 
	 *   }
	 * } 
	 * </code>
	 *
	 * @throws Exception
	 */
	@Test
	@Ignore("Regression due to GH-1642 - no appetite pending deprecation of LuceneSpinSail")
	public void test771issue() throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("prefix t: <urn:test.org/onto#> \n");
		sb.append("prefix kw: <urn:test.org/key-words/> \n\n");
		sb.append("select ?term_string ?sub ?score where { \n");
		sb.append("  (?term_string <" + ALL_MATCHES + "> <" + SCORE + ">) <" + SEARCH + "> (?sub ?score) . \n");
		sb.append("  ?sub a t:Data . \n");
		sb.append("   { \n");
		sb.append("   select ?term_string where { \n");
		sb.append("     ?pred_map rdfs:label \"keyWord\" ; \n");
		sb.append("     t:column ?pred . \n");
		sb.append("     [] ?pred ?term . \n");
		sb.append("     bind(str(?term) as ?term_string) . \n");
		sb.append("     } \n");
		sb.append("   } \n");
		sb.append("}");

		log.info("SPARQL query:\n=======\n{}\n=======\n", sb.toString());

		TupleQuery query = getConnection().prepareTupleQuery(sb.toString());
		try {
			printTupleResult(query);
			List<BindingSet> results = Iterations.asList(query.evaluate());
			List<String> subjects = Lists.transform(results,
					(BindingSet input) -> input.getBinding("sub").getValue().stringValue());

			Assert.assertTrue(subjects.contains("urn:test.org/data/rec5"));
			Assert.assertTrue(subjects.contains("urn:test.org/data/rec4"));
			Assert.assertTrue(subjects.contains("urn:test.org/data/rec2"));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public int countStatements(RepositoryConnection connection) throws Exception {
		RepositoryResult<Statement> sts = connection.getStatements(null, null, null, new Resource[] {});
		return Iterations.asList(sts).size();
	}

	public int countTupleResults(TupleQueryResult results) throws Exception {
		return Iterations.asList(results).size();
	}

	public int countGraphResults(GraphQueryResult results) throws Exception {
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
