/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.solr;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.lucene.DocumentScore;
import org.eclipse.rdf4j.sail.lucene.SearchFields;
import org.eclipse.rdf4j.sail.lucene.SearchQuery;

import com.google.common.collect.Iterables;

/**
 * To be removed, no longer used.
 */
@Deprecated
public class SolrSearchQuery implements SearchQuery {

	private final SolrQuery query;

	private final SolrIndex index;

	public SolrSearchQuery(SolrQuery q, SolrIndex index) {
		this.query = q;
		this.index = index;
	}

	@Override
	public Iterable<? extends DocumentScore> query(Resource resource) throws IOException {
		QueryResponse response;
		if (query.getHighlight()) {
			query.addField("*");
		} else {
			query.addField(SearchFields.URI_FIELD_NAME);
		}
		query.addField("score");
		try {
			if (resource != null) {
				response = index.search(resource, query);
			} else {
				response = index.search(query);
			}
		} catch (SolrServerException e) {
			throw new IOException(e);
		}
		SolrDocumentList results = response.getResults();
		final Map<String, Map<String, List<String>>> highlighting = response.getHighlighting();
		return Iterables.transform(results, (SolrDocument document) -> {
			SolrSearchDocument doc = new SolrSearchDocument(document);
			Map<String, List<String>> docHighlighting = (highlighting != null) ? highlighting.get(doc.getId())
					: null;
			return new SolrDocumentScore(doc, docHighlighting);
		});
	}

	/**
	 * Highlights the given field or all fields if null.
	 */
	@Override
	public void highlight(IRI property) {
		query.setHighlight(true);
		String field = (property != null) ? SearchFields.getPropertyField(property) : "*";
		query.addHighlightField(field);
		query.setHighlightSimplePre(SearchFields.HIGHLIGHTER_PRE_TAG);
		query.setHighlightSimplePost(SearchFields.HIGHLIGHTER_POST_TAG);
		query.setHighlightSnippets(2);
	}

}
