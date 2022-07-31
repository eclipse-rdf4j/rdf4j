/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.mock;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationExecutionLogger;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationTuple;

/**
 * @author Håvard Ottestad
 */
public class MockConsumePlanNode {

	// set to true to enable logging
	private final ValidationExecutionLogger VALIDATION_EXECUTION_LOGGER = ValidationExecutionLogger.getInstance(false);

	PlanNode innerNode;

	public MockConsumePlanNode(PlanNode innerNode) {
		this.innerNode = innerNode;
		innerNode.receiveLogger(VALIDATION_EXECUTION_LOGGER);
	}

	public List<ValidationTuple> asList() {

		try (CloseableIteration<? extends ValidationTuple, SailException> iterator = innerNode.iterator()) {

			List<ValidationTuple> ret = new ArrayList<>();

			while (iterator.hasNext()) {
				ret.add(iterator.next());
			}

			VALIDATION_EXECUTION_LOGGER.flush();

			return ret;
		}
	}
}
