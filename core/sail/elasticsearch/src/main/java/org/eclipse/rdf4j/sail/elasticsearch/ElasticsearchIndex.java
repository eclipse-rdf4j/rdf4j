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
import java.io.StringReader;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.http.HttpHost;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.lucene.AbstractSearchIndex;
import org.eclipse.rdf4j.sail.lucene.BulkUpdater;
import org.eclipse.rdf4j.sail.lucene.DocumentDistance;
import org.eclipse.rdf4j.sail.lucene.DocumentResult;
import org.eclipse.rdf4j.sail.lucene.DocumentScore;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.QuerySpec;
import org.eclipse.rdf4j.sail.lucene.SearchDocument;
import org.eclipse.rdf4j.sail.lucene.SearchFields;
import org.apache.http.HttpHost;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch._types.WaitForActiveShards;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.RefreshRequest;
import co.elastic.clients.elasticsearch.indices.RefreshResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.geometry.Geometry;
import org.elasticsearch.geometry.utils.GeometryValidator;
import org.elasticsearch.geometry.utils.StandardValidator;
import org.elasticsearch.geometry.utils.WellKnownText;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoShapeQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.seqno.SequenceNumbers;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.geometry.Geometry;
import org.elasticsearch.geometry.utils.GeometryValidator;
import org.elasticsearch.geometry.utils.StandardValidator;
import org.elasticsearch.geometry.utils.WellKnownText;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoShapeQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.seqno.SequenceNumbers;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.xcontent.XContentType;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.io.GeohashUtils;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Iterables;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch._types.WaitForActiveShards;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.RefreshRequest;
import co.elastic.clients.elasticsearch.indices.RefreshResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;

/**
 * Requires an Elasticsearch cluster with the DeleteByQuery plugin.
 * <p>
 * Note that, while RDF4J is licensed under the EDL, several ElasticSearch dependencies are licensed under the Elastic
 * license or the SSPL, which may have implications for some projects.
 * <p>
 * Please consult the ElasticSearch website and license FAQ for more information.
 *
 * @see <a href="https://www.elastic.co/licensing/elastic-license/faq">Elastic License FAQ</a>
 * @see LuceneSail
 */
public class ElasticsearchIndex extends AbstractSearchIndex {

	/**
	 * Set the parameter "indexName=" to specify the index to use.
	 */
	public static final String INDEX_NAME_KEY = "indexName";

	/**
	 * Set the parameter "documentType=" to specify the document type to use. By default, the document type is
	 * "resource".
	 */
	public static final String DOCUMENT_TYPE_KEY = "documentType";

	/**
	 * Set the parameter "transport=" to specify the address of the cluster to use (e.g. localhost:9300).
	 */
	public static final String TRANSPORT_KEY = "transport";

	/**
	 * Set the parameter "waitForStatus=" to configure if {@link #initialize(java.util.Properties) initialization}
	 * should wait for a particular health status. The value can be one of "green" or "yellow". Does not wait by
	 * default.
	 */
	public static final String WAIT_FOR_STATUS_KEY = "waitForStatus";

	/**
	 * Set the parameter "waitForNodes=" to configure if {@link #initialize(java.util.Properties) initialization} should
	 * wait until the specified number of nodes are available. Does not wait by default.
	 */
	public static final String WAIT_FOR_NODES_KEY = "waitForNodes";

	/**
	 * Set the parameter "waitForActiveShards=" to configure if {@link #initialize(java.util.Properties) initialization}
	 * should wait until the specified number of shards to be active. Does not wait by default.
	 */
	public static final String WAIT_FOR_ACTIVE_SHARDS_KEY = "waitForActiveShards";

	/**
	 * Set the parameter "waitForRelocatingShards=" to configure if {@link #initialize(java.util.Properties)
	 * initialization} should wait until the specified number of nodes are relocating. Does not wait by default.
	 *
	 * @deprecated use {@link #WAIT_FOR_NO_RELOCATING_SHARDS_KEY} in elastic search >= 5.x
	 */
	@Deprecated
	public static final String WAIT_FOR_RELOCATING_SHARDS_KEY = "waitForRelocatingShards";

