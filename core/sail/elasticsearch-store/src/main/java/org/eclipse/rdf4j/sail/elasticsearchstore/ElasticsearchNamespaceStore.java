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

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.NamespaceStoreInterface;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author Håvard Mikkelsen Ottestad
 */
class ElasticsearchNamespaceStore implements NamespaceStoreInterface {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchNamespaceStore.class);
	private static final String PREFIX = "prefix";
	private static final String NAMESPACE = "namespace";

	private final ClientProvider clientProvider;
	private final String index;

	private static final String ELASTICSEARCH_TYPE = "namespace";
	private static final String MAPPING;

	static {
		try {
			MAPPING = IOUtils.toString(ElasticsearchNamespaceStore.class.getClassLoader()
					.getResourceAsStream("elasticsearchStoreNamespaceMapping.json"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	ElasticsearchNamespaceStore(ClientProvider clientProvider, String index) {
		this.clientProvider = clientProvider;
		this.index = index;
	}

	@Override
	public String getNamespace(String prefix) {
		GetResponse documentFields = clientProvider.getClient().prepareGet(index, ELASTICSEARCH_TYPE, prefix).get();
		if (documentFields.isExists()) {
			return documentFields.getSource().get(NAMESPACE).toString();
		}

		return null;
	}

	@Override
	public void setNamespace(String prefix, String namespace) {

		Map<String, String> map = new HashMap<>();
		map.put(PREFIX, prefix);
		map.put(NAMESPACE, namespace);

		clientProvider.getClient().prepareIndex(index, ELASTICSEARCH_TYPE, prefix).setSource(map).get();
		clientProvider.getClient().admin().indices().prepareRefresh(index).get();
	}

	@Override
	public void removeNamespace(String prefix) {
		clientProvider.getClient().prepareDelete(index, ELASTICSEARCH_TYPE, prefix).get();
		clientProvider.getClient().admin().indices().prepareRefresh(index).get();
	}

	@Override
	public void clear() {
		clientProvider.getClient().admin().indices().prepareDelete(index).get();
		init();
	}

	@Override
	public void init() {
		boolean indexExistsAlready = clientProvider.getClient()
				.admin()
				.indices()
				.exists(new IndicesExistsRequest(index))
				.actionGet()
				.isExists();

		if (!indexExistsAlready) {
			CreateIndexRequest request = new CreateIndexRequest(index);
			request.mapping(ELASTICSEARCH_TYPE, MAPPING, XContentType.JSON);
			clientProvider.getClient().admin().indices().create(request).actionGet();
		}

	}

	@Override
	public Iterator<SimpleNamespace> iterator() {

		SearchResponse searchResponse = clientProvider.getClient()
				.prepareSearch(index)
				.addSort(FieldSortBuilder.DOC_FIELD_NAME, SortOrder.ASC)
				.setQuery(QueryBuilders.constantScoreQuery(matchAllQuery()))
				.setTrackTotalHits(true)
				.setSize(10000)
				.get();

		SearchHits hits = searchResponse.getHits();
		if (hits.getTotalHits().value > 10000) {
			throw new SailException("Namespace store only supports 10 000 items, found " + hits.getTotalHits().value);
		}

		return StreamSupport.stream(hits.spliterator(), false)
				.map(SearchHit::getSourceAsMap)
				.map(map -> new SimpleNamespace(map.get(PREFIX).toString(), map.get(NAMESPACE).toString()))
				.iterator();

	}
}
