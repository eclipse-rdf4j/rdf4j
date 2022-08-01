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

import java.util.Arrays;

import org.eclipse.rdf4j.sail.lucene.DocumentScore;
import org.eclipse.rdf4j.sail.lucene.SearchFields;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.locationtech.spatial4j.context.SpatialContext;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

public class ElasticsearchDocumentScore extends ElasticsearchDocumentResult implements DocumentScore {

	public ElasticsearchDocumentScore(SearchHit hit,
			Function<? super String, ? extends SpatialContext> geoContextMapper) {
		super(hit, geoContextMapper);
	}

	@Override
	public float getScore() {
		return hit.getScore();
	}

	@Override
	public boolean isHighlighted() {
		return (hit.getHighlightFields() != null);
	}

	@Override
	public Iterable<String> getSnippets(String property) {
		HighlightField highlightField = hit.getHighlightFields().get(ElasticsearchIndex.toPropertyFieldName(property));
		if (highlightField == null) {
			return null;
		}
		return Iterables.transform(Arrays.asList(highlightField.getFragments()),
				(Text fragment) -> SearchFields.getSnippet(fragment.string()));
	}
}