	/**
	 * Set the parameter "waitForNoRelocatingShards=true|false" to configure if {@link #initialize(java.util.Properties)
	 * initialization} should wait until the are no relocating shards. Defaults to false, meaning the operation does not
	 * wait on there being no more relocating shards. Set to true to wait until the number of relocating shards in the
	 * cluster is 0.
	 */
	public static final String WAIT_FOR_NO_RELOCATING_SHARDS_KEY = "waitForNoRelocatingShards";

	public static final String DEFAULT_INDEX_NAME = "elastic-search-sail";

	public static final String DEFAULT_DOCUMENT_TYPE = "resource";

	public static final String DEFAULT_TRANSPORT = "localhost";

	public static final String DEFAULT_ANALYZER = "standard";

	public static final String ELASTICSEARCH_KEY_PREFIX = "elasticsearch.";

	public static final String PROPERTY_FIELD_PREFIX = "p_";

	public static final String ALL_PROPERTY_FIELDS = "p_*";

	public static final String GEOPOINT_FIELD_PREFIX = "_geopoint_";

	public static final String GEOSHAPE_FIELD_PREFIX = "_geoshape_";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private static final Type MAP_TYPE = new TypeReference<Map<String, Object>>() {
	}.getType();

	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

	private static final Set<String> UNSUPPORTED_QUERY_FIELDS = Set.of("adjust_pure_negative", "ignore_unmapped");

	private static final Set<String> LOWERCASE_ENUM_FIELDS = Set.of("validation_method", "multi_value_mode");

	private static final GeometryValidator GEOMETRY_VALIDATOR = StandardValidator.instance(true);

	private volatile RestClient lowLevelClient;

	private volatile ElasticsearchTransport transport;

	private volatile ElasticsearchClient client;

	private String clusterName;

	private String indexName;

	private String documentType;

	private String analyzer;

	private String queryAnalyzer = DEFAULT_ANALYZER;

	private Function<? super String, ? extends SpatialContext> geoContextMapper;

	public ElasticsearchIndex() {
	}

	public String getClusterName() {
		return clusterName;
	}

	public String getIndexName() {
		return indexName;
	}

	public String[] getTypes() {
		return new String[] { documentType };
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Properties parameters) throws Exception {
		super.initialize(parameters);
		indexName = parameters.getProperty(INDEX_NAME_KEY, DEFAULT_INDEX_NAME);
		documentType = parameters.getProperty(DOCUMENT_TYPE_KEY, DEFAULT_DOCUMENT_TYPE);
		analyzer = parameters.getProperty(LuceneSail.ANALYZER_CLASS_KEY, DEFAULT_ANALYZER);
		queryAnalyzer = parameters.getProperty(LuceneSail.QUERY_ANALYZER_CLASS_KEY, DEFAULT_ANALYZER);
		// slightly hacky cast to cope with the fact that Properties is
		// Map<Object,Object>
		// even though it is effectively Map<String,String>
		geoContextMapper = createSpatialContextMapper((Map<String, String>) (Map<?, ?>) parameters);

		String transportHosts = parameters.getProperty(TRANSPORT_KEY, DEFAULT_TRANSPORT);
		String[] hostSpecs = transportHosts.split(",");
		HttpHost[] httpHosts = Arrays.stream(hostSpecs).map(spec -> {
			String[] hostPort = spec.split(":");
			String host = hostPort[0];
			int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 9200;
			return new HttpHost(host, port, "http");
		}).toArray(HttpHost[]::new);

		lowLevelClient = RestClient.builder(httpHosts).build();
		transport = new RestClientTransport(lowLevelClient, new JacksonJsonpMapper());
		client = new ElasticsearchClient(transport);

		clusterName = parameters.getProperty(ELASTICSEARCH_KEY_PREFIX + "cluster.name");

		BooleanResponse existsResponse = client.indices().exists(ExistsRequest.of(b -> b.index(indexName)));
		if (!existsResponse.value()) {
			createIndex();
		}

		logger.info("Field mappings:\n{}", getMappings());

		String waitForStatus = parameters.getProperty(WAIT_FOR_STATUS_KEY);
		String waitForNodes = parameters.getProperty(WAIT_FOR_NODES_KEY);
		String waitForActiveShards = parameters.getProperty(WAIT_FOR_ACTIVE_SHARDS_KEY);
		String waitForRelocatingShards = parameters.getProperty(WAIT_FOR_RELOCATING_SHARDS_KEY);
		if (waitForRelocatingShards != null) {
			logger.warn("Property " + WAIT_FOR_RELOCATING_SHARDS_KEY + " no longer supported. Use "
					+ WAIT_FOR_NO_RELOCATING_SHARDS_KEY + " instead");
		}
		String waitForNoRelocatingShards = parameters.getProperty(WAIT_FOR_NO_RELOCATING_SHARDS_KEY);

		client.cluster().health(h -> {
			h.index(indexName);
			if ("green".equals(waitForStatus)) {
				h.waitForStatus(HealthStatus.Green);
			} else if ("yellow".equals(waitForStatus)) {
				h.waitForStatus(HealthStatus.Yellow);
			}
			if (waitForNodes != null) {
				h.waitForNodes(waitForNodes);
			}
			if (waitForActiveShards != null) {
				h.waitForActiveShards(
						WaitForActiveShards.of(w -> w.count(Integer.parseInt(waitForActiveShards))));
			}
			if (waitForNoRelocatingShards != null) {
				h.waitForNoRelocatingShards(Boolean.parseBoolean(waitForNoRelocatingShards));
			}
			return h;
		});
	}

	protected Function<? super String, ? extends SpatialContext> createSpatialContextMapper(
			Map<String, String> parameters) {
		// this should really be based on the schema
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		SpatialContext geoContext = SpatialContextFactory.makeSpatialContext(parameters, classLoader);
		return Functions.constant(geoContext);
	}

	public Map<String, Object> getMappings() throws IOException {
		GetMappingResponse resp = client.indices().getMapping(g -> g.index(indexName));
		if (resp.result() == null || resp.result().get(indexName) == null) {
			return Map.of();
		}
		TypeMapping mapping = resp.result().get(indexName).mappings();
		if (mapping == null || mapping.meta() == null) {
			return Map.of();
		}
		Map<String, Object> meta = new HashMap<>();
		mapping.meta().forEach((k, v) -> meta.put(k, v == null ? null : v.toString()));
		return meta;
	}

	private void createIndex() throws IOException {
		try (XContentBuilder xContentBuilder = XContentFactory.jsonBuilder()
				.startObject()
				.field("index.query.default_field", SearchFields.TEXT_FIELD_NAME)
				.startObject("analysis")
				.startObject("analyzer")
				.startObject("default")
				.field("type", analyzer)
				.endObject()
				.endObject()
				.endObject()
				.endObject()) {

			CreateIndexResponse createResponse = client.indices()
					.create(c -> c.index(indexName)
							.settings(s -> s.withJson(new StringReader(Strings.toString(xContentBuilder)))));
			if (!createResponse.acknowledged()) {
				throw new IOException("Failed to create index " + indexName);
			}
		}

		// use _source instead of explicit stored = true
		try (XContentBuilder typeMapping = XContentFactory.jsonBuilder()) {
			typeMapping.startObject().startObject("properties");
			typeMapping.startObject(SearchFields.CONTEXT_FIELD_NAME)
					.field("type", "keyword")
					.field("index", true)
					.field("copy_to", "_all")
					.endObject();
			typeMapping.startObject(SearchFields.URI_FIELD_NAME)
					.field("type", "keyword")
					.field("index", true)
					.field("copy_to", "_all")
					.endObject();
			typeMapping.startObject(SearchFields.TEXT_FIELD_NAME)
					.field("type", "text")
					.field("index", true)
					.field("copy_to", "_all")
					.endObject();
			for (String wktField : wktFields) {
				typeMapping.startObject(toGeoPointFieldName(wktField)).field("type", "geo_point").endObject();
				if (supportsShapes(wktField)) {
					typeMapping.startObject(toGeoShapeFieldName(wktField))
							.field("type", "geo_shape")
							.field("copy_to", "_all")
							.endObject();
				}
			}
			typeMapping.endObject().endObject();

			client.indices()
					.putMapping(
							PutMappingRequest.of(pm -> pm.index(indexName)
									.withJson(new StringReader(Strings.toString(typeMapping)))));
			client.indices().refresh(RefreshRequest.of(r -> r.index(indexName)));
		}
	}

	private boolean supportsShapes(String field) {
		SpatialContext geoContext = geoContextMapper.apply(field);
		try {
			geoContext.readShapeFromWkt("POLYGON ((0 0, 1 0, 1 1, 0 1, 0 0))");
			return true;
		} catch (ParseException e) {
			return false;
		}
	}

	@Override
	protected SpatialContext getSpatialContext(String property) {
		return geoContextMapper.apply(property);
	}

	@Override
	public void shutDown() throws IOException {
		RestClient toCloseClient = lowLevelClient;
		lowLevelClient = null;
		transport = null;
		client = null;
		if (toCloseClient != null) {
			toCloseClient.close();
		}
	}

	// //////////////////////////////// Methods for updating the index

	/**
	 * Returns a Document representing the specified document ID (combination of resource and context), or null when no
	 * such Document exists yet.
	 */
	@Override
	protected SearchDocument getDocument(String id) throws IOException {
		GetResponse<Map<String, Object>> response = client.get(
				GetRequest.of(g -> g.index(indexName).id(id)),
				MAP_TYPE);
		if (response.found()) {
			long seqNo = response.seqNo() == null ? SequenceNumbers.UNASSIGNED_SEQ_NO : response.seqNo();
			long primaryTerm = response.primaryTerm() == null ? SequenceNumbers.UNASSIGNED_PRIMARY_TERM
					: response.primaryTerm();
			return new ElasticsearchDocument(response.id(), documentType, response.index(), seqNo, primaryTerm,
					response.source(), geoContextMapper);
		}
		// no such Document
		return null;
	}

	@Override
	protected Iterable<? extends SearchDocument> getDocuments(String resourceId) throws IOException {
		Iterable<Hit<Map<String, Object>>> hits = getDocuments(QueryBuilders.termQuery(SearchFields.URI_FIELD_NAME,
				resourceId));
		return Iterables.transform(hits,
				(Function<Hit<Map<String, Object>>, SearchDocument>) hit -> new ElasticsearchDocument(hit,
						geoContextMapper));
	}

	@Override
	protected SearchDocument newDocument(String id, String resourceId, String context) {
		return new ElasticsearchDocument(id, documentType, indexName, resourceId, context, geoContextMapper);
	}

	@Override
	protected SearchDocument copyDocument(SearchDocument doc) {
		ElasticsearchDocument esDoc = (ElasticsearchDocument) doc;
		Map<String, Object> source = esDoc.getSource();
		Map<String, Object> newDocument = new HashMap<>(source);
		return new ElasticsearchDocument(esDoc.getId(), esDoc.getType(), esDoc.getIndex(), esDoc.getSeqNo(),
				esDoc.getPrimaryTerm(), newDocument, geoContextMapper);
	}

	@Override
	protected void addDocument(SearchDocument doc) throws IOException {
		ElasticsearchDocument esDoc = (ElasticsearchDocument) doc;
		IndexResponse response = client.index(
				IndexRequest.of(i -> i.index(esDoc.getIndex()).id(esDoc.getId()).document(esDoc.getSource())));
		if (response.result() == null) {
			throw new IOException("Index request failed for " + esDoc.getId());
		}
	}

	@Override
	protected void updateDocument(SearchDocument doc) throws IOException {
		ElasticsearchDocument esDoc = (ElasticsearchDocument) doc;
		IndexRequest.Builder<Map<String, Object>> request = new IndexRequest.Builder<>();
		request.index(esDoc.getIndex()).id(esDoc.getId()).document(esDoc.getSource());
		if (esDoc.getSeqNo() != SequenceNumbers.UNASSIGNED_SEQ_NO
				&& esDoc.getPrimaryTerm() != SequenceNumbers.UNASSIGNED_PRIMARY_TERM) {
			request.ifSeqNo(esDoc.getSeqNo()).ifPrimaryTerm(esDoc.getPrimaryTerm());
		}
		IndexResponse response = client.index(request.build());
		if (response.result() == null) {
			throw new IOException("Update request failed for " + esDoc.getId());
		}
	}

	@Override
	protected void deleteDocument(SearchDocument doc) throws IOException {
		ElasticsearchDocument esDoc = (ElasticsearchDocument) doc;
		client.delete(DeleteRequest.of(d -> {
			d.index(esDoc.getIndex()).id(esDoc.getId());
			if (esDoc.getSeqNo() != SequenceNumbers.UNASSIGNED_SEQ_NO
					&& esDoc.getPrimaryTerm() != SequenceNumbers.UNASSIGNED_PRIMARY_TERM) {
				d.ifSeqNo(esDoc.getSeqNo()).ifPrimaryTerm(esDoc.getPrimaryTerm());
			}
			return d;
		}));
	}

	@Override
	protected BulkUpdater newBulkUpdate() {
		return new ElasticsearchBulkUpdater(client);
	}

	/**
	 * Returns a list of Documents representing the specified Resource (empty when no such Document exists yet). Each
	 * document represent a set of statements with the specified Resource as a subject, which are stored in a specific
	 * context
	 */
	private Iterable<Hit<Map<String, Object>>> getDocuments(QueryBuilder query) throws IOException {
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(query).trackTotalHits(true);
		return search(sourceBuilder).hits().hits();
	}

	/**
	 * Returns a Document representing the specified Resource and Context combination, or null when no such Document
	 * exists yet.
	 *
	 * @param subject
	 * @param context
	 * @return search document
	 * @throws IOException
	 */
	public SearchDocument getDocument(Resource subject, Resource context) throws IOException {
		// fetch the Document representing this Resource
		String resourceId = SearchFields.getResourceID(subject);
		String contextId = SearchFields.getContextID(context);
		return getDocument(SearchFields.formIdString(resourceId, contextId));
	}

	/**
	 * Returns a list of Documents representing the specified Resource (empty when no such Document exists yet).Each
	 * document represent a set of statements with the specified Resource as a subject, which are stored in a specific
	 * context
	 *
	 * @param subject
	 * @return list of documents
	 * @throws IOException
	 */
	public Iterable<? extends SearchDocument> getDocuments(Resource subject) throws IOException {
		String resourceId = SearchFields.getResourceID(subject);
		return getDocuments(resourceId);
	}

	/**
	 * Filters the given list of fields, retaining all property fields.
	 *
	 * @param fields
	 * @return set of fields
	 */
	public static Set<String> getPropertyFields(Set<String> fields) {
		Set<String> result = new HashSet<>(fields.size());
		for (String field : fields) {
			if (SearchFields.isPropertyField(field)) {
				result.add(field);
			}
		}
		return result;
	}

	@Override
	public void begin() throws IOException {
	}

	@Override
	public void commit() throws IOException {
		client.indices().refresh(RefreshRequest.of(r -> r.index(indexName)));
	}

	@Override
	public void rollback() throws IOException {
	}

	// //////////////////////////////// Methods for querying the index

