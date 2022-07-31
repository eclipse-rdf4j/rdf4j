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
package org.eclipse.rdf4j.spin.function;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SPL;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryPreparer;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

public class ObjectFunction extends AbstractSpinFunction implements Function {

	public ObjectFunction() {
		super(SPL.OBJECT_FUNCTION.toString());
	}

	@Override
	public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
		QueryPreparer qp = getCurrentQueryPreparer();
		if (args.length != 2) {
			throw new ValueExprEvaluationException(
					String.format("%s requires 2 arguments, got %d", getURI(), args.length));
		}
		Value subj = args[0];
		if (!(subj instanceof Resource)) {
			throw new ValueExprEvaluationException("First argument must be a subject");
		}
		Value pred = args[1];
		if (!(pred instanceof IRI)) {
			throw new ValueExprEvaluationException("Second argument must be a predicate");
		}

		try {
			try (CloseableIteration<? extends Statement, QueryEvaluationException> stmts = qp.getTripleSource()
					.getStatements((Resource) subj, (IRI) pred, null)) {
				if (stmts.hasNext()) {
					return stmts.next().getObject();
				} else {
					throw new ValueExprEvaluationException("No value");
				}
			}
		} catch (QueryEvaluationException e) {
			throw new ValueExprEvaluationException(e);
		}
	}
}
