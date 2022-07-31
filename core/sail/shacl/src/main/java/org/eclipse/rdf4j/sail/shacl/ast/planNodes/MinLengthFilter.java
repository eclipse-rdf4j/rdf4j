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

import org.eclipse.rdf4j.model.Value;

/**
 * @author Håvard Ottestad
 */
public class MinLengthFilter extends FilterPlanNode {

	private final long minLength;

	public MinLengthFilter(PlanNode parent, long minLength) {
		super(parent);
		this.minLength = minLength;
	}

	@Override
	boolean checkTuple(ValidationTuple t) {
		Value literal = t.getValue();

		return literal.stringValue().length() >= minLength;
	}

	@Override
	public String toString() {
		return "MinLengthFilter{" + "minLength=" + minLength + '}';
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
		MinLengthFilter that = (MinLengthFilter) o;
		return minLength == that.minLength;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), minLength);
	}
}
