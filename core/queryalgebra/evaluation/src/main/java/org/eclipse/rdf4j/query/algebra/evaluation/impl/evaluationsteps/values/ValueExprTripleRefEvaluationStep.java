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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryValueEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;

public final class ValueExprTripleRefEvaluationStep implements QueryValueEvaluationStep {
	private final QueryValueEvaluationStep subject;
	private final ValueFactory valueFactory;
	private final QueryValueEvaluationStep predicate;
	private final QueryValueEvaluationStep object;

	public ValueExprTripleRefEvaluationStep(QueryValueEvaluationStep subject, ValueFactory valueFactory,
			QueryValueEvaluationStep predicate, QueryValueEvaluationStep object) {
		this.subject = subject;
		this.valueFactory = valueFactory;
		this.predicate = predicate;
		this.object = object;
	}

	@Override
	public Value evaluate(BindingSet bindings) throws ValueExprEvaluationException, QueryEvaluationException {
		Value subj = subject.evaluate(bindings);
		if (!(subj instanceof Resource)) {
			throw new ValueExprEvaluationException("no subject value");
		}
		Value pred = predicate.evaluate(bindings);
		if (!(pred instanceof IRI)) {
			throw new ValueExprEvaluationException("no predicate value");
		}
		Value obj = object.evaluate(bindings);
		if (obj == null) {
			throw new ValueExprEvaluationException("no object value");
		}
		return valueFactory.createTriple((Resource) subj, (IRI) pred, obj);
	}
}