/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.plan;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

import java.util.List;

/**
 * @author Heshan Jayasinghe
 */
public class MinCountValidator implements PlanNode {

	GroupPlanNode groupby;

	int minCount;

	public MinCountValidator(GroupPlanNode groupBy, int minCount) {
		this.groupby = groupBy;
		this.minCount = minCount;
	}
//
//	@Override
//	public boolean validate() {
//		CloseableIteration<List<Tuple>, SailException> groupByIterator = groupby.iterator();
//		while (groupByIterator.hasNext()) {
//			List<Tuple> tuple = groupByIterator.next();
//			if (tuple.size() < minCount) {
//				return false;
//			}
//		}
//		return true;
//	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return null;
	}

}
