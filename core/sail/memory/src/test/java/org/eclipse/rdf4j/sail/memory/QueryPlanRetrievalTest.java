/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.memory;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.query.explanation.GenericPlanNode;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class QueryPlanRetrievalTest {

	public static final String MAIN_QUERY = "{" +
			"    {" +
			"        OPTIONAL {" +
			"            ?d ?e ?f" +
			"        }" +
			"    } " +
			"    ?a a ?c, ?d. " +
			"    FILTER(?c != ?d) " +
			"    OPTIONAL{" +
			"        ?d ?e ?f" +
			"    } " +
			"}";

	public static final String TUPLE_QUERY = "SELECT ?a WHERE " + MAIN_QUERY;
	public static final String ASK_QUERY = "ASK WHERE " + MAIN_QUERY;
	public static final String CONSTRUCT_QUERY = "CONSTRUCT {?a a ?c, ?d} WHERE " + MAIN_QUERY;

	ValueFactory vf = SimpleValueFactory.getInstance();

	private void addData(SailRepository sailRepository) {
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDF.PROPERTY, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(vf.createBNode(), FOAF.KNOWS, vf.createBNode());
			connection.add(vf.createBNode(), FOAF.KNOWS, vf.createBNode());
			connection.add(vf.createBNode(), FOAF.KNOWS, vf.createBNode());
			connection.add(vf.createBNode(), FOAF.KNOWS, vf.createBNode());
			connection.add(vf.createBNode(), FOAF.KNOWS, vf.createBNode());
			connection.add(vf.createBNode(), FOAF.KNOWS, vf.createBNode());
			connection.add(vf.createBNode(), FOAF.KNOWS, vf.createBNode());
			connection.add(vf.createBNode(), FOAF.KNOWS, vf.createBNode());
		}
	}

	@Test
	public void testTupleQuery() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(TUPLE_QUERY);

			String actual = query.explain(Explanation.Level.Unoptimized).toString();

			String expected = "Projection\n" +
					"   ProjectionElemList\n" +
					"      ProjectionElem \"a\"\n" +
					"   Filter\n" +
					"      Compare (!=)\n" +
					"         Var (name=c)\n" +
					"         Var (name=d)\n" +
					"      LeftJoin\n" +
					"         Join\n" +
					"            Join\n" +
					"               LeftJoin\n" +
					"                  SingletonSet\n" +
					"                  StatementPattern\n" +
					"                     Var (name=d)\n" +
					"                     Var (name=e)\n" +
					"                     Var (name=f)\n" +
					"               StatementPattern\n" +
					"                  Var (name=a)\n" +
					"                  Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"                  Var (name=c)\n" +
					"            StatementPattern\n" +
					"               Var (name=a)\n" +
					"               Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"               Var (name=d)\n" +
					"         StatementPattern\n" +
					"            Var (name=d)\n" +
					"            Var (name=e)\n" +
					"            Var (name=f)\n";

			Assert.assertEquals(expected, actual);

		}
	}

	@Test
	public void testTupleQueryOptimized() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			TupleQuery query = connection.prepareTupleQuery(TUPLE_QUERY);

			String actual = query.explain(Explanation.Level.Optimized).toString();
			String expected = "Projection\n" +
					"   ProjectionElemList\n" +
					"      ProjectionElem \"a\"\n" +
					"   LeftJoin\n" +
					"      Join\n" +
					"         StatementPattern (costEstimate=1, resultSizeEstimate=4)\n" +
					"            Var (name=a)\n" +
					"            Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"            Var (name=d)\n" +
					"         Filter\n" +
					"            Compare (!=)\n" +
					"               Var (name=c)\n" +
					"               Var (name=d)\n" +
					"            Join\n" +
					"               StatementPattern (costEstimate=2, resultSizeEstimate=4)\n" +
					"                  Var (name=a)\n" +
					"                  Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"                  Var (name=c)\n" +
					"               LeftJoin (costEstimate=5, resultSizeEstimate=12)\n" +
					"                  SingletonSet\n" +
					"                  StatementPattern (resultSizeEstimate=12)\n" +
					"                     Var (name=d)\n" +
					"                     Var (name=e)\n" +
					"                     Var (name=f)\n" +
					"      StatementPattern (resultSizeEstimate=12)\n" +
					"         Var (name=d)\n" +
					"         Var (name=e)\n" +
					"         Var (name=f)\n";
			Assert.assertEquals(expected, actual);

		}
	}

	@Test
	public void testTupleQueryTimed() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			TupleQuery query = connection.prepareTupleQuery(TUPLE_QUERY);

			GenericPlanNode genericPlanNode = query.explain(Explanation.Level.Timed).toGenericPlanNode();

			GenericPlanNode leftJoin = genericPlanNode.getPlans().get(1);
			GenericPlanNode filterNode = genericPlanNode.getPlans().get(1).getPlans().get(0).getPlans().get(1);

			assertEquals("LeftJoin", leftJoin.getType());
			assertEquals("Filter", filterNode.getType());

			assertTrue(filterNode.getSelfTimeActual() > leftJoin.getSelfTimeActual());
			assertTrue(filterNode.getSelfTimeActual() < leftJoin.getTotalTimeActual());

			assertThat(genericPlanNode.toString(), containsString("selfTimeActual"));
			assertThat(genericPlanNode.toString(), containsString("totalTimeActual"));

		}
	}

	@Test
	public void testTupleQueryExecuted() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(TUPLE_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toString();
			String expected = "Projection (resultSizeActual=2)\n" +
					"   ProjectionElemList\n" +
					"      ProjectionElem \"a\"\n" +
					"   LeftJoin (resultSizeActual=2)\n" +
					"      Join (resultSizeActual=2)\n" +
					"         StatementPattern (costEstimate=1, resultSizeEstimate=4, resultSizeActual=4)\n" +
					"            Var (name=a)\n" +
					"            Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"            Var (name=d)\n" +
					"         Filter (resultSizeActual=2)\n" +
					"            Compare (!=)\n" +
					"               Var (name=c)\n" +
					"               Var (name=d)\n" +
					"            Join (resultSizeActual=6)\n" +
					"               StatementPattern (costEstimate=2, resultSizeEstimate=4, resultSizeActual=6)\n" +
					"                  Var (name=a)\n" +
					"                  Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"                  Var (name=c)\n" +
					"               LeftJoin (costEstimate=5, resultSizeEstimate=12, resultSizeActual=72)\n" +
					"                  SingletonSet (resultSizeActual=6)\n" +
					"                  StatementPattern (resultSizeEstimate=12, resultSizeActual=72)\n" +
					"                     Var (name=d)\n" +
					"                     Var (name=e)\n" +
					"                     Var (name=f)\n" +
					"      StatementPattern (resultSizeEstimate=12, resultSizeActual=2)\n" +
					"         Var (name=d)\n" +
					"         Var (name=e)\n" +
					"         Var (name=f)\n";
			Assert.assertEquals(expected, actual);

		}
	}

	@Test
	public void testGenericPlanNode() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(TUPLE_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toGenericPlanNode().toString();
			String expected = "Projection (resultSizeActual=2)\n" +
					"   ProjectionElemList\n" +
					"      ProjectionElem \"a\"\n" +
					"   LeftJoin (resultSizeActual=2)\n" +
					"      Join (resultSizeActual=2)\n" +
					"         StatementPattern (costEstimate=1, resultSizeEstimate=4, resultSizeActual=4)\n" +
					"            Var (name=a)\n" +
					"            Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"            Var (name=d)\n" +
					"         Filter (resultSizeActual=2)\n" +
					"            Compare (!=)\n" +
					"               Var (name=c)\n" +
					"               Var (name=d)\n" +
					"            Join (resultSizeActual=6)\n" +
					"               StatementPattern (costEstimate=2, resultSizeEstimate=4, resultSizeActual=6)\n" +
					"                  Var (name=a)\n" +
					"                  Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"                  Var (name=c)\n" +
					"               LeftJoin (costEstimate=5, resultSizeEstimate=12, resultSizeActual=72)\n" +
					"                  SingletonSet (resultSizeActual=6)\n" +
					"                  StatementPattern (resultSizeEstimate=12, resultSizeActual=72)\n" +
					"                     Var (name=d)\n" +
					"                     Var (name=e)\n" +
					"                     Var (name=f)\n" +
					"      StatementPattern (resultSizeEstimate=12, resultSizeActual=2)\n" +
					"         Var (name=d)\n" +
					"         Var (name=e)\n" +
					"         Var (name=f)\n";
			Assert.assertEquals(expected, actual);
		}
	}

	@Test
	public void testJsonPlanNode() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(TUPLE_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toJson();
			String expected = "{\n" +
					"  \"type\" : \"Projection\",\n" +
					"  \"resultSizeActual\" : 2,\n" +
					"  \"plans\" : [ {\n" +
					"    \"type\" : \"ProjectionElemList\",\n" +
					"    \"plans\" : [ {\n" +
					"      \"type\" : \"ProjectionElem \\\"a\\\"\"\n" +
					"    } ]\n" +
					"  }, {\n" +
					"    \"type\" : \"LeftJoin\",\n" +
					"    \"resultSizeActual\" : 2,\n" +
					"    \"plans\" : [ {\n" +
					"      \"type\" : \"Join\",\n" +
					"      \"resultSizeActual\" : 2,\n" +
					"      \"plans\" : [ {\n" +
					"        \"type\" : \"StatementPattern\",\n" +
					"        \"costEstimate\" : 1.3333333333333333,\n" +
					"        \"resultSizeEstimate\" : 4.0,\n" +
					"        \"resultSizeActual\" : 4,\n" +
					"        \"plans\" : [ {\n" +
					"          \"type\" : \"Var (name=a)\"\n" +
					"        }, {\n" +
					"          \"type\" : \"Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\"\n"
					+
					"        }, {\n" +
					"          \"type\" : \"Var (name=d)\"\n" +
					"        } ]\n" +
					"      }, {\n" +
					"        \"type\" : \"Filter\",\n" +
					"        \"resultSizeActual\" : 2,\n" +
					"        \"plans\" : [ {\n" +
					"          \"type\" : \"Compare (!=)\",\n" +
					"          \"plans\" : [ {\n" +
					"            \"type\" : \"Var (name=c)\"\n" +
					"          }, {\n" +
					"            \"type\" : \"Var (name=d)\"\n" +
					"          } ]\n" +
					"        }, {\n" +
					"          \"type\" : \"Join\",\n" +
					"          \"resultSizeActual\" : 6,\n" +
					"          \"plans\" : [ {\n" +
					"            \"type\" : \"StatementPattern\",\n" +
					"            \"costEstimate\" : 2.0,\n" +
					"            \"resultSizeEstimate\" : 4.0,\n" +
					"            \"resultSizeActual\" : 6,\n" +
					"            \"plans\" : [ {\n" +
					"              \"type\" : \"Var (name=a)\"\n" +
					"            }, {\n" +
					"              \"type\" : \"Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\"\n"
					+
					"            }, {\n" +
					"              \"type\" : \"Var (name=c)\"\n" +
					"            } ]\n" +
					"          }, {\n" +
					"            \"type\" : \"LeftJoin\",\n" +
					"            \"costEstimate\" : 5.241482788417793,\n" +
					"            \"resultSizeEstimate\" : 12.0,\n" +
					"            \"resultSizeActual\" : 72,\n" +
					"            \"plans\" : [ {\n" +
					"              \"type\" : \"SingletonSet\",\n" +
					"              \"resultSizeActual\" : 6\n" +
					"            }, {\n" +
					"              \"type\" : \"StatementPattern\",\n" +
					"              \"resultSizeEstimate\" : 12.0,\n" +
					"              \"resultSizeActual\" : 72,\n" +
					"              \"plans\" : [ {\n" +
					"                \"type\" : \"Var (name=d)\"\n" +
					"              }, {\n" +
					"                \"type\" : \"Var (name=e)\"\n" +
					"              }, {\n" +
					"                \"type\" : \"Var (name=f)\"\n" +
					"              } ]\n" +
					"            } ]\n" +
					"          } ]\n" +
					"        } ]\n" +
					"      } ]\n" +
					"    }, {\n" +
					"      \"type\" : \"StatementPattern\",\n" +
					"      \"resultSizeEstimate\" : 12.0,\n" +
					"      \"resultSizeActual\" : 2,\n" +
					"      \"plans\" : [ {\n" +
					"        \"type\" : \"Var (name=d)\"\n" +
					"      }, {\n" +
					"        \"type\" : \"Var (name=e)\"\n" +
					"      }, {\n" +
					"        \"type\" : \"Var (name=f)\"\n" +
					"      } ]\n" +
					"    } ]\n" +
					"  } ]\n" +
					"}";
			Assert.assertEquals(expected, actual);

		}
	}

	@Test
	public void testAskQuery() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareBooleanQuery(ASK_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toString();
			String expected = "Slice (limit=1) (resultSizeActual=1)\n" +
					"   LeftJoin (resultSizeActual=1)\n" +
					"      Join (resultSizeActual=1)\n" +
					"         StatementPattern (costEstimate=1, resultSizeEstimate=4, resultSizeActual=3)\n" +
					"            Var (name=a)\n" +
					"            Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"            Var (name=d)\n" +
					"         Filter (resultSizeActual=1)\n" +
					"            Compare (!=)\n" +
					"               Var (name=c)\n" +
					"               Var (name=d)\n" +
					"            Join (resultSizeActual=4)\n" +
					"               StatementPattern (costEstimate=2, resultSizeEstimate=4, resultSizeActual=4)\n" +
					"                  Var (name=a)\n" +
					"                  Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"                  Var (name=c)\n" +
					"               LeftJoin (costEstimate=5, resultSizeEstimate=12, resultSizeActual=38)\n" +
					"                  SingletonSet (resultSizeActual=4)\n" +
					"                  StatementPattern (resultSizeEstimate=12, resultSizeActual=38)\n" +
					"                     Var (name=d)\n" +
					"                     Var (name=e)\n" +
					"                     Var (name=f)\n" +
					"      StatementPattern (resultSizeEstimate=12, resultSizeActual=1)\n" +
					"         Var (name=d)\n" +
					"         Var (name=e)\n" +
					"         Var (name=f)\n";
			Assert.assertEquals(expected, actual);

		}
	}

	@Test
	public void testConstructQuery() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareGraphQuery(CONSTRUCT_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toString();
			String expected = "Reduced (resultSizeActual=3)\n" +
					"   MultiProjection (resultSizeActual=4)\n" +
					"      ProjectionElemList\n" +
					"         ProjectionElem \"a\" AS \"subject\"\n" +
					"         ProjectionElem \"_const_f5e5585a_uri\" AS \"predicate\"\n" +
					"         ProjectionElem \"c\" AS \"object\"\n" +
					"      ProjectionElemList\n" +
					"         ProjectionElem \"a\" AS \"subject\"\n" +
					"         ProjectionElem \"_const_f5e5585a_uri\" AS \"predicate\"\n" +
					"         ProjectionElem \"d\" AS \"object\"\n" +
					"      Extension (resultSizeActual=2)\n" +
					"         ExtensionElem (_const_f5e5585a_uri)\n" +
					"            ValueConstant (value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type)\n" +
					"         LeftJoin (resultSizeActual=2)\n" +
					"            Join (resultSizeActual=2)\n" +
					"               StatementPattern (costEstimate=1, resultSizeEstimate=4, resultSizeActual=4)\n" +
					"                  Var (name=a)\n" +
					"                  Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"                  Var (name=d)\n" +
					"               Filter (resultSizeActual=2)\n" +
					"                  Compare (!=)\n" +
					"                     Var (name=c)\n" +
					"                     Var (name=d)\n" +
					"                  Join (resultSizeActual=6)\n" +
					"                     StatementPattern (costEstimate=2, resultSizeEstimate=4, resultSizeActual=6)\n"
					+
					"                        Var (name=a)\n" +
					"                        Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"                        Var (name=c)\n" +
					"                     LeftJoin (costEstimate=5, resultSizeEstimate=12, resultSizeActual=72)\n" +
					"                        SingletonSet (resultSizeActual=6)\n" +
					"                        StatementPattern (resultSizeEstimate=12, resultSizeActual=72)\n" +
					"                           Var (name=d)\n" +
					"                           Var (name=e)\n" +
					"                           Var (name=f)\n" +
					"            StatementPattern (resultSizeEstimate=12, resultSizeActual=2)\n" +
					"               Var (name=d)\n" +
					"               Var (name=e)\n" +
					"               Var (name=f)\n";
			Assert.assertEquals(expected, actual);

		}
	}

	@Ignore // slow test used for debugging
	@Test
	public void bigDataset() throws IOException {
		SailRepository repository = new SailRepository(new MemoryStore());

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(QueryPlanRetrievalTest.class.getClassLoader()
					.getResourceAsStream("benchmarkFiles/datagovbe-valid.ttl"), "", RDFFormat.TURTLE);
			connection.commit();
		}

		String query1 = IOUtils.toString(
				QueryPlanRetrievalTest.class.getClassLoader().getResourceAsStream("benchmarkFiles/query1.qr"),
				StandardCharsets.UTF_8);

		try (SailRepositoryConnection connection = repository.getConnection()) {
			String s = connection.prepareTupleQuery(query1).explain(Explanation.Level.Timed).toString();
			System.out.println(s);
		}

	}

}
