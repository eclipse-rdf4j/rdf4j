/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import java.util.Arrays;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;

/**
 * @author Håvard Ottestad
 */
public class ValueInFilter extends FilterPlanNode {

	private final Set<Value> valueSet;

	public ValueInFilter(PlanNode parent, Set<Value> valueSet) {
		super(parent);
		this.valueSet = valueSet;
	}

	@Override
	boolean checkTuple(Tuple t) {
		return valueSet.contains(t.getLine().get(1));
	}

	@Override
	public String toString() {
		return "ValueInFilter{" + "valueSet=" + Arrays.toString(valueSet.stream().map(Formatter::prefix).toArray())
				+ '}';
	}
}
