package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleSailStore;

public class ExtensibleConstantEvaluationStatistics extends ExtensibleEvaluationStatistics {
	public ExtensibleConstantEvaluationStatistics(ExtensibleSailStore extensibleSailStore) {
		super(extensibleSailStore);
	}

	@Override
	protected CardinalityCalculator createCardinalityCalculator() {
		return cardinalityCalculator;
	}
	
	CardinalityCalculator cardinalityCalculator = new CardinalityCalculator() {
	};

}
