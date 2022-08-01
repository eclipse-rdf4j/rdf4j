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
package org.eclipse.rdf4j.sail.elasticsearch;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.lucene.DocumentScore;
import org.eclipse.rdf4j.sail.lucene.SearchFields;
import org.eclipse.rdf4j.sail.lucene.SearchQuery;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

/**
 * To be removed, no longer used.
 */
@Deprecated
public class ElasticsearchQuery implements SearchQuery {

	private final SearchRequestBuilder request;

	private final QueryBuilder qb;

	private final ElasticsearchIndex index;

	public ElasticsearchQuery(SearchRequestBuilder request, QueryBuilder qb, ElasticsearchIndex index) {
		this.request = request;
		this.qb = qb;
		this.index = index;
	}

	@Override
	public Iterable<? extends DocumentScore> query(Resource resource) throws IOException {
		SearchHits hits;
		if (resource != null) {
			hits = index.search(resource, request, qb);
		} else {
			hits = index.search(request, qb);
		}
		return Iterables.transform(hits, new Function<>() {

			@Override
			public DocumentScore apply(SearchHit hit) {
				return new ElasticsearchDocumentScore(hit, null);
			}
		});
	}

	/**
	 * Highlights the given field or all fields if null.
	 */
	@Override
	public void highlight(IRI property) {
		String field = (property != null)
				? ElasticsearchIndex.toPropertyFieldName(SearchFields.getPropertyField(property))
				: ElasticsearchIndex.ALL_PROPERTY_FIELDS;
		HighlightBuilder hb = new HighlightBuilder();
		hb.field(field);
		hb.preTags(SearchFields.HIGHLIGHTER_PRE_TAG);
		hb.postTags(SearchFields.HIGHLIGHTER_POST_TAG);
		// Elastic Search doesn't really have the same support for fragments as Lucene.
		// So, we have to get back the whole highlighted value (comma-separated if it is a list)
		// and then post-process it into fragments ourselves.
		hb.numOfFragments(0);
		request.highlighter(hb);
	}
}
