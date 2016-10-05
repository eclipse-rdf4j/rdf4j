/**
 * Copyright (c) 2015 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.sail.lucene;

public class QueryContext {

	private static final ThreadLocal<SearchIndex> searchIndex = new ThreadLocal<SearchIndex>();

	public static SearchIndex getSearchIndex() {
		return searchIndex.get();
	}

	public static QueryContext begin(SearchIndex qp) {
		return new QueryContext(qp);
	}

	private final SearchIndex previous;

	private QueryContext(SearchIndex qp) {
		this.previous = searchIndex.get();
		searchIndex.set(qp);
	}

	public void end() {
		searchIndex.remove();
		if (previous != null) {
			searchIndex.set(previous);
		}
	}
}