	/**
	 * Parse the passed query.
	 *
	 * @param subject
	 * @param spec    query to process
	 * @return the parsed query
	 * @throws MalformedQueryException
	 * @throws IOException
	 * @throws IllegalArgumentException if the spec contains a multi-param query
	 */
	@Override
	protected Iterable<? extends DocumentScore> query(Resource subject, QuerySpec spec)
			throws MalformedQueryException, IOException {
		if (spec.getQueryPatterns().size() != 1) {
			throw new IllegalArgumentException("Multi-param query not implemented!");
		}
		QuerySpec.QueryParam param = spec.getQueryPatterns().iterator().next();
		IRI propertyURI = param.getProperty();
		boolean highlight = param.isHighlight();
		String query = param.getQuery();
		QueryBuilder qb = prepareQuery(propertyURI, QueryBuilders.queryStringQuery(query));
		SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().query(qb)
				.trackTotalHits(true)
				.seqNoAndPrimaryTerm(true);
		if (highlight) {
			HighlightBuilder hb = new HighlightBuilder();
			String field;
			if (propertyURI != null) {
				field = toPropertyFieldName(SearchFields.getPropertyField(propertyURI));
			} else {
				field = ALL_PROPERTY_FIELDS;
				hb.requireFieldMatch(false);
			}
			hb.field(field);
			hb.preTags(SearchFields.HIGHLIGHTER_PRE_TAG);
			hb.postTags(SearchFields.HIGHLIGHTER_POST_TAG);
			// Elastic Search doesn't really have the same support for fragments as
			// Lucene.
			// So, we have to get back the whole highlighted value (comma-separated
			// if it is a list)
			// and then post-process it into fragments ourselves.
			hb.numOfFragments(0);
			sourceBuilder.highlighter(hb);
		}

		int numDocs;

		Integer specNumDocs = spec.getNumDocs();
		if (specNumDocs != null) {
			if (specNumDocs < 0) {
				throw new IllegalArgumentException("numDocs must be >= 0");
			}
			numDocs = specNumDocs;
		} else {
			numDocs = -1;
		}

		QueryBuilder combinedQuery = qb;
		if (subject != null) {
			QueryBuilder idQuery = QueryBuilders.termQuery(SearchFields.URI_FIELD_NAME,
					SearchFields.getResourceID(subject));
			combinedQuery = QueryBuilders.boolQuery().must(idQuery).must(qb);
		}

		SearchResponse<Map<String, Object>> response = executeSearch(combinedQuery, numDocs, sourceBuilder);
		return Iterables.transform(response.hits().hits(),
				(Function<Hit<Map<String, Object>>, DocumentScore>) hit -> new ElasticsearchDocumentScore(hit,
						geoContextMapper));
	}

	@Override
	protected Iterable<? extends DocumentDistance> geoQuery(final IRI geoProperty, Point p, final IRI units,
			double distance, String distanceVar, Var contextVar) throws MalformedQueryException, IOException {
		double unitDist;
		final DistanceUnit unit;
		if (GEOF.UOM_METRE.equals(units)) {
			unit = DistanceUnit.METERS;
			unitDist = distance;
		} else if (GEOF.UOM_DEGREE.equals(units)) {
			unit = DistanceUnit.KILOMETERS;
			unitDist = unit.getDistancePerDegree() * distance;
		} else if (GEOF.UOM_RADIAN.equals(units)) {
			unit = DistanceUnit.KILOMETERS;
			unitDist = DistanceUtils.radians2Dist(distance, DistanceUtils.EARTH_MEAN_RADIUS_KM);
		} else if (GEOF.UOM_UNITY.equals(units)) {
			unit = DistanceUnit.KILOMETERS;
			unitDist = distance * Math.PI * DistanceUtils.EARTH_MEAN_RADIUS_KM;
		} else {
			throw new MalformedQueryException("Unsupported units: " + units);
		}

		double lat = p.getY();
		double lon = p.getX();
		final String fieldName = toGeoPointFieldName(SearchFields.getPropertyField(geoProperty));
		QueryBuilder qb = QueryBuilders.functionScoreQuery(
				QueryBuilders.geoDistanceQuery(fieldName).point(lat, lon).distance(unitDist, unit),
				ScoreFunctionBuilders.linearDecayFunction(fieldName, GeohashUtils.encodeLatLon(lat, lon),
						new DistanceUnit.Distance(unitDist, unit).toString()));
		if (contextVar != null) {
			qb = addContextTerm(qb, (Resource) contextVar.getValue());
		}

		SearchResponse<Map<String, Object>> response = executeSearch(qb, -1);
		final GeoPoint srcPoint = new GeoPoint(lat, lon);
		return Iterables.transform(response.hits().hits(),
				(Function<Hit<Map<String, Object>>, DocumentDistance>) hit -> new ElasticsearchDocumentDistance(hit,
						geoContextMapper, fieldName, units, srcPoint, unit));
	}

