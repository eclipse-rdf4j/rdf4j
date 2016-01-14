/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation.evaluation;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.query.QueryEvaluationException;

/**
 * If the primary cursor is empty, use the alternative cursor.
 * 
 * @author James Leigh
 */
public class AlternativeCursor<E> extends LookAheadIteration<E, QueryEvaluationException> {

	private CloseableIteration<? extends E, QueryEvaluationException> delegate;

	private final CloseableIteration<? extends E, QueryEvaluationException> primary;

	private final CloseableIteration<? extends E, QueryEvaluationException> alternative;

	public AlternativeCursor(CloseableIteration<? extends E, QueryEvaluationException> primary,
			CloseableIteration<? extends E, QueryEvaluationException> alternative)
	{
		super();
		this.alternative = alternative;
		this.primary = primary;
	}

	public void handleClose()
		throws QueryEvaluationException
	{
		primary.close();
		alternative.close();
	}

	public E getNextElement()
		throws QueryEvaluationException
	{
		if (delegate == null) {
			delegate = primary.hasNext() ? primary : alternative;
		}
		return delegate.hasNext() ? delegate.next() : null; // NOPMD
	}

	@Override
	public String toString() {
		String name = getClass().getName().replaceAll("^.*\\.|Cursor$", "");
		return name + "\n\t" + primary.toString() + "\n\t" + alternative.toString();
	}
}
