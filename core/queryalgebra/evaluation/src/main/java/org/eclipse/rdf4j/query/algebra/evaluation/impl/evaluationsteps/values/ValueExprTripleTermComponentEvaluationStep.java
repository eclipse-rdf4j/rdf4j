/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.evaluationsteps.values;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.TripleComponent;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

public class ValueExprTripleTermComponentEvaluationStep implements QueryValueEvaluationStep {
	private final QueryValueEvaluationStep tripleTerm;
	private final TripleComponent.Role role;

	public ValueExprTripleTermComponentEvaluationStep(QueryValueEvaluationStep tripleTerm, TripleComponent.Role role) {
		this.tripleTerm = tripleTerm;
		this.role = role;
	}

	@Override
	public Value evaluate(BindingSet bindings) throws QueryEvaluationException {
		Value boundTripleTerm = tripleTerm.evaluate(bindings);
		if (!(boundTripleTerm instanceof TripleTerm)) {
			throw new ValueExprEvaluationException("no tripleTerm value");
		}
		if (role == TripleComponent.Role.SUBJECT) {
			return ((TripleTerm) boundTripleTerm).getSubject();
		} else if (role == TripleComponent.Role.PREDICATE) {
			return ((TripleTerm) boundTripleTerm).getPredicate();
		} else if (role == TripleComponent.Role.OBJECT) {
			return ((TripleTerm) boundTripleTerm).getObject();
		} else {
			throw new ValueExprEvaluationException("unsupported role: " + role);
		}
	}
}
