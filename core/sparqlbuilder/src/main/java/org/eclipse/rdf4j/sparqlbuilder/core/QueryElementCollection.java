/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sparqlbuilder.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A logical collection of query elements. Provides common functionality for elements which are collections of other
 * elements, especially in printing. Would have loved to have avoided making this public.
 *
 * @param <T> the type of {@link QueryElement}s in the collection
 */
public abstract class QueryElementCollection<T extends QueryElement> implements QueryElement {
	protected Collection<T> elements = new LinkedHashSet<>();
	private String delimiter = "\n";

	protected QueryElementCollection() {
	}

	protected QueryElementCollection(String delimiter) {
		this.delimiter = delimiter;
	}

	protected QueryElementCollection(String delimiter, Collection<T> elements) {
		this.delimiter = delimiter;
		this.elements = elements;
	}

	/**
	 * @return if this collection is empty
	 */
	public boolean isEmpty() {
		return elements.isEmpty();
	}

	@SuppressWarnings("unchecked")
	protected void addElements(T... queryElements) {
		Collections.addAll(elements, queryElements);
	}

	@SuppressWarnings("unchecked")
	protected <O> void addElements(Function<? super O, ? extends T> mapper, O... os) {
		Arrays.stream(os).map(mapper).forEach(elements::add);
	}

	@Override
	public String getQueryString() {
		return elements.stream().map(QueryElement::getQueryString).collect(Collectors.joining(delimiter));
	}
}
