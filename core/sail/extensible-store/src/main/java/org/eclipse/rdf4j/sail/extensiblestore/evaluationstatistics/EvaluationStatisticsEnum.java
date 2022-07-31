/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import java.util.function.Function;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleSailStore;

/**
 * Enum to support multiple different EvaluationStatistics implementations. The user can control which is used by
 * overriding getEvalStats() in the ExtensibleStore.
 */
@Experimental
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
