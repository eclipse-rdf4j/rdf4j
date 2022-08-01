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

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleSailStore;

@Experimental
public abstract class ExtensibleEvaluationStatistics extends EvaluationStatistics {

	final ExtensibleSailStore extensibleSailStore;

	public ExtensibleEvaluationStatistics(ExtensibleSailStore extensibleSailStore) {
		this.extensibleSailStore = extensibleSailStore;
	}

	@Override
	abstract protected CardinalityCalculator createCardinalityCalculator();

}
