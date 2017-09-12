/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.shacl.mock;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.plan.GroupPlanNode;
import org.eclipse.rdf4j.plan.Tuple;
import org.eclipse.rdf4j.sail.SailException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Håvard Ottestad
 */

public class MockConsumeGroupPlanNode {

	GroupPlanNode innerNode;

	public MockConsumeGroupPlanNode(GroupPlanNode innerNode) {
		this.innerNode = innerNode;
	}

	public List<List<Tuple>> asList() {

		CloseableIteration<List<Tuple>, SailException> iterator = innerNode.iterator();

		List<List<Tuple>> ret = new ArrayList<>();

		while (iterator.hasNext()) {
			ret.add(iterator.next());
		}

		return ret;

	}

}



