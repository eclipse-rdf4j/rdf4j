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
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.NamespaceStoreInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.transport.endpoints.BooleanResponse;

/**
 * @Author Håvard Mikkelsen Ottestad
 */
class ElasticsearchNamespaceStore implements NamespaceStoreInterface {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchNamespaceStore.class);
	private static final String PREFIX = "prefix";
	private static final String NAMESPACE = "namespace";

	private final ClientProvider clientProvider;
	private final String index;

	private static final java.lang.reflect.Type MAP_TYPE = new TypeReference<Map<String, Object>>() {
	}.getType();
	private final ObjectMapper objectMapper = new ObjectMapper();

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

	private String typelessMapping(String mapping) {
		try {
			JsonNode root = objectMapper.readTree(mapping);
			if (root.size() == 1) {
				JsonNode typeNode = root.elements().next();
				JsonNode properties = typeNode.get("properties");
				if (properties != null) {
					ObjectNode wrapper = objectMapper.createObjectNode();
					ObjectNode mappingsNode = objectMapper.createObjectNode();
					mappingsNode.set("properties", properties);
					wrapper.set("mappings", mappingsNode);
					return objectMapper.writeValueAsString(wrapper);
				}
			}
			return mapping;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String getNamespace(String prefix) {
		try {
			GetResponse<Map<String, Object>> documentFields = clientProvider.getClient()
					.get(g -> g.index(index).id(prefix), MAP_TYPE);
			if (documentFields.found()) {
				return documentFields.source().get(NAMESPACE).toString();
			}

			return null;
		} catch (IOException e) {
			throw new SailException(e);
		}
	}

	@Override
	public void setNamespace(String prefix, String namespace) {

		Map<String, String> map = new HashMap<>();
		map.put(PREFIX, prefix);
		map.put(NAMESPACE, namespace);

		try {
			clientProvider.getClient().index(i -> i.index(index).id(prefix).document(map));
			clientProvider.getClient().indices().refresh(r -> r.index(index));
		} catch (IOException e) {
			throw new SailException(e);
		}
	}

	@Override
	public void removeNamespace(String prefix) {
		try {
			clientProvider.getClient().delete(d -> d.index(index).id(prefix));
			clientProvider.getClient().indices().refresh(r -> r.index(index));
		} catch (IOException e) {
			throw new SailException(e);
		}
	}

	@Override
	public void clear() {
		try {
			clientProvider.getClient().indices().delete(d -> d.index(index));
			init();
		} catch (IOException e) {
			throw new SailException(e);
		}
	}

	@Override
	public void init() {
		try {
			BooleanResponse existsResponse = clientProvider.getClient().indices().exists(b -> b.index(index));

			if (!existsResponse.value()) {
				String mappingJson = typelessMapping(MAPPING);
				clientProvider.getClient()
						.indices()
						.create(c -> c
								.index(index)
								.withJson(new StringReader(mappingJson)));
			}
		} catch (IOException e) {
			throw new SailException(e);
		}

	}

	@Override
	public Iterator<SimpleNamespace> iterator() {

		try {
			SearchResponse<Map<String, Object>> searchResponse = clientProvider.getClient()
					.search(s -> s
							.index(index)
							.sort(sort -> sort.field(f -> f.field("_doc").order(SortOrder.Asc)))
							.size(10000)
							.trackTotalHits(t -> t.enabled(true))
							.query(q -> q.matchAll(m -> m)), MAP_TYPE);

			if (searchResponse.hits().total() != null && searchResponse.hits().total().value() > 10000) {
				throw new SailException("Namespace store only supports 10 000 items, found "
						+ searchResponse.hits().total().value());
			}

			return StreamSupport.stream(searchResponse.hits().hits().spliterator(), false)
					.map(Hit::source)
					.map(map -> new SimpleNamespace(map.get(PREFIX).toString(), map.get(NAMESPACE).toString()))
					.iterator();
		} catch (IOException e) {
			throw new SailException(e);
		}

	}
}
