/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.sail.SailException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * @Author Håvard Mikkelsen Ottestad
 */
public class ElasticsearchNamespaceStore implements NamespaceStore {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchNamespaceStore.class);
	private static final String PREFIX = "prefix";
	private static final String NAMESPACE = "namespace";

	private final ClientPool clientPool;
	private final String index;

	private static final String ELASTICSEARCH_TYPE = "namespace";
	private static final String mapping;

	static {
		try {
			mapping = IOUtils.toString(ElasticsearchNamespaceStore.class.getClassLoader()
					.getResourceAsStream("elasticsearchStoreNamespaceMapping.json"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public ElasticsearchNamespaceStore(ClientPool clientPool, String index) {
		this.clientPool = clientPool;
		this.index = index;
	}

	@Override
	public String getNamespace(String prefix) {
		GetResponse documentFields = clientPool.getClient().prepareGet(index, ELASTICSEARCH_TYPE, prefix).get();
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

		clientPool.getClient().prepareIndex(index, ELASTICSEARCH_TYPE, prefix).setSource(map).get();
		clientPool.getClient().admin().indices().prepareRefresh(index).get();
	}

	@Override
	public void removeNamespace(String prefix) {
		clientPool.getClient().prepareDelete(index, ELASTICSEARCH_TYPE, prefix).get();
		clientPool.getClient().admin().indices().prepareRefresh(index).get();
	}

	@Override
	public void clear() {
		clientPool.getClient().admin().indices().prepareDelete(index).get();
		init();
	}

	@Override
	public void init() {

		CreateIndexRequest request = new CreateIndexRequest(index);

		request.mapping(ELASTICSEARCH_TYPE, mapping, XContentType.JSON);

		boolean indexExistsAlready = clientPool.getClient()
				.admin()
				.indices()
				.exists(new IndicesExistsRequest(index))
				.actionGet()
				.isExists();

		if (!indexExistsAlready) {
			clientPool.getClient().admin().indices().create(request).actionGet();
		}

	}

	@Override
	public Iterator<SimpleNamespace> iterator() {

		SearchResponse searchResponse = clientPool.getClient()
				.prepareSearch(index)
				.setQuery(QueryBuilders.constantScoreQuery(matchAllQuery()))
				.setSize(10000)
				.get();

		SearchHits hits = searchResponse.getHits();
		if (hits.totalHits > 10000) {
			throw new SailException("Namespace store only supports 10 000 items, found " + hits.totalHits);
		}

		return new Iterator<SimpleNamespace>() {
			Iterator<SearchHit> iterator = hits.iterator();

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public SimpleNamespace next() {
				Map<String, Object> sourceAsMap = iterator.next().getSourceAsMap();

				return new SimpleNamespace(sourceAsMap.get(PREFIX).toString(), sourceAsMap.get(NAMESPACE).toString());
			}
		};

	}
}
