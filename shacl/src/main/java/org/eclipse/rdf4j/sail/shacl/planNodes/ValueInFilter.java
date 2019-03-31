/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
		return valueSet.contains(t.line.get(1));
	}

	@Override
	public String toString() {
		return "ValueInFilter{" + "valueSet=" + Arrays.toString(valueSet.toArray()) + '}';
	}
}
