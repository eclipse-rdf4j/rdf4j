/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.mock;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Tuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.ValidationExecutionLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Håvard Ottestad
 */
public class MockConsumePlanNode {

	private static final ValidationExecutionLogger VALIDATION_EXECUTION_LOGGER = new ValidationExecutionLogger();
	PlanNode innerNode;

	public MockConsumePlanNode(PlanNode innerNode) {
		this.innerNode = innerNode;
		innerNode.receiveLogger(VALIDATION_EXECUTION_LOGGER);
	}

	public List<Tuple> asList() {

		try (CloseableIteration<Tuple, SailException> iterator = innerNode.iterator()) {

			List<Tuple> ret = new ArrayList<>();

			while (iterator.hasNext()) {
				ret.add(iterator.next());
			}

			VALIDATION_EXECUTION_LOGGER.flush();

			return ret;
		}
	}
}