	private QueryBuilder addContextTerm(QueryBuilder qb, Resource ctx) {
		BoolQueryBuilder combinedQuery = QueryBuilders.boolQuery();
		QueryBuilder idQuery = QueryBuilders.termQuery(SearchFields.CONTEXT_FIELD_NAME, SearchFields.getContextID(ctx));
		if (ctx != null) {
			// the specified named graph
			combinedQuery.must(idQuery);
		} else {
			// not the unnamed graph
			combinedQuery.mustNot(idQuery);
		}
		combinedQuery.must(qb);
		return combinedQuery;
	}

	private Query toQuery(QueryBuilder qb) {
		return Query.of(q -> q.withJson(new StringReader(sanitizeQueryJson(qb.toString()))));
	}

	@Override
	protected Iterable<? extends DocumentResult> geoRelationQuery(String relation, IRI geoProperty, String wkt,
			Var contextVar) throws MalformedQueryException, IOException {

		Shape shape = null;
		try {
			shape = super.parseQueryShape(SearchFields.getPropertyField(geoProperty), wkt);
		} catch (ParseException e) {
			logger.error("error while parsing wkt geometry", e);
		}
		ShapeRelation spatialOp = toSpatialOp(relation);
		if (spatialOp == null) {
			return null;
		}
		final String fieldName = toGeoShapeFieldName(SearchFields.getPropertyField(geoProperty));
		Geometry geometry;
		try {
			geometry = WellKnownText.fromWKT(GEOMETRY_VALIDATOR, true, wkt);
		} catch (ParseException e) {
			throw new MalformedQueryException("error while parsing wkt geometry", e);
		}
		GeoShapeQueryBuilder fb = QueryBuilders.geoShapeQuery(fieldName, geometry);
		fb.relation(spatialOp);
		QueryBuilder qb = QueryBuilders.matchAllQuery();
		if (contextVar != null) {
			qb = addContextTerm(qb, (Resource) contextVar.getValue());
		}

		SearchResponse<Map<String, Object>> response = executeSearch(
				QueryBuilders.boolQuery().must(qb).filter(fb), -1);
		return Iterables.transform(response.hits().hits(),
				(Function<Hit<Map<String, Object>>, DocumentResult>) hit -> new ElasticsearchDocumentResult(hit,
						geoContextMapper));
	}

	private ShapeRelation toSpatialOp(String relation) {
		if (GEOF.SF_INTERSECTS.stringValue().equals(relation)) {
			return ShapeRelation.INTERSECTS;
		}
		if (GEOF.SF_DISJOINT.stringValue().equals(relation)) {
			return ShapeRelation.DISJOINT;
		}
		if (GEOF.EH_COVERED_BY.stringValue().equals(relation)) {
			return ShapeRelation.WITHIN;
		}
		return null;
	}

	public SearchResponse<Map<String, Object>> search(QueryBuilder query) throws IOException {
		return executeSearch(query, -1);
	}

	private SearchResponse<Map<String, Object>> executeSearch(QueryBuilder query, int numDocs) throws IOException {
		return executeSearch(query, numDocs, null);
	}

	private SearchResponse<Map<String, Object>> executeSearch(QueryBuilder query, int numDocs,
			SearchSourceBuilder template) throws IOException {
		int size = resolveSize(query, numDocs);
		SearchSourceBuilder source = template != null ? template : new SearchSourceBuilder();
		source.query(query);
		source.trackTotalHits(true);
		source.seqNoAndPrimaryTerm(true);
		source.size(size);
		return search(source);
	}

