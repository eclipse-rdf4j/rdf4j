package org.eclipse.rdf4j.query.algebra.evaluation.impl;

import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizer;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryOptimizerTest;
import org.eclipse.rdf4j.query.algebra.evaluation.optimizer.OrderLimitOptimizer;

public class OrderLimitOptimizerTest extends QueryOptimizerTest {

	@Override
	public QueryOptimizer getOptimizer() {
		return new OrderLimitOptimizer();
	}

}
