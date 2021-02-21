/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil;

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

		try {
			return QueryEvaluationUtil.compare(compareTo, literal, this.compareOp);
		} catch (ValueExprEvaluationException e) {
			return false;
		}

	}

	@Override
	public String toString() {
		return "LiteralComparatorFilter{" +
				"compareTo=" + compareTo +
				", compareOp=" + compareOp +
				'}';
	}
}
