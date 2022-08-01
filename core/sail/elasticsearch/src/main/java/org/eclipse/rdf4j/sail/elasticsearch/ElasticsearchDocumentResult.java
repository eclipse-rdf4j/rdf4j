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

import org.eclipse.rdf4j.sail.lucene.DocumentResult;
import org.eclipse.rdf4j.sail.lucene.SearchDocument;
import org.elasticsearch.search.SearchHit;
import org.locationtech.spatial4j.context.SpatialContext;

import com.google.common.base.Function;

public class ElasticsearchDocumentResult implements DocumentResult {

	protected final SearchHit hit;

	private final Function<? super String, ? extends SpatialContext> geoContextMapper;

	private ElasticsearchDocument fullDoc;

	public ElasticsearchDocumentResult(SearchHit hit,
			Function<? super String, ? extends SpatialContext> geoContextMapper) {
		this.hit = hit;
		this.geoContextMapper = geoContextMapper;
	}

	@Override
	public SearchDocument getDocument() {
		if (fullDoc == null) {
			fullDoc = new ElasticsearchDocument(hit, geoContextMapper);
		}
		return fullDoc;
	}
}
