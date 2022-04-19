/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.spin.function.list;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.LIST;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.algebra.evaluation.function.TupleFunction;

public class Index implements TupleFunction {

	@Override
	public String getURI() {
		return LIST.INDEX.toString();
	}

	@Override
	public CloseableIteration<? extends List<? extends Value>, QueryEvaluationException> evaluate(
			final ValueFactory valueFactory, final Value... args) throws QueryEvaluationException {
		return new CloseableIteratorIteration<>(
				new Iterator<List<Value>>() {

					int pos = 0;

					@Override
					public boolean hasNext() {
						return (pos < args.length);
					}

					@Override
					public List<Value> next() {
						return Arrays.asList(valueFactory.createLiteral(pos), args[pos++]);
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				});
	}
}
