package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleSailStore;

import java.util.function.Function;

public enum EvaluationStatisticsEnum {

	direct("Looks up the count directly in the underlying data structure.", ExtensibleDirectEvaluationStatistics::new),
	constant("Uses constant values instead of statistics.", ExtensibleConstantEvaluationStatistics::new),
	dynamic("Continually keeps dynamic estimates on the counts of various statement patterns.",
			ExtensibleDynamicEvaluationStatistics::new);

	private final Function<ExtensibleSailStore, ExtensibleEvaluationStatistics> evaluationStatisticsSupplier;

	EvaluationStatisticsEnum(String comment,
			Function<ExtensibleSailStore, ExtensibleEvaluationStatistics> evaluationStatisticsSupplier) {
		this.evaluationStatisticsSupplier = evaluationStatisticsSupplier;
	}

	public ExtensibleEvaluationStatistics getInstance(ExtensibleSailStore extensibleSailStore) {
		return evaluationStatisticsSupplier.apply(extensibleSailStore);
	}
}
