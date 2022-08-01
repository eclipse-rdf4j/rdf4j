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

import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

class ElasticsearchHelper {

	static CloseableIteration<SearchHit, RuntimeException> getScrollingIterator(QueryBuilder queryBuilder,
			Client client, String index, int scrollTimeout) {

		return new CloseableIteration<>() {

			Iterator<SearchHit> items;
			String scrollId;
			long itemsRetrieved = 0;
			final int size = 1000;

			{

				SearchResponse scrollResp = client.prepareSearch(index)
						.addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
						.setScroll(scrollTimeout + "ms")
						.setQuery(queryBuilder)
						.setSize(size)
						.get();

				items = Arrays.asList(scrollResp.getHits().getHits()).iterator();
				scrollId = scrollResp.getScrollId();

			}

			SearchHit next;
			boolean empty = false;

			private void calculateNext() {

				if (next != null) {
					return;
				}
				if (empty) {
					return;
				}

				if (items.hasNext()) {
					next = items.next();
				} else {
					if (itemsRetrieved < size - 2) {
						// the count of our prevous scroll was lower than requested size, so nothing more to get now.
						scrollIsEmpty();
					} else {
						SearchResponse scrollResp = client.prepareSearchScroll(scrollId)
								.setScroll(scrollTimeout + "ms")
								.execute()
								.actionGet();

						items = Arrays.asList(scrollResp.getHits().getHits()).iterator();
						scrollId = scrollResp.getScrollId();

						if (items.hasNext()) {
							next = items.next();
						} else {
							scrollIsEmpty();
						}

						itemsRetrieved = 0;
					}

				}

			}

			private void scrollIsEmpty() {
				ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
				clearScrollRequest.addScrollId(scrollId);
				client.clearScroll(clearScrollRequest).actionGet();
				scrollId = null;
				empty = true;
			}

			@Override
			public boolean hasNext() {
				calculateNext();
				return next != null;
			}

			@Override
			public SearchHit next() {
				calculateNext();

				SearchHit temp = next;
				next = null;

				itemsRetrieved++;
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
