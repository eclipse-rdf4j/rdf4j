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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

class SingleValueToListTransformer<E> implements Function<E, List<? extends E>> {

	private static final SingleValueToListTransformer<?> INSTANCE = new SingleValueToListTransformer<>();

	@SuppressWarnings("unchecked")
	static <E> Iterator<List<? extends E>> transform(Iterator<E> iter) {
		return Iterators.transform(iter, (SingleValueToListTransformer<E>) INSTANCE);
	}

	@Override
	public List<? extends E> apply(E v) {
		return Collections.singletonList(v);
	}
}
