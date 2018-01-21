package org.eclipse.rdf4j.spanqit.core;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A logical collection of query elements. Provides common functionality for
 * elements which are collections of other elements, especially in printing.
 * Would have loved to have avoided making this public.
 *
 * @param <T>
 *            the type of {@link QueryElement}s in the collection
 */
public abstract class QueryElementCollection<T extends QueryElement> implements QueryElement {
	protected Collection<T> elements = new HashSet<>();
	private String delimiter = "\n";

	protected QueryElementCollection() { }

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
	protected <O> void addElements(Function<O, T> mapper, O... os) {
		Arrays.stream(os).map(mapper).forEach(elements::add);
	}

	@Override
	public String getQueryString() {
		return elements.stream().map(QueryElement::getQueryString).collect(Collectors.joining(delimiter));
	}
}