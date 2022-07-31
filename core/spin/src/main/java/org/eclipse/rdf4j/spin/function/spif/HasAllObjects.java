/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.spin.function.spif;

import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryPreparer;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;
import org.eclipse.rdf4j.query.algebra.evaluation.util.TripleSources;
import org.eclipse.rdf4j.spin.function.AbstractSpinFunction;

public class HasAllObjects extends AbstractSpinFunction implements Function {

	public HasAllObjects() {
		super(SPIF.HAS_ALL_OBJECTS_FUNCTION.stringValue());
	}

	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		QueryPreparer qp = getCurrentQueryPreparer();
		if (args.length != 3) {
			throw new ValueExprEvaluationException(
					String.format("%s requires 3 argument, got %d", getURI(), args.length));
		}
		Resource subj = (Resource) args[0];
		IRI pred = (IRI) args[1];
		Resource list = (Resource) args[2];
		try {
			Iteration<Value, QueryEvaluationException> iter = TripleSources.list(list, qp.getTripleSource());
			while (iter.hasNext()) {
				Value obj = iter.next();
				if (TripleSources.single(subj, pred, obj, qp.getTripleSource()) == null) {
					return BooleanLiteral.FALSE;
				}
			}
		} catch (QueryEvaluationException e) {
			throw new ValueExprEvaluationException(e);
		}
		return BooleanLiteral.TRUE;
	}
}
