/******************************************************************************* 
 * Copyright (c) 2021 Eclipse RDF4J contributors. 
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Distribution License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/org/documents/edl-v10.php. 
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.Values.iri;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.ArbitraryLengthPath;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.LeftJoin;
import org.eclipse.rdf4j.query.algebra.Projection;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerTest;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.parser.ParsedTupleQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Jeen
 */
public class BindingSetAssignmentInlinerTest extends QueryOptimizerTest {

	@Test
	public void testOptimizeAssignsVars() {
		String query = "select * \n"
				+ "where { values ?z { <urn:z1> } \n"
				+ "        ?x <urn:pred1> ?y ; \n"
				+ "           (<urn:pred2>/<urn:pred3>)* ?z . \n"
				+ "}";

		ParsedTupleQuery parsedQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);

		QueryOptimizer optimizer = getOptimizer();
		optimizer.optimize(parsedQuery.getTupleExpr(), new SimpleDataset(), EmptyBindingSet.getInstance());

		TupleExpr optimizedTree = parsedQuery.getTupleExpr();
		assertThat(optimizedTree).isInstanceOf(Projection.class);

		Projection projection = (Projection) optimizedTree;

		Join join = (Join) projection.getArg();
		assertThat(join.getRightArg()).isInstanceOf(ArbitraryLengthPath.class);

		ArbitraryLengthPath path = (ArbitraryLengthPath) join.getRightArg();
		assertThat(path.getObjectVar().getName()).isEqualTo("z");
		assertThat(path.getObjectVar().getValue()).isEqualTo(iri("urn:z1"));
	}

	@Test
	public void testOptimize_leftJoin() {
		String query = "PREFIX : <http://example.org/> \n"
				+ "PREFIX foaf: <http://xmlns.com/foaf/0.1/> \n"
				+ "SELECT ?s ?o1 ?o2\n"
				+ "{\n"
				+ "  ?s ?p1 ?o1 \n"
				+ "  OPTIONAL { ?s foaf:knows ?o2 }\n"
				+ "} VALUES (?o2) {\n"
				+ " (:b)\n"
				+ "}";
		ParsedTupleQuery parsedQuery = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL, query, null);

		QueryOptimizer optimizer = getOptimizer();
		optimizer.optimize(parsedQuery.getTupleExpr(), new SimpleDataset(), EmptyBindingSet.getInstance());

		TupleExpr optimizedTree = parsedQuery.getTupleExpr();

		assertThat(optimizedTree).isInstanceOf(Projection.class);

		Projection projection = (Projection) optimizedTree;

		Join join = (Join) projection.getArg();
		assertThat(join.getRightArg()).isInstanceOf(LeftJoin.class);
		LeftJoin optional = (LeftJoin) join.getRightArg();

		Var o2 = ((StatementPattern) optional.getRightArg()).getObjectVar();
		assertThat(o2.getName()).isEqualTo("o2");
		assertThat(o2.getValue()).isNull();
	}

	@Override
	public QueryOptimizer getOptimizer() {
		return new BindingSetAssignmentInliner();
	}
}
