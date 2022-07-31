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

import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SPIF;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException;
import org.eclipse.rdf4j.spin.function.InverseMagicProperty;

public class For implements InverseMagicProperty {

	@Override
	public String getURI() {
		return SPIF.FOR_PROPERTY.toString();
	}

	@Override
	public CloseableIteration<? extends List<? extends Value>, QueryEvaluationException> evaluate(
			final ValueFactory valueFactory, Value... args) throws QueryEvaluationException {
		if (args.length != 2) {
			throw new ValueExprEvaluationException(
					String.format("%s requires 2 arguments, got %d", getURI(), args.length));
		}
		if (!(args[0] instanceof Literal)) {
			throw new ValueExprEvaluationException("First list element must be a literal");
		}
		if (!(args[1] instanceof Literal)) {
			throw new ValueExprEvaluationException("Second list element must be a literal");
		}
		final int from = ((Literal) args[0]).intValue();
		final int to = ((Literal) args[1]).intValue();
		return new CloseableIteratorIteration<>(
				SingleValueToListTransformer.transform(new Iterator<>() {

					int value = from;

					@Override
					public boolean hasNext() {
						return (value <= to);
					}

					@Override
					public Value next() {
						return valueFactory.createLiteral(value++);
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				}));
	}
}
