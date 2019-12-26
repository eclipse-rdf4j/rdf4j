package org.eclipse.rdf4j.sail.extensiblestore;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;

public class ExtensibleEvaluationStatistics extends EvaluationStatistics {


	final ExtensibleSailStore extensibleSailStore;

	public ExtensibleEvaluationStatistics(ExtensibleSailStore extensibleSailStore) {
		this.extensibleSailStore = extensibleSailStore;
	}

	@Override
	protected CardinalityCalculator createCardinalityCalculator() {
		return new ExtensibleCardinalityCalculator();
	}

	protected class ExtensibleCardinalityCalculator extends CardinalityCalculator {

		@Override
		public double getCardinality(StatementPattern sp) {

			extensibleSailStore.getExplicitSailSource().dataset(IsolationLevels.NONE)

			return 0.5;
		}
	}
}
