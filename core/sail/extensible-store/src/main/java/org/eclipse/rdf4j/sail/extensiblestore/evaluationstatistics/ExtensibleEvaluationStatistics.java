package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleSailStore;

public abstract class ExtensibleEvaluationStatistics extends EvaluationStatistics {


	final ExtensibleSailStore extensibleSailStore;

	public ExtensibleEvaluationStatistics(ExtensibleSailStore extensibleSailStore) {
		this.extensibleSailStore = extensibleSailStore;
	}

	@Override
	abstract protected CardinalityCalculator createCardinalityCalculator();

}
