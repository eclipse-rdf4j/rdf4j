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

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;

/**
 * @author Håvard Mikkelsen Ottestad
 */
public interface PlanNode {

	CloseableIteration<? extends ValidationTuple> iterator();

	int depth();

	void getPlanAsGraphvizDot(StringBuilder stringBuilder);

	String getId();

	void receiveLogger(ValidationExecutionLogger validationExecutionLogger);

	boolean producesSorted();

	boolean requiresSorted();

	default boolean isGuaranteedEmpty() {
		return false;
	}

}
