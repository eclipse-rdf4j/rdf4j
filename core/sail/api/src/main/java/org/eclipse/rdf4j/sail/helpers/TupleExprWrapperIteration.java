/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.helpers;

import java.util.stream.Stream;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.algebra.TupleExpr;

/**
 * This iteration is used to debug issues with the TupleExpr that was used to generate the wrapped iteration.
 * AbstractSailConnection will use this class when evaluating a query if assertions are enabled.
 *
 * @author Håvard M. Ottestad
 */
class TupleExprWrapperIteration<T extends BindingSet, X extends Exception> implements CloseableIteration<T> {

	private final CloseableIteration<T> delegate;
	private final TupleExpr tupleExpr;
	private final TupleExpr tupleExprClone;

	public TupleExprWrapperIteration(CloseableIteration<T> delegate, TupleExpr tupleExpr) {
		this.delegate = delegate;
		this.tupleExpr = tupleExpr;
		this.tupleExprClone = tupleExpr.clone();
	}

	@Override
	public Stream<T> stream() {
		return delegate.stream();
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public boolean hasNext() {
		return delegate.hasNext();
	}

	@Override
	public T next() {
		return delegate.next();
	}

	@Override
	public void remove() {
		delegate.remove();
	}

	public TupleExpr getTupleExpr() {
		return tupleExpr;
	}

	public TupleExpr getTupleExprClone() {
		return tupleExprClone;
	}
}
