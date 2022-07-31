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

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtility;

/**
 * @author Håvard Ottestad
 */
public class LiteralComparatorFilter extends FilterPlanNode {

	private final Literal compareTo;

	private final Compare.CompareOp compareOp;

	public LiteralComparatorFilter(PlanNode parent, Literal compareTo, Compare.CompareOp compareOp) {
		super(parent);
		this.compareTo = compareTo;
		this.compareOp = compareOp;

	}

	@Override
	boolean checkTuple(ValidationTuple t) {
		Value literal = t.getValue();
		return QueryEvaluationUtility.compare(compareTo, literal, this.compareOp).orElse(false);
	}

	@Override
	public String toString() {
		return "LiteralComparatorFilter{" +
				"compareTo=" + compareTo +
				", compareOp=" + compareOp +
				'}';
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
		LiteralComparatorFilter that = (LiteralComparatorFilter) o;
		return compareTo.equals(that.compareTo) && compareOp == that.compareOp;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), compareTo, compareOp);
	}
}
