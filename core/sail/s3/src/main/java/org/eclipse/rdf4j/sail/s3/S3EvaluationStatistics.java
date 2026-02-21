/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3;

import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;

/**
 * Evaluation statistics for the S3 sail. Currently uses the base class's default cardinality estimation. This can be
 * enhanced later to query the actual storage for more accurate estimates.
 */
class S3EvaluationStatistics extends EvaluationStatistics {

	@Override
	protected CardinalityCalculator createCardinalityCalculator() {
		return new S3CardinalityCalculator();
	}

	protected class S3CardinalityCalculator extends CardinalityCalculator {
		// Uses the default cardinality estimation from the base class.
		// Can be enhanced to consult S3ValueStore and storage for accurate estimates.
	}
}
