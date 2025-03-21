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

import java.util.Objects;
import java.util.Set;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

/**
 * @author Håvard Ottestad
 */
public class ValueInFilter extends FilterPlanNode {

	private final Set<Value> valueSet;

	public ValueInFilter(PlanNode parent, Set<Value> valueSet, ConnectionsGroup connectionsGroup) {
		super(parent, connectionsGroup);
		this.valueSet = valueSet;
	}

	@Override
	boolean checkTuple(Reference t) {
		return valueSet.contains(t.get().getValue());
	}

	@Override
	public String toString() {
		return "ValueInFilter{" + "valueSet=" + Formatter.prefix(valueSet) + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		ValueInFilter that = (ValueInFilter) o;
		return valueSet.equals(that.valueSet);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), valueSet);
	}
}
