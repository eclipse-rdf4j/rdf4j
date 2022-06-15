/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class QueryPlanRetrievalTest {

	public static final String MAIN_QUERY = String.join("\n", "",
			"{",
			"    {",
			"        OPTIONAL {",
			"            ?d ?e ?f",
			"        }",
			"    } ",
			"    ?a a ?c, ?d. ",
			"    FILTER(?c != ?d && ?c != \"<\") ",
			"    OPTIONAL{",
			"        ?d ?e ?f",
			"    } ",
			"}");

	public static final String TUPLE_QUERY = "SELECT ?a WHERE " + MAIN_QUERY;
	public static final String ASK_QUERY = "ASK WHERE " + MAIN_QUERY;
	public static final String CONSTRUCT_QUERY = "CONSTRUCT {?a a ?c, ?d} WHERE " + MAIN_QUERY;

	public static final String SUB_QUERY = "select ?a where {{select ?a where {?a a ?type}} {SELECT ?a WHERE "
			+ MAIN_QUERY + "}}";

	public static final String UNION_QUERY = "select ?a where {?a a ?type. {?a ?b ?c, ?c2. {?c2 a ?type1}UNION{?c2 a ?type2}} UNION {?type ?d ?c}}";

	ValueFactory vf = SimpleValueFactory.getInstance();

	private void addData(SailRepository sailRepository) {
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDF.PROPERTY, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDF.TYPE, RDF.TYPE, RDF.PROPERTY);
			connection.add(RDF.TYPE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(vf.createBNode("01"), FOAF.KNOWS, vf.createBNode("02"));
			connection.add(vf.createBNode("03"), FOAF.KNOWS, vf.createBNode("04"));
			connection.add(vf.createBNode("05"), FOAF.KNOWS, vf.createBNode("06"));
			connection.add(vf.createBNode("07"), FOAF.KNOWS, vf.createBNode("08"));
			connection.add(vf.createBNode("09"), FOAF.KNOWS, vf.createBNode("10"));
			connection.add(vf.createBNode("11"), FOAF.KNOWS, vf.createBNode("12"));
			connection.add(vf.createBNode("13"), FOAF.KNOWS, vf.createBNode("14"));
			connection.add(vf.createBNode("15"), FOAF.KNOWS, vf.createBNode("16"));
		}
	}

	@Test
	public void testTupleQuery() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(TUPLE_QUERY);

			String actual = query.explain(Explanation.Level.Unoptimized).toString();
			String expected = "Projection\n" +
					"╠══ProjectionElemList\n" +
					"║     ProjectionElem \"a\"\n" +
					"╚══Filter\n" +
					"   ├──And\n" +
					"   │  ╠══Compare (!=)\n" +
					"   │  ║     Var (name=c)\n" +
					"   │  ║     Var (name=d)\n" +
					"   │  ╚══Compare (!=)\n" +
					"   │        Var (name=c)\n" +
					"   │        ValueConstant (value=\"<\")\n" +
					"   └──LeftJoin\n" +
					"      ╠══Join\n" +
					"      ║  ├──LeftJoin (new scope)\n" +
					"      ║  │  ╠══SingletonSet\n" +
					"      ║  │  ╚══StatementPattern\n" +
					"      ║  │        Var (name=d)\n" +
					"      ║  │        Var (name=e)\n" +
					"      ║  │        Var (name=f)\n" +
					"      ║  └──Join\n" +
					"      ║     ╠══StatementPattern\n" +
					"      ║     ║     Var (name=a)\n" +
					"      ║     ║     Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)"
					+ "\n"
					+
					"      ║     ║     Var (name=c)\n" +
					"      ║     ╚══StatementPattern\n" +
					"      ║           Var (name=a)\n" +
					"      ║           Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)"
					+ "\n"
					+
					"      ║           Var (name=d)\n" +
					"      ╚══StatementPattern\n" +
					"            Var (name=d)\n" +
					"            Var (name=e)\n" +
					"            Var (name=f)\n";

			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();
	}

	@Test
	public void testTupleQueryOptimized() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			TupleQuery query = connection.prepareTupleQuery(TUPLE_QUERY);
			String actual = query.explain(Explanation.Level.Optimized).toString();
			String expected = "Projection\n" +
					"╠══ProjectionElemList\n" +
					"║     ProjectionElem \"a\"\n" +
					"╚══LeftJoin (LeftJoinIterator)\n" +
					"   ├──Join (JoinIterator)\n" +
					"   │  ╠══StatementPattern (costEstimate=1, resultSizeEstimate=4)\n" +
					"   │  ║     Var (name=a)\n" +
					"   │  ║     Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)"
					+ "\n"
					+
					"   │  ║     Var (name=d)\n" +
					"   │  ╚══Filter\n" +
					"   │     ├──Compare (!=)\n" +
					"   │     │     Var (name=c)\n" +
					"   │     │     Var (name=d)\n" +
					"   │     └──Join (HashJoinIteration)\n" +
					"   │        ╠══Filter\n" +
					"   │        ║  ├──Compare (!=)\n" +
					"   │        ║  │     Var (name=c)\n" +
					"   │        ║  │     ValueConstant (value=\"<\")\n" +
					"   │        ║  └──StatementPattern (costEstimate=2, resultSizeEstimate=4)\n" +
					"   │        ║        Var (name=a)\n" +
					"   │        ║        Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)"
					+ "\n"
					+
					"   │        ║        Var (name=c)\n" +
					"   │        ╚══LeftJoin (new scope) (costEstimate=5, resultSizeEstimate=12)\n" +
					"   │           ├──SingletonSet\n" +
					"   │           └──StatementPattern (resultSizeEstimate=12)\n" +
					"   │                 Var (name=d)\n" +
					"   │                 Var (name=e)\n" +
					"   │                 Var (name=f)\n" +
					"   └──StatementPattern (resultSizeEstimate=12)\n" +
					"         Var (name=d)\n" +
					"         Var (name=e)\n" +
					"         Var (name=f)\n";
			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

	@Test
	@Disabled
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

			assertThat(genericPlanNode.toString()).contains("selfTimeActual");
			assertThat(genericPlanNode.toString()).contains("totalTimeActual");

		}
		sailRepository.shutDown();

	}

	@Test
	public void testTupleQueryExecuted() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(TUPLE_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toString();
			String expected = "Projection (resultSizeActual=2)\n" +
					"╠══ProjectionElemList\n" +
					"║     ProjectionElem \"a\"\n" +
					"╚══LeftJoin (LeftJoinIterator) (resultSizeActual=2)\n" +
					"   ├──Join (JoinIterator) (resultSizeActual=2)\n" +
					"   │  ╠══StatementPattern (costEstimate=1, resultSizeEstimate=4, resultSizeActual=4)\n" +
					"   │  ║     Var (name=a)\n" +
					"   │  ║     Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)"
					+ "\n"
					+
					"   │  ║     Var (name=d)\n" +
					"   │  ╚══Filter (resultSizeActual=2)\n" +
					"   │     ├──Compare (!=)\n" +
					"   │     │     Var (name=c)\n" +
					"   │     │     Var (name=d)\n" +
					"   │     └──Join (HashJoinIteration) (resultSizeActual=6)\n" +
					"   │        ╠══Filter (resultSizeActual=6)\n" +
					"   │        ║  ├──Compare (!=)\n" +
					"   │        ║  │     Var (name=c)\n" +
					"   │        ║  │     ValueConstant (value=\"<\")\n" +
					"   │        ║  └──StatementPattern (costEstimate=2, resultSizeEstimate=4, resultSizeActual=6)"
					+ "\n" +
					"   │        ║        Var (name=a)\n" +
					"   │        ║        Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)"
					+ "\n"
					+
					"   │        ║        Var (name=c)\n" +
					"   │        ╚══LeftJoin (new scope) (BadlyDesignedLeftJoinIterator) (costEstimate=5, resultSizeEstimate=12, resultSizeActual=4)"
					+ "\n"
					+
					"   │           ├──SingletonSet (resultSizeActual=4)\n" +
					"   │           └──StatementPattern (resultSizeEstimate=12, resultSizeActual=48)\n" +
					"   │                 Var (name=d)\n" +
					"   │                 Var (name=e)\n" +
					"   │                 Var (name=f)\n" +
					"   └──StatementPattern (resultSizeEstimate=12, resultSizeActual=2)\n" +
					"         Var (name=d)\n" +
					"         Var (name=e)\n" +
					"         Var (name=f)\n";
			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

	@Test
	public void testGenericPlanNode() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(TUPLE_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toGenericPlanNode().toString();
			String expected = "Projection (resultSizeActual=2)\n" +
					"╠══ProjectionElemList\n" +
					"║     ProjectionElem \"a\"\n" +
					"╚══LeftJoin (LeftJoinIterator) (resultSizeActual=2)\n" +
					"   ├──Join (JoinIterator) (resultSizeActual=2)\n" +
					"   │  ╠══StatementPattern (costEstimate=1, resultSizeEstimate=4, resultSizeActual=4)\n" +
					"   │  ║     Var (name=a)\n" +
					"   │  ║     Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)"
					+ "\n"
					+
					"   │  ║     Var (name=d)\n" +
					"   │  ╚══Filter (resultSizeActual=2)\n" +
					"   │     ├──Compare (!=)\n" +
					"   │     │     Var (name=c)\n" +
					"   │     │     Var (name=d)\n" +
					"   │     └──Join (HashJoinIteration) (resultSizeActual=6)\n" +
					"   │        ╠══Filter (resultSizeActual=6)\n" +
					"   │        ║  ├──Compare (!=)\n" +
					"   │        ║  │     Var (name=c)\n" +
					"   │        ║  │     ValueConstant (value=\"<\")\n" +
					"   │        ║  └──StatementPattern (costEstimate=2, resultSizeEstimate=4, resultSizeActual=6)"
					+ "\n" +
					"   │        ║        Var (name=a)\n" +
					"   │        ║        Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)"
					+ "\n"
					+
					"   │        ║        Var (name=c)\n" +
					"   │        ╚══LeftJoin (new scope) (BadlyDesignedLeftJoinIterator) (costEstimate=5, resultSizeEstimate=12, resultSizeActual=4)"
					+ "\n"
					+
					"   │           ├──SingletonSet (resultSizeActual=4)\n" +
					"   │           └──StatementPattern (resultSizeEstimate=12, resultSizeActual=48)\n" +
					"   │                 Var (name=d)\n" +
					"   │                 Var (name=e)\n" +
					"   │                 Var (name=f)\n" +
					"   └──StatementPattern (resultSizeEstimate=12, resultSizeActual=2)\n" +
					"         Var (name=d)\n" +
					"         Var (name=e)\n" +
					"         Var (name=f)\n";
			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

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
					"    \"algorithm\" : \"LeftJoinIterator\",\n" +
					"    \"plans\" : [ {\n" +
					"      \"type\" : \"Join\",\n" +
					"      \"resultSizeActual\" : 2,\n" +
					"      \"algorithm\" : \"JoinIterator\",\n" +
					"      \"plans\" : [ {\n" +
					"        \"type\" : \"StatementPattern\",\n" +
					"        \"costEstimate\" : 1.3333333333333333,\n" +
					"        \"resultSizeEstimate\" : 4.0,\n" +
					"        \"resultSizeActual\" : 4,\n" +
					"        \"plans\" : [ {\n" +
					"          \"type\" : \"Var (name=a)\"\n" +
					"        }, {\n" +
					"          \"type\" : \"Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\""
					+ "\n"
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
					"          \"algorithm\" : \"HashJoinIteration\",\n" +
					"          \"plans\" : [ {\n" +
					"            \"type\" : \"Filter\",\n" +
					"            \"resultSizeActual\" : 6,\n" +
					"            \"plans\" : [ {\n" +
					"              \"type\" : \"Compare (!=)\",\n" +
					"              \"plans\" : [ {\n" +
					"                \"type\" : \"Var (name=c)\"\n" +
					"              }, {\n" +
					"                \"type\" : \"ValueConstant (value=\\\"<\\\")\"\n" +
					"              } ]\n" +
					"            }, {\n" +
					"              \"type\" : \"StatementPattern\",\n" +
					"              \"costEstimate\" : 2.0,\n" +
					"              \"resultSizeEstimate\" : 4.0,\n" +
					"              \"resultSizeActual\" : 6,\n" +
					"              \"plans\" : [ {\n" +
					"                \"type\" : \"Var (name=a)\"\n" +
					"              }, {\n" +
					"                \"type\" : \"Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\""
					+ "\n"
					+
					"              }, {\n" +
					"                \"type\" : \"Var (name=c)\"\n" +
					"              } ]\n" +
					"            } ]\n" +
					"          }, {\n" +
					"            \"type\" : \"LeftJoin\",\n" +
					"            \"costEstimate\" : 5.241482788417793,\n" +
					"            \"resultSizeEstimate\" : 12.0,\n" +
					"            \"resultSizeActual\" : 4,\n" +
					"            \"newScope\" : true,\n" +
					"            \"algorithm\" : \"BadlyDesignedLeftJoinIterator\",\n" +
					"            \"plans\" : [ {\n" +
					"              \"type\" : \"SingletonSet\",\n" +
					"              \"resultSizeActual\" : 4\n" +
					"            }, {\n" +
					"              \"type\" : \"StatementPattern\",\n" +
					"              \"resultSizeEstimate\" : 12.0,\n" +
					"              \"resultSizeActual\" : 48,\n" +
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
			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

	@Test
	public void testAskQuery() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareBooleanQuery(ASK_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toString();
			String expected = "Slice (limit=1) (resultSizeActual=1)\n" +
					"   LeftJoin (LeftJoinIterator) (resultSizeActual=1)\n" +
					"   ├──Join (JoinIterator) (resultSizeActual=1)\n" +
					"   │  ╠══StatementPattern (costEstimate=1, resultSizeEstimate=4, resultSizeActual=3)\n" +
					"   │  ║     Var (name=a)\n" +
					"   │  ║     Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"   │  ║     Var (name=d)\n" +
					"   │  ╚══Filter (resultSizeActual=1)\n" +
					"   │     ├──Compare (!=)\n" +
					"   │     │     Var (name=c)\n" +
					"   │     │     Var (name=d)\n" +
					"   │     └──Join (HashJoinIteration) (resultSizeActual=4)\n" +
					"   │        ╠══Filter (resultSizeActual=4)\n" +
					"   │        ║  ├──Compare (!=)\n" +
					"   │        ║  │     Var (name=c)\n" +
					"   │        ║  │     ValueConstant (value=\"<\")\n" +
					"   │        ║  └──StatementPattern (costEstimate=2, resultSizeEstimate=4, resultSizeActual=4)\n" +
					"   │        ║        Var (name=a)\n" +
					"   │        ║        Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)\n"
					+
					"   │        ║        Var (name=c)\n" +
					"   │        ╚══LeftJoin (new scope) (BadlyDesignedLeftJoinIterator) (costEstimate=5, resultSizeEstimate=12, resultSizeActual=3)\n"
					+
					"   │           ├──SingletonSet (resultSizeActual=3)\n" +
					"   │           └──StatementPattern (resultSizeEstimate=12, resultSizeActual=36)\n" +
					"   │                 Var (name=d)\n" +
					"   │                 Var (name=e)\n" +
					"   │                 Var (name=f)\n" +
					"   └──StatementPattern (resultSizeEstimate=12, resultSizeActual=1)\n" +
					"         Var (name=d)\n" +
					"         Var (name=e)\n" +
					"         Var (name=f)\n";
			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

	@Test
	public void testConstructQuery() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareGraphQuery(CONSTRUCT_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toString();

			String expected = "Reduced (resultSizeActual=3)\n"
					+ "   MultiProjection (resultSizeActual=4)\n"
					+ "      ProjectionElemList\n"
					+ "         ProjectionElem \"a\" AS \"subject\"\n"
					+ "         ProjectionElem \"_const_f5e5585a_uri\" AS \"predicate\"\n"
					+ "         ProjectionElem \"c\" AS \"object\"\n"
					+ "      ProjectionElemList\n"
					+ "         ProjectionElem \"a\" AS \"subject\"\n"
					+ "         ProjectionElem \"_const_f5e5585a_uri\" AS \"predicate\"\n"
					+ "         ProjectionElem \"d\" AS \"object\"\n"
					+ "      Extension (resultSizeActual=2)\n"
					+ "      ╠══LeftJoin (LeftJoinIterator) (resultSizeActual=2)\n"
					+ "      ║  ├──Join (JoinIterator) (resultSizeActual=2)\n"
					+ "      ║  │  ╠══StatementPattern (costEstimate=1, resultSizeEstimate=4, resultSizeActual=4)"
					+ "\n"
					+ "      ║  │  ║     Var (name=a)\n"
					+ "      ║  │  ║     Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)"
					+ "\n"
					+ "      ║  │  ║     Var (name=d)\n"
					+ "      ║  │  ╚══Filter (resultSizeActual=2)\n"
					+ "      ║  │     ├──Compare (!=)\n"
					+ "      ║  │     │     Var (name=c)\n"
					+ "      ║  │     │     Var (name=d)\n"
					+ "      ║  │     └──Join (HashJoinIteration) (resultSizeActual=6)\n"
					+ "      ║  │        ╠══Filter (resultSizeActual=6)\n"
					+ "      ║  │        ║  ├──Compare (!=)\n"
					+ "      ║  │        ║  │     Var (name=c)\n"
					+ "      ║  │        ║  │     ValueConstant (value=\"<\")\n"
					+ "      ║  │        ║  └──StatementPattern (costEstimate=2, resultSizeEstimate=4, resultSizeActual=6)"
					+ "\n"
					+ "      ║  │        ║        Var (name=a)\n"
					+ "      ║  │        ║        Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)"
					+ "\n"
					+ "      ║  │        ║        Var (name=c)\n"
					+ "      ║  │        ╚══LeftJoin (new scope) (BadlyDesignedLeftJoinIterator) (costEstimate=5, resultSizeEstimate=12, resultSizeActual=4)"
					+ "\n"
					+ "      ║  │           ├──SingletonSet (resultSizeActual=4)\n"
					+ "      ║  │           └──StatementPattern (resultSizeEstimate=12, resultSizeActual=48)\n"
					+ "      ║  │                 Var (name=d)\n"
					+ "      ║  │                 Var (name=e)\n"
					+ "      ║  │                 Var (name=f)\n"
					+ "      ║  └──StatementPattern (resultSizeEstimate=12, resultSizeActual=2)\n"
					+ "      ║        Var (name=d)\n"
					+ "      ║        Var (name=e)\n"
					+ "      ║        Var (name=f)\n"
					+ "      ╚══ExtensionElem (_const_f5e5585a_uri)\n"
					+ "            ValueConstant (value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type)\n";

			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

	@Disabled // slow test used for debugging
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

		repository.shutDown();

	}

	@Test
	public void testSubQuery() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(SUB_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toString();
			String expected = "Projection (resultSizeActual=4)\n" +
					"╠══ProjectionElemList\n" +
					"║     ProjectionElem \"a\"\n" +
					"╚══Join (HashJoinIteration) (resultSizeActual=4)\n" +
					"   ├──Projection (new scope) (resultSizeActual=4)\n" +
					"   │  ╠══ProjectionElemList\n" +
					"   │  ║     ProjectionElem \"a\"\n" +
					"   │  ╚══StatementPattern (resultSizeActual=4)\n" +
					"   │        Var (name=a)\n" +
					"   │        Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)"
					+ "\n"
					+
					"   │        Var (name=type)\n" +
					"   └──Projection (new scope) (resultSizeActual=2)\n" +
					"      ╠══ProjectionElemList\n" +
					"      ║     ProjectionElem \"a\"\n" +
					"      ╚══LeftJoin (LeftJoinIterator) (resultSizeActual=2)\n" +
					"         ├──Join (JoinIterator) (resultSizeActual=2)\n" +
					"         │  ╠══LeftJoin (new scope) (LeftJoinIterator) (resultSizeActual=12)\n" +
					"         │  ║  ├──SingletonSet (resultSizeActual=1)\n" +
					"         │  ║  └──StatementPattern (resultSizeActual=12)\n" +
					"         │  ║        Var (name=d)\n" +
					"         │  ║        Var (name=e)\n" +
					"         │  ║        Var (name=f)\n" +
					"         │  ╚══Filter (resultSizeActual=2)\n" +
					"         │     ├──Compare (!=)\n" +
					"         │     │     Var (name=c)\n" +
					"         │     │     Var (name=d)\n" +
					"         │     └──Join (JoinIterator) (resultSizeActual=6)\n" +
					"         │        ╠══Filter (resultSizeActual=48)\n" +
					"         │        ║  ├──Compare (!=)\n" +
					"         │        ║  │     Var (name=c)\n" +
					"         │        ║  │     ValueConstant (value=\"<\")\n" +
					"         │        ║  └──StatementPattern (resultSizeActual=48)\n" +
					"         │        ║        Var (name=a)\n" +
					"         │        ║        Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)"
					+ "\n"
					+
					"         │        ║        Var (name=c)\n" +
					"         │        ╚══StatementPattern (resultSizeActual=6)\n" +
					"         │              Var (name=a)\n" +
					"         │              Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)"
					+ "\n"
					+
					"         │              Var (name=d)\n" +
					"         └──StatementPattern (resultSizeActual=2)\n" +
					"               Var (name=d)\n" +
					"               Var (name=e)\n" +
					"               Var (name=f)\n";

			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}

		sailRepository.shutDown();

	}

	@Test
	public void testUnionQuery() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(UNION_QUERY);

			String actual = query.explain(Explanation.Level.Executed).toString();
			String expected = "Projection (resultSizeActual=24)\n"
					+ "╠══ProjectionElemList\n"
					+ "║     ProjectionElem \"a\"\n"
					+ "╚══Join (JoinIterator) (resultSizeActual=24)\n"
					+ "   ├──StatementPattern (costEstimate=1, resultSizeEstimate=4, resultSizeActual=4)\n"
					+ "   │     Var (name=a)\n"
					+ "   │     Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)"
					+ "\n"
					+ "   │     Var (name=type)\n"
					+ "   └──Union (resultSizeActual=24)\n"
					+ "      ╠══Join (JoinIterator) (resultSizeActual=20)\n"
					+ "      ║  ├──StatementPattern (costEstimate=2, resultSizeEstimate=12, resultSizeActual=6)"
					+ "\n"
					+ "      ║  │     Var (name=a)\n"
					+ "      ║  │     Var (name=b)\n"
					+ "      ║  │     Var (name=c2)\n"
					+ "      ║  └──Union (resultSizeActual=20)\n"
					+ "      ║     ╠══Join (JoinIterator) (resultSizeActual=10)\n"
					+ "      ║     ║  ├──StatementPattern (new scope) (costEstimate=2, resultSizeEstimate=4, resultSizeActual=6)"
					+ "\n"
					+ "      ║     ║  │     Var (name=c2)\n"
					+ "      ║     ║  │     Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)"
					+ "\n"
					+ "      ║     ║  │     Var (name=type1)\n"
					+ "      ║     ║  └──StatementPattern (costEstimate=2, resultSizeEstimate=12, resultSizeActual=10)"
					+ "\n"
					+ "      ║     ║        Var (name=a)\n"
					+ "      ║     ║        Var (name=b)\n"
					+ "      ║     ║        Var (name=c)\n"
					+ "      ║     ╚══Join (JoinIterator) (resultSizeActual=10)\n"
					+ "      ║        ├──StatementPattern (new scope) (costEstimate=2, resultSizeEstimate=4, resultSizeActual=6)"
					+ "\n"
					+ "      ║        │     Var (name=c2)\n"
					+ "      ║        │     Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)"
					+ "\n"
					+ "      ║        │     Var (name=type2)\n"
					+ "      ║        └──StatementPattern (costEstimate=2, resultSizeEstimate=12, resultSizeActual=10)"
					+ "\n"
					+ "      ║              Var (name=a)\n"
					+ "      ║              Var (name=b)\n"
					+ "      ║              Var (name=c)\n"
					+ "      ╚══StatementPattern (new scope) (costEstimate=5, resultSizeEstimate=12, resultSizeActual=4)"
					+ "\n"
					+ "            Var (name=type)\n"
					+ "            Var (name=d)\n"
					+ "            Var (name=c)\n";

			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}

		sailRepository.shutDown();

	}

	@Test
	public void testTimeout() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.begin();
			for (int i = 0; i < 1000; i++) {
				connection.add(vf.createBNode(i + ""), RDF.TYPE, vf.createBNode((i + 1) + ""));
				connection.add(vf.createBNode(i + ""), RDF.TYPE, vf.createBNode((i - 1) + ""));

				connection.add(vf.createBNode(i + ""), RDF.TYPE, vf.createBNode((i + 2) + ""));
				connection.add(vf.createBNode(i + ""), RDF.TYPE, vf.createBNode((i - 2) + ""));
			}
			connection.commit();
		}

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(String.join("\n", "",
					"select * where {",
					"	?a (a|^a)* ?type.   ",
					"	FILTER NOT EXISTS{?a (a|^a)* ?type} ",
					"	FILTER NOT EXISTS{?a (a|^a)* ?type} ",
					"	FILTER NOT EXISTS{?a (a|^a)* ?type} ",
					"	FILTER NOT EXISTS{?a (a|^a)* ?type}",
					"	FILTER NOT EXISTS{?a (a|^a)* ?type}",
					"	FILTER NOT EXISTS{?a (a|^a)* ?type}",
					"	FILTER NOT EXISTS{?a (a|^a)* ?type}",
					"	FILTER NOT EXISTS{?a (a|^a)* ?type}",
					"}"));

			query.setMaxExecutionTime(1);

			String actual = query.explain(Explanation.Level.Timed).toString();
			assertThat(actual).contains("Timed out");

		}
		sailRepository.shutDown();

	}

	@Test
	public void testDot() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(TUPLE_QUERY);

			Explanation explain = query.explain(Explanation.Level.Optimized);
			String actual = explain.toDot();
			actual = actual.replaceAll("UUID_\\w+", "UUID");

			String expected = "digraph Explanation {\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Projection</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>ProjectionElemList</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID -> UUID [label=\"\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>ProjectionElem &quot;a&quot;</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>LeftJoin</U></td></tr> <tr><td>Algorithm</td><td>LeftJoinIterator</td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Join</U></td></tr> <tr><td>Algorithm</td><td>JoinIterator</td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>StatementPattern</U></td></tr> <tr><td>Cost estimate</td><td>1</td></tr> <tr><td>Result size estimate</td><td>4</td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID -> UUID [label=\"index 0\"] ;\n" +
					"   UUID -> UUID [label=\"index 1\"] ;\n" +
					"   UUID -> UUID [label=\"index 2\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=a)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=d)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Filter</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Compare (!=)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=c)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=d)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Join</U></td></tr> <tr><td>Algorithm</td><td>HashJoinIteration</td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Filter</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Compare (!=)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=c)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>ValueConstant (value=&quot;&lt;&quot;)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>StatementPattern</U></td></tr> <tr><td>Cost estimate</td><td>2</td></tr> <tr><td>Result size estimate</td><td>4</td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID -> UUID [label=\"index 0\"] ;\n" +
					"   UUID -> UUID [label=\"index 1\"] ;\n" +
					"   UUID -> UUID [label=\"index 2\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=a)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=_const_f5e5585a_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#type, anonymous)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=c)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   subgraph cluster_UUID {\n" +
					"   color=grey\n" +
					"UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>LeftJoin</U></td></tr> <tr><td><B>New scope</B></td><td><B>true</B></td></tr> <tr><td>Cost estimate</td><td>5</td></tr> <tr><td>Result size estimate</td><td>12</td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID -> UUID [label=\"left\"] ;\n" +
					"   UUID -> UUID [label=\"right\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>SingletonSet</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>StatementPattern</U></td></tr> <tr><td>Result size estimate</td><td>12</td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID -> UUID [label=\"index 0\"] ;\n" +
					"   UUID -> UUID [label=\"index 1\"] ;\n" +
					"   UUID -> UUID [label=\"index 2\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=d)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=e)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=f)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"\n" +
					"}\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>StatementPattern</U></td></tr> <tr><td>Result size estimate</td><td>12</td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID -> UUID [label=\"index 0\"] ;\n" +
					"   UUID -> UUID [label=\"index 1\"] ;\n" +
					"   UUID -> UUID [label=\"index 2\"] ;\n" +
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=d)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=e)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"   UUID [label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"#FFFFFF\"><U>Var (name=f)</U></td></tr></table>> shape=plaintext];"
					+ "\n"
					+
					"\n" +
					"}\n";
			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

	@Test
	public void testDotTimed() {
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			Query query = connection.prepareTupleQuery(SUB_QUERY);

			Explanation explain = query.explain(Explanation.Level.Timed);
			String actual = explain.toDot();
			actual = actual.replaceAll("UUID_\\w+", "UUID");

			assertThat(actual).startsWith("digraph Explanation {");
			assertThat(actual).contains(
					"[label=<<table BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\" CELLPADDING=\"3\" ><tr><td COLSPAN=\"2\" BGCOLOR=\"");
			assertThat(actual).contains("Total time actual</td><td BGCOLOR=");
			assertThat(actual).contains("Self time actual</td><td BGCOLOR=\"");
			assertThat(actual).contains("ms</td>");
			assertThat(actual).contains("<U>Projection</U>");
			assertThat(actual).contains("<U>ProjectionElemList</U>");
			assertThat(actual).contains("<U>Join</U>");

		}
		sailRepository.shutDown();

	}

	@Test
	public void testWildcard() {

		String expected = "Projection\n" +
				"╠══ProjectionElemList\n" +
				"║     ProjectionElem \"a\"\n" +
				"║     ProjectionElem \"b\"\n" +
				"║     ProjectionElem \"c\"\n" +
				"╚══StatementPattern (resultSizeEstimate=12)\n" +
				"      Var (name=a)\n" +
				"      Var (name=b)\n" +
				"      Var (name=c)\n";
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			TupleQuery query = connection.prepareTupleQuery("select * where {?a ?b ?c.}");
			String actual = query.explain(Explanation.Level.Optimized).toString();

			assertThat(actual).isEqualToNormalizingNewlines(expected);
		}
		sailRepository.shutDown();

	}

	@Test
	public void testArbitraryLengthPath() {

		String expected = "Projection\n" +
				"╠══ProjectionElemList\n" +
				"║     ProjectionElem \"a\"\n" +
				"║     ProjectionElem \"b\"\n" +
				"║     ProjectionElem \"c\"\n" +
				"║     ProjectionElem \"d\"\n" +
				"╚══Join (JoinIterator)\n" +
				"   ├──StatementPattern (costEstimate=6, resultSizeEstimate=12)\n" +
				"   │     Var (name=a)\n" +
				"   │     Var (name=b)\n" +
				"   │     Var (name=c)\n" +
				"   └──ArbitraryLengthPath (costEstimate=5, resultSizeEstimate=24)\n" +
				"         Var (name=c)\n" +
				"         StatementPattern (resultSizeEstimate=0)\n" +
				"            Var (name=c)\n" +
				"            Var (name=_const_f804988f_uri, value=http://a, anonymous)\n" +
				"            Var (name=d)\n" +
				"         Var (name=d)\n" +
				"";
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		addData(sailRepository);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			TupleQuery query = connection.prepareTupleQuery("select * where {?a ?b ?c. ?c <http://a>* ?d}");
			String actual = query.explain(Explanation.Level.Optimized).toString();

			assertThat(actual).isEqualToNormalizingNewlines(expected);

		}
		sailRepository.shutDown();

	}

}
