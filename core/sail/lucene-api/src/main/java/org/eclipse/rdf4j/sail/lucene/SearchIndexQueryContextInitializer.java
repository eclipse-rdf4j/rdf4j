/**
 * Copyright (c) 2015 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.eclipse.rdf4j.sail.lucene;

import org.eclipse.rdf4j.query.algebra.evaluation.QueryContext;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryContextInitializer;

public class SearchIndexQueryContextInitializer implements QueryContextInitializer {

	private static final String SEARCH_INDEX_ATTRIBUTE = SearchIndex.class.getName();

	private final SearchIndex searchIndex;

	public static void init(QueryContext qctx, SearchIndex searchIndex) {
		qctx.setAttribute(SEARCH_INDEX_ATTRIBUTE, searchIndex);
	}

	public static SearchIndex getSearchIndex(QueryContext qctx) {
		return qctx.getAttribute(SEARCH_INDEX_ATTRIBUTE);
	}

	public SearchIndexQueryContextInitializer(SearchIndex searchIndex) {
		this.searchIndex = searchIndex;
	}

	@Override
	public void init(QueryContext qctx) {
		init(qctx, searchIndex);
	}

	@Override
	public void destroy(QueryContext qctx) {
	}

}
