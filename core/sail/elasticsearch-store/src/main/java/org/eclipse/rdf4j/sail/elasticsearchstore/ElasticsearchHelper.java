/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

import com.fasterxml.jackson.core.type.TypeReference;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.ScrollResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

class ElasticsearchHelper {

	private static final Type MAP_TYPE = new TypeReference<Map<String, Object>>() {
	}.getType();

	static CloseableIteration<Hit<Map<String, Object>>> getScrollingIterator(Query query,
			ElasticsearchClient client, String index, int scrollTimeout) {

		return new CloseableIteration<>() {

			Iterator<Hit<Map<String, Object>>> items;
			String scrollId;
			int currentBatchSize;
			final int size = 1000;

			{
				try {
					SearchResponse<Map<String, Object>> scrollResp = client.search(s -> s
							.index(index)
							.sort(sort -> sort.field(f -> f.field("_doc").order(SortOrder.Asc)))
							.scroll(sc -> sc.time(scrollTimeout + "ms"))
							.size(size)
							.query(query), MAP_TYPE);

					List<Hit<Map<String, Object>>> hits = scrollResp.hits().hits();
					items = hits.iterator();
					scrollId = scrollResp.scrollId();
					currentBatchSize = hits.size();
				} catch (IOException e) {
					throw new SailException(e);
				}
			}

			Hit<Map<String, Object>> next;
			boolean empty = false;

			private void calculateNext() {

				if (next != null || empty) {
					return;
				}

				if (items.hasNext()) {
					next = items.next();
					return;
				}

				if (currentBatchSize < size) {
					scrollIsEmpty();
					return;
				}

				try {
					ScrollResponse<Map<String, Object>> scrollResp = client.scroll(sc -> sc
							.scrollId(scrollId)
							.scroll(t -> t.time(scrollTimeout + "ms")), MAP_TYPE);

					List<Hit<Map<String, Object>>> hits = scrollResp.hits().hits();
					items = hits.iterator();
					scrollId = scrollResp.scrollId();
					currentBatchSize = hits.size();
				} catch (IOException e) {
					throw new SailException(e);
				}

				if (items.hasNext()) {
					next = items.next();
				} else {
					scrollIsEmpty();
				}
			}

			private void scrollIsEmpty() {
				if (scrollId != null) {
					try {
						client.clearScroll(c -> c.scrollId(scrollId));
					} catch (IOException e) {
						throw new SailException(e);
					}
				}
				scrollId = null;
				empty = true;
			}

			@Override
			public boolean hasNext() {
				calculateNext();
				return next != null;
			}

			@Override
			public Hit<Map<String, Object>> next() {
				calculateNext();

				Hit<Map<String, Object>> temp = next;
				next = null;

				return temp;
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

			@Override
			public void close() {

				if (scrollId != null) {
					scrollIsEmpty();
				}

			}

		};
	}
}
