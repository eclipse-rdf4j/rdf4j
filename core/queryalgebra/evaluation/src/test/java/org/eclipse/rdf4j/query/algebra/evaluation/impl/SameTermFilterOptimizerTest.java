/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.Extension;
import org.eclipse.rdf4j.query.algebra.SameTerm;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerTest;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.SameTermFilterOptimizer;
import org.eclipse.rdf4j.query.algebra.helpers.collectors.StatementPatternCollector;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.junit.jupiter.api.Test;

/**
 * @author jeen
 */
public class SameTermFilterOptimizerTest extends QueryOptimizerTest {

	@Override
	public SameTermFilterOptimizer getOptimizer() {
		return new SameTermFilterOptimizer();
	}

	/**
	 * A VALUES column with an UNDEF row is not an assured binding name, but it still binds the variable on the other
	 * rows: renaming the variable away would drop that constraint.
	 */
	@Test
	public void valuesColumnWithUndefRowBlocksInlining() {
		TupleExpr expr = QueryParserUtil.parseTupleQuery(QueryLanguage.SPARQL,
				"SELECT * WHERE { VALUES ?y { <urn:d> UNDEF } ?s ?p ?x . ?s ?q ?y FILTER(sameTerm(?x, ?y)) }", null)
				.getTupleExpr();

		getOptimizer().optimize(expr, null, EmptyBindingSet.getInstance());

		assertThat(StatementPatternCollector.process(expr))
				.as("the pattern reading ?y must still bind the VALUES variable")
				.anyMatch(sp -> "y".equals(sp.getObjectVar().getName()));
		assertThat(expr.toString())
				.as("the filter is kept instead of being rewritten into an Extension")
				.contains(SameTerm.class.getSimpleName())
				.doesNotContain(Extension.class.getSimpleName());
	}

}
