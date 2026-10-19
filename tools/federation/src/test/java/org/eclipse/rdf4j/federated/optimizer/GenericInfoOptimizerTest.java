/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.optimizer;

import static org.mockito.Mockito.when;

import org.eclipse.rdf4j.federated.FedXBaseTest;
import org.eclipse.rdf4j.federated.FedXConfig;
import org.eclipse.rdf4j.federated.FederationContext;
import org.eclipse.rdf4j.federated.algebra.NJoin;
import org.eclipse.rdf4j.federated.algebra.TripleRefStatementPattern;
import org.eclipse.rdf4j.federated.structures.QueryInfo;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GenericInfoOptimizerTest extends FedXBaseTest {

	@Mock
	private QueryInfo queryInfo;

	@Mock
	private FederationContext federationContext;

	@BeforeEach
	public void setUp() {
		when(queryInfo.getFederationContext()).thenReturn(federationContext);
		when(federationContext.getConfig()).thenReturn(new FedXConfig().withEnableTripleRefSupport(true));
	}

	/**
	 * Verifies that a query containing a reification triple pattern (RDF 1.2 style) is optimized into a
	 * {@link TripleRefStatementPattern} node rather than left as a Join over a StatementPattern and a TripleRef.
	 */
	@Test
	public void testTripleRefJoinIsOptimizedToTripleRefStatementPattern() {
		String query = """
				PREFIX ex:    <http://www.example.org/>
				PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
				SELECT * WHERE {
				   ?node rdf:reifies <<( ex:bob ex:jobTitle ?jobTitle )>>
				}
				""";

		ParsedTupleQuery parsedQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
		TupleExpr tupleExpr = parsedQuery.getTupleExpr();

		GenericInfoOptimizer optimizer = new GenericInfoOptimizer(queryInfo);
		optimizer.optimize(tupleExpr);

		String expectedQueryPlan = """
				QueryRoot
				   Projection
				      ProjectionElemList
				         ProjectionElem "node"
				         ProjectionElem "jobTitle"
				      TripleRefStatementPattern
				         Var (name=node)
				         Var (name=_const_9fd69527_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies, anonymous)
				         Var (name=_anon_2ce88c483427245f9ab5869662768984301, anonymous)
				         TripleRef
				            Var (name=_const_ad4a55dd_uri, value=http://www.example.org/bob, anonymous)
				            Var (name=_const_d9dc6573_uri, value=http://www.example.org/jobTitle, anonymous)
				            Var (name=jobTitle)
				            Var (name=_anon_2ce88c483427245f9ab5869662768984301, anonymous)
								""";

		assertQueryPlanEquals(expectedQueryPlan, tupleExpr.toString());
	}

	/**
	 * Verifies that a query with an embedded triple as the subject (RDF 1.2 style) is optimized into a
	 * {@link TripleRefStatementPattern} node rather than left as a Join over a TripleRef and a StatementPattern.
	 */
	@Test
	public void testEmbeddedTripleSubjectIsOptimizedToTripleRefStatementPattern() {
		String query = """
				PREFIX ex:    <http://www.example.org/>
				PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
				SELECT * WHERE {
				   << ex:bob ex:jobTitle ?jobTitle >> ex:accordingTo ?source
				}
				""";

		ParsedTupleQuery parsedQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
		TupleExpr tupleExpr = parsedQuery.getTupleExpr();

		GenericInfoOptimizer optimizer = new GenericInfoOptimizer(queryInfo);
		optimizer.optimize(tupleExpr);

		String expectedQueryPlan = """
				QueryRoot
				   Projection
				      ProjectionElemList
				         ProjectionElem "jobTitle"
				         ProjectionElem "source"
				      NJoin
				         TripleRefStatementPattern
				            Var (name=_anon_18a7b5c1b9b9045c4b34845786c50a85f0, anonymous)
				            Var (name=reifies, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies, anonymous)
				            Var (name=_anon_28a7b5c1b9b9045c4b34845786c50a85f01, anonymous)
				            ReifiedTripleRef
				               Var (name=_const_ad4a55dd_uri, value=http://www.example.org/bob, anonymous)
				               Var (name=_const_d9dc6573_uri, value=http://www.example.org/jobTitle, anonymous)
				               Var (name=jobTitle)
				               Var (name=_anon_28a7b5c1b9b9045c4b34845786c50a85f01, anonymous)
				               Var (name=_anon_18a7b5c1b9b9045c4b34845786c50a85f0, anonymous)
				         StatementPattern
				            Var (name=_anon_18a7b5c1b9b9045c4b34845786c50a85f0, anonymous)
				            Var (name=_const_50f03f65_uri, value=http://www.example.org/accordingTo, anonymous)
				            Var (name=source)
								""";

		assertQueryPlanEquals(expectedQueryPlan, tupleExpr.toString());
	}

	/**
	 * Verifies that a query with a reification triple pattern combined with a further statement (RDF 1.2 style) is
	 * optimized into an {@link NJoin} that contains a {@link TripleRefStatementPattern} as a direct child.
	 */
	@Test
	public void testTripleRefInJoinIsOptimizedToNJoinWithTripleRefStatementPattern() {
		String query = """
				PREFIX ex:    <http://www.example.org/>
				PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
				SELECT * WHERE {
				   ?node rdf:reifies <<( ex:bob ex:jobTitle ?jobTitle )>> ;
				      ex:accordingTo ?source
				}
				""";

		ParsedTupleQuery parsedQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);
		TupleExpr tupleExpr = parsedQuery.getTupleExpr();

		GenericInfoOptimizer optimizer = new GenericInfoOptimizer(queryInfo);
		optimizer.optimize(tupleExpr);

		String expectedQueryPlan = """
				QueryRoot
				   Projection
				      ProjectionElemList
				         ProjectionElem "node"
				         ProjectionElem "jobTitle"
				         ProjectionElem "source"
				      NJoin
				         TripleRefStatementPattern
				            Var (name=node)
				            Var (name=_const_9fd69527_uri, value=http://www.w3.org/1999/02/22-rdf-syntax-ns#reifies, anonymous)
				            Var (name=_anon_22aef978a7edb4a9ba38159091244d07501, anonymous)
				            TripleRef
				               Var (name=_const_ad4a55dd_uri, value=http://www.example.org/bob, anonymous)
				               Var (name=_const_d9dc6573_uri, value=http://www.example.org/jobTitle, anonymous)
				               Var (name=jobTitle)
				               Var (name=_anon_22aef978a7edb4a9ba38159091244d07501, anonymous)
				         StatementPattern
				            Var (name=node)
				            Var (name=_const_50f03f65_uri, value=http://www.example.org/accordingTo, anonymous)
				            Var (name=source)
								""";

		assertQueryPlanEquals(expectedQueryPlan, tupleExpr.toString());
	}

	@Override
	protected FederationContext federationContext() {
		// Not yet required for this test
		throw new UnsupportedOperationException();
	}
}
