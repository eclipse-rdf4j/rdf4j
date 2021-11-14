package org.eclipse.rdf4j.query.algebra.evaluation;

import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;

public interface QueryOptimizerFunctionalInterface {

	QueryOptimizer getOptimizer(EvaluationStrategy strategy, TripleSource tripleSource,
			EvaluationStatistics evaluationStatistics);

}
