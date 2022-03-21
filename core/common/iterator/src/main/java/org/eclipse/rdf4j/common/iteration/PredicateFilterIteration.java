/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.function.Predicate;

public class PredicateFilterIteration<E, X extends Exception> extends FilterIteration<E, X> {

	private final Predicate<E> filter;

	public PredicateFilterIteration(CloseableIteration<? extends E, X> stIter1, Predicate<E> filter) {
		super(stIter1);
		this.filter = filter;
	}

	@Override
	protected boolean accept(E object) throws X {
		return filter.test(object);
	}
}