	private int resolveSize(QueryBuilder query, int numDocs) throws IOException {
		if (numDocs < -1) {
			throw new IllegalArgumentException("numDocs should be 0 or greater if defined by the user");
		}
		if (numDocs >= 0) {
			return Math.min(maxDocs, numDocs);
		}
		if (defaultNumDocs >= 0) {
			return Math.min(maxDocs, defaultNumDocs);
		}
		SearchSourceBuilder countSource = new SearchSourceBuilder().size(0).query(query).trackTotalHits(true);
		SearchResponse<Map<String, Object>> countResponse = search(countSource);
		long docCount = countResponse.hits().total() != null ? countResponse.hits().total().value() : 0;
		return Math.max((int) Math.min(docCount, maxDocs), 1);
	}

	private SearchResponse<Map<String, Object>> search(SearchSourceBuilder source) throws IOException {
		return client.search(
				s -> s.index(indexName)
						.seqNoPrimaryTerm(true)
						.withJson(new StringReader(sanitizeQueryJson(source.toString()))),
				MAP_TYPE);
	}

	private QueryStringQueryBuilder prepareQuery(IRI propertyURI, QueryStringQueryBuilder query) {
		// check out which query parser to use, based on the given property URI
		if (propertyURI == null)
		// if we have no property given, we create a default query parser which
		// has the TEXT_FIELD_NAME as the default field
		{
			query.defaultField(SearchFields.TEXT_FIELD_NAME).analyzer(queryAnalyzer);
		} else
		// otherwise we create a query parser that has the given property as
		// the default field
		{
			query.defaultField(toPropertyFieldName(SearchFields.getPropertyField(propertyURI))).analyzer(queryAnalyzer);
		}
		return query;
	}

	/**
	 * @param contexts
	 * @throws IOException
	 */
	@Override
	public synchronized void clearContexts(Resource... contexts) throws IOException {
		logger.debug("deleting contexts: {}", Arrays.toString(contexts));
		// these resources have to be read from the underlying rdf store
		// and their triples have to be added to the luceneindex after deletion of
		// documents

		// remove all contexts passed
		for (Resource context : contexts) {
			// attention: context can be NULL!
			String contextString = SearchFields.getContextID(context);
			// now delete all documents from the deleted context
			client.deleteByQuery(dbq -> dbq.index(indexName)
					.query(toQuery(QueryBuilders.termQuery(SearchFields.CONTEXT_FIELD_NAME, contextString))));
		}
	}

	/**
	 *
	 */
	@Override
	public synchronized void clear() throws IOException {
		client.indices().delete(d -> d.index(indexName));
		createIndex();
	}

	static String toPropertyFieldName(String prop) {
		return PROPERTY_FIELD_PREFIX + encodeFieldName(prop);
	}

	static String toPropertyName(String field) {
		return decodeFieldName(field.substring(PROPERTY_FIELD_PREFIX.length()));
	}

	static String toGeoPointFieldName(String prop) {
		return GEOPOINT_FIELD_PREFIX + encodeFieldName(prop);
	}

	static String toGeoShapeFieldName(String prop) {
		return GEOSHAPE_FIELD_PREFIX + encodeFieldName(prop);
	}

	static String encodeFieldName(String s) {
		return s.replace('.', '^');
	}

	static String decodeFieldName(String s) {
		return s.replace('^', '.');
	}

	private static String sanitizeQueryJson(String json) {
		try {
			JsonNode node = JSON_MAPPER.readTree(json);
			sanitizeNode(node);
			return JSON_MAPPER.writeValueAsString(node);
		} catch (IOException e) {
			return json;
		}
	}

	private static void sanitizeNode(JsonNode node) {
		if (node == null) {
			return;
		}
		if (node.isObject()) {
			ObjectNode obj = (ObjectNode) node;
			for (String field : UNSUPPORTED_QUERY_FIELDS) {
				obj.remove(field);
			}
			obj.fields().forEachRemaining(entry -> {
				String fieldName = entry.getKey();
				JsonNode child = entry.getValue();
				if (LOWERCASE_ENUM_FIELDS.contains(fieldName) && child.isTextual()) {
					obj.put(fieldName, child.asText().toLowerCase(Locale.ROOT));
				} else {
					sanitizeNode(child);
				}
			});
		} else if (node.isArray()) {
			for (JsonNode child : node) {
				sanitizeNode(child);
			}
		}
	}
}
