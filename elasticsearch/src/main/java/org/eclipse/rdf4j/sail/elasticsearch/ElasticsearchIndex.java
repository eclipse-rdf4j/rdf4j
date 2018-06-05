/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearch;

import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.GEOF;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lucene.AbstractSearchIndex;
import org.eclipse.rdf4j.sail.lucene.BulkUpdater;
import org.eclipse.rdf4j.sail.lucene.DocumentDistance;
import org.eclipse.rdf4j.sail.lucene.DocumentResult;
import org.eclipse.rdf4j.sail.lucene.DocumentScore;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene.SearchDocument;
import org.eclipse.rdf4j.sail.lucene.SearchFields;
import org.eclipse.rdf4j.sail.lucene.SearchQuery;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.health.ClusterIndexHealth;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.ShapeRelation;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.GeoShapeQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.io.GeohashUtils;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Iterables;


/**
 * Requires an Elasticsearch cluster with the DeleteByQuery plugin.
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
	 * Set the parameter "waitForStatus=" to configure if {@link #initialize(java.util.Properties)
	 * initialization} should wait for a particular health status. The value can be one of "green" or
	 * "yellow". Does not wait by default.
	 */
	public static final String WAIT_FOR_STATUS_KEY = "waitForStatus";

	/**
	 * Set the parameter "waitForNodes=" to configure if {@link #initialize(java.util.Properties)
	 * initialization} should wait until the specified number of nodes are available. Does not wait by
	 * default.
	 */
	public static final String WAIT_FOR_NODES_KEY = "waitForNodes";

	/**
	 * Set the parameter "waitForActiveShards=" to configure if {@link #initialize(java.util.Properties)
	 * initialization} should wait until the specified number of shards to be active. Does not wait by
	 * default.
	 */
	public static final String WAIT_FOR_ACTIVE_SHARDS_KEY = "waitForActiveShards";

	/**
	 * Set the parameter "waitForRelocatingShards=" to configure if {@link #initialize(java.util.Properties)
	 * initialization} should wait until the specified number of nodes are relocating. Does not wait by
	 * default.
	 * 
	 * @deprecated use {@link #WAIT_FOR_NO_RELOCATING_SHARDS_KEY} in elastic search >= 5.x
	 */
	@Deprecated
	public static final String WAIT_FOR_RELOCATING_SHARDS_KEY = "waitForRelocatingShards";

	/**
	 * Set the parameter "waitForNoRelocatingShards=true|false" to configure if
	 * {@link #initialize(java.util.Properties) initialization} should wait until the are no relocating
	 * shards. Defaults to false, meaning the operation does not wait on there being no more relocating
	 * shards. Set to true to wait until the number of relocating shards in the cluster is 0.
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

	private volatile TransportClient client;

	private String clusterName;

	private String indexName;

	private String documentType;

	private String analyzer;

	private String queryAnalyzer = "standard";

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
	public void initialize(Properties parameters)
		throws Exception
	{
		super.initialize(parameters);
		indexName = parameters.getProperty(INDEX_NAME_KEY, DEFAULT_INDEX_NAME);
		documentType = parameters.getProperty(DOCUMENT_TYPE_KEY, DEFAULT_DOCUMENT_TYPE);
		analyzer = parameters.getProperty(LuceneSail.ANALYZER_CLASS_KEY, DEFAULT_ANALYZER);
		// slightly hacky cast to cope with the fact that Properties is
		// Map<Object,Object>
		// even though it is effectively Map<String,String>
		geoContextMapper = createSpatialContextMapper((Map<String, String>)(Map<?, ?>)parameters);

		Settings.Builder settingsBuilder = Settings.builder();
		for (Enumeration<?> iter = parameters.propertyNames(); iter.hasMoreElements();) {
			String propName = (String)iter.nextElement();
			if (propName.startsWith(ELASTICSEARCH_KEY_PREFIX)) {
				String esName = propName.substring(ELASTICSEARCH_KEY_PREFIX.length());
				settingsBuilder.put(esName, parameters.getProperty(propName));
			}
		}

		client = new PreBuiltTransportClient(settingsBuilder.build());
		String transport = parameters.getProperty(TRANSPORT_KEY, DEFAULT_TRANSPORT);
		for (String addrStr : transport.split(",")) {
			TransportAddress addr;
			if (addrStr.startsWith("local[")) {
				String id = addrStr.substring("local[".length(), addrStr.length() - 1);
				//addr = new LocalTransportAddress(id);
				throw new UnsupportedOperationException("Local Transport Address no longer supported");
			}
			else {
				String host;
				int port;
				String[] hostPort = addrStr.split(":");
				host = hostPort[0];
				if (hostPort.length > 1) {
					port = Integer.parseInt(hostPort[1]);
				}
				else {
					port = 9300;
				}
				addr = new TransportAddress(InetAddress.getByName(host), port);
			}
			client.addTransportAddress(addr);
		}
		clusterName = client.settings().get("cluster.name");

		boolean exists = client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
		if (!exists) {
			createIndex();
		}

		logger.info("Field mappings:\n{}", getMappings());

		ClusterHealthRequestBuilder healthReqBuilder = client.admin().cluster().prepareHealth(indexName);
		String waitForStatus = parameters.getProperty(WAIT_FOR_STATUS_KEY);
		if ("green".equals(waitForStatus)) {
			healthReqBuilder.setWaitForGreenStatus();
		}
		else if ("yellow".equals(waitForStatus)) {
			healthReqBuilder.setWaitForYellowStatus();
		}
		String waitForNodes = parameters.getProperty(WAIT_FOR_NODES_KEY);
		if (waitForNodes != null) {
			healthReqBuilder.setWaitForNodes(waitForNodes);
		}
		String waitForActiveShards = parameters.getProperty(WAIT_FOR_ACTIVE_SHARDS_KEY);
		if (waitForActiveShards != null) {
			healthReqBuilder.setWaitForActiveShards(Integer.parseInt(waitForActiveShards));
		}
		String waitForRelocatingShards = parameters.getProperty(WAIT_FOR_RELOCATING_SHARDS_KEY);
		if (waitForRelocatingShards != null) {
			logger.warn("Property " + WAIT_FOR_RELOCATING_SHARDS_KEY + " no longer supported. Use "
					+ WAIT_FOR_NO_RELOCATING_SHARDS_KEY + " instead");
		}
		String waitForNoRelocatingShards = parameters.getProperty(WAIT_FOR_NO_RELOCATING_SHARDS_KEY);
		if (waitForNoRelocatingShards != null) {
			healthReqBuilder.setWaitForNoRelocatingShards(Boolean.parseBoolean(waitForNoRelocatingShards));
		}
		ClusterHealthResponse healthResponse = healthReqBuilder.execute().actionGet();
		logger.info("Cluster health: {}", healthResponse.getStatus());
		logger.info("Cluster nodes: {} (data {})", healthResponse.getNumberOfNodes(),
				healthResponse.getNumberOfDataNodes());
		ClusterIndexHealth indexHealth = healthResponse.getIndices().get(indexName);
		logger.info("Index health: {}", indexHealth.getStatus());
		logger.info(
				"Index shards: {} (active {} [primary {}], initializing {}, unassigned {}, relocating {})",
				indexHealth.getNumberOfShards(), indexHealth.getActiveShards(),
				indexHealth.getActivePrimaryShards(), indexHealth.getInitializingShards(),
				indexHealth.getUnassignedShards(), indexHealth.getRelocatingShards());
	}

	protected Function<? super String, ? extends SpatialContext> createSpatialContextMapper(
			Map<String, String> parameters)
	{
		// this should really be based on the schema
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		SpatialContext geoContext = SpatialContextFactory.makeSpatialContext(parameters, classLoader);
		return Functions.constant(geoContext);
	}

	public Map<String, Object> getMappings()
		throws IOException
	{
		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> indexMappings = client.admin().indices().prepareGetMappings(
				indexName).setTypes(documentType).execute().actionGet().getMappings();
		ImmutableOpenMap<String, MappingMetaData> typeMappings = indexMappings.get(indexName);
		MappingMetaData mappings = typeMappings.get(documentType);
		return mappings.sourceAsMap();
	}

	private void createIndex()
		throws IOException
	{
		String settings = XContentFactory.jsonBuilder().startObject().field("index.query.default_field",
				SearchFields.TEXT_FIELD_NAME).startObject("analysis").startObject("analyzer").startObject(
						"default").field("type",
								analyzer).endObject().endObject().endObject().endObject().string();

		doAcknowledgedRequest(client.admin().indices().prepareCreate(indexName).setSettings(
				Settings.builder().loadFromSource(settings, XContentType.JSON)));

		// use _source instead of explicit stored = true
		XContentBuilder typeMapping = XContentFactory.jsonBuilder();
		typeMapping.startObject().startObject(documentType).startObject("properties");
		typeMapping.startObject(SearchFields.CONTEXT_FIELD_NAME).field("type", "keyword").field("index",
				true).field("copy_to", "_all").endObject();
		typeMapping.startObject(SearchFields.URI_FIELD_NAME).field("type", "keyword").field("index",
				true).field("copy_to", "_all").endObject();
		typeMapping.startObject(SearchFields.TEXT_FIELD_NAME).field("type", "text").field("index",
				true).field("copy_to", "_all").endObject();
		for (String wktField : wktFields) {
			typeMapping.startObject(toGeoPointFieldName(wktField)).field("type", "geo_point").endObject();
			if (supportsShapes(wktField)) {
				typeMapping.startObject(toGeoShapeFieldName(wktField)).field("type",
						"geo_shape").field("copy_to", "_all").endObject();
			}
		}
		typeMapping.endObject().endObject().endObject();

		doAcknowledgedRequest(
				client.admin().indices().preparePutMapping(indexName).setType(documentType).setSource(
						typeMapping));
	}

	private boolean supportsShapes(String field) {
		SpatialContext geoContext = geoContextMapper.apply(field);
		try {
			geoContext.readShapeFromWkt("POLYGON ((0 0, 1 0, 1 1, 0 1, 0 0))");
			return true;
		}
		catch (ParseException e) {
			return false;
		}
	}

	@Override
	protected SpatialContext getSpatialContext(String property) {
		return geoContextMapper.apply(property);
	}

	@Override
	public void shutDown()
		throws IOException
	{
		Client toCloseClient = client;
		client = null;
		if (toCloseClient != null) {
			toCloseClient.close();
		}
	}

	// //////////////////////////////// Methods for updating the index

	/**
	 * Returns a Document representing the specified document ID (combination of resource and context), or
	 * null when no such Document exists yet.
	 */
	@Override
	protected SearchDocument getDocument(String id)
		throws IOException
	{
		GetResponse response = client.prepareGet(indexName, documentType, id).execute().actionGet();
		if (response.isExists()) {
			return new ElasticsearchDocument(response.getId(), response.getType(), response.getIndex(),
					response.getVersion(), response.getSource(), geoContextMapper);
		}
		// no such Document
		return null;
	}

	@Override
	protected Iterable<? extends SearchDocument> getDocuments(String resourceId)
		throws IOException
	{
		SearchHits hits = getDocuments(QueryBuilders.termQuery(SearchFields.URI_FIELD_NAME, resourceId));
		return Iterables.transform(hits, new Function<SearchHit, SearchDocument>() {

			@Override
			public SearchDocument apply(SearchHit hit) {
				return new ElasticsearchDocument(hit, geoContextMapper);
			}
		});
	}

	@Override
	protected SearchDocument newDocument(String id, String resourceId, String context) {
		return new ElasticsearchDocument(id, documentType, indexName, resourceId, context, geoContextMapper);
	}

	@Override
	protected SearchDocument copyDocument(SearchDocument doc) {
		ElasticsearchDocument esDoc = (ElasticsearchDocument)doc;
		Map<String, Object> source = esDoc.getSource();
		Map<String, Object> newDocument = new HashMap<String, Object>(source);
		return new ElasticsearchDocument(esDoc.getId(), esDoc.getType(), esDoc.getIndex(), esDoc.getVersion(),
				newDocument, geoContextMapper);
	}

	@Override
	protected void addDocument(SearchDocument doc)
		throws IOException
	{
		ElasticsearchDocument esDoc = (ElasticsearchDocument)doc;
		doIndexRequest(client.prepareIndex(esDoc.getIndex(), esDoc.getType(), esDoc.getId()).setSource(
				esDoc.getSource()));
	}

	@Override
	protected void updateDocument(SearchDocument doc)
		throws IOException
	{
		ElasticsearchDocument esDoc = (ElasticsearchDocument)doc;
		doUpdateRequest(client.prepareUpdate(esDoc.getIndex(), esDoc.getType(), esDoc.getId()).setVersion(
				esDoc.getVersion()).setDoc(esDoc.getSource()));
	}

	@Override
	protected void deleteDocument(SearchDocument doc)
		throws IOException
	{
		ElasticsearchDocument esDoc = (ElasticsearchDocument)doc;
		client.prepareDelete(esDoc.getIndex(), esDoc.getType(), esDoc.getId()).setVersion(
				esDoc.getVersion()).execute().actionGet();
	}

	@Override
	protected BulkUpdater newBulkUpdate() {
		return new ElasticsearchBulkUpdater(client);
	}

	/**
	 * Returns a list of Documents representing the specified Resource (empty when no such Document exists
	 * yet). Each document represent a set of statements with the specified Resource as a subject, which are
	 * stored in a specific context
	 */
	private SearchHits getDocuments(QueryBuilder query)
		throws IOException
	{
		return search(client.prepareSearch(), query);
	}

	/**
	 * Returns a Document representing the specified Resource & Context combination, or null when no such
	 * Document exists yet.
	 */
	public SearchDocument getDocument(Resource subject, Resource context)
		throws IOException
	{
		// fetch the Document representing this Resource
		String resourceId = SearchFields.getResourceID(subject);
		String contextId = SearchFields.getContextID(context);
		return getDocument(SearchFields.formIdString(resourceId, contextId));
	}

	/**
	 * Returns a list of Documents representing the specified Resource (empty when no such Document exists
	 * yet). Each document represent a set of statements with the specified Resource as a subject, which are
	 * stored in a specific context
	 */
	public Iterable<? extends SearchDocument> getDocuments(Resource subject)
		throws IOException
	{
		String resourceId = SearchFields.getResourceID(subject);
		return getDocuments(resourceId);
	}

	/**
	 * Filters the given list of fields, retaining all property fields.
	 */
	public static Set<String> getPropertyFields(Set<String> fields) {
		Set<String> result = new HashSet<String>(fields.size());
		for (String field : fields) {
			if (SearchFields.isPropertyField(field))
				result.add(field);
		}
		return result;
	}

	@Override
	public void begin()
		throws IOException
	{
	}

	@Override
	public void commit()
		throws IOException
	{
		client.admin().indices().prepareRefresh(indexName).execute().actionGet();
	}

	@Override
	public void rollback()
		throws IOException
	{
	}

	@Override
	public void beginReading()
		throws IOException
	{
	}

	@Override
	public void endReading()
		throws IOException
	{
	}

	// //////////////////////////////// Methods for querying the index

	/**
	 * Parse the passed query. To be removed, no longer used.
	 * 
	 * @param query
	 *        string
	 * @return the parsed query
	 * @throws ParseException
	 *         when the parsing brakes
	 */
	@Override
	@Deprecated
	protected SearchQuery parseQuery(String query, IRI propertyURI)
		throws MalformedQueryException
	{
		QueryBuilder qb = prepareQuery(propertyURI, QueryBuilders.queryStringQuery(query));
		return new ElasticsearchQuery(client.prepareSearch(), qb, this);
	}

	/**
	 * Parse the passed query.
	 * 
	 * @param query
	 *        string
	 * @return the parsed query
	 * @throws ParseException
	 *         when the parsing brakes
	 */
	@Override
	protected Iterable<? extends DocumentScore> query(Resource subject, String query, IRI propertyURI,
			boolean highlight)
		throws MalformedQueryException, IOException
	{
		QueryBuilder qb = prepareQuery(propertyURI, QueryBuilders.queryStringQuery(query));
		SearchRequestBuilder request = client.prepareSearch();
		if (highlight) {
			HighlightBuilder hb = new HighlightBuilder();
			String field;
			if(propertyURI != null) {
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
			request.highlighter(hb);
		}

		SearchHits hits;
		if (subject != null) {
			hits = search(subject, request, qb);
		}
		else {
			hits = search(request, qb);
		}
		return Iterables.transform(hits, new Function<SearchHit, DocumentScore>() {

			@Override
			public DocumentScore apply(SearchHit hit) {
				return new ElasticsearchDocumentScore(hit, geoContextMapper);
			}
		});
	}

	// /**
	// * Parses an id-string used for a context filed (a serialized resource)
	// back to a resource.
	// * <b>CAN RETURN NULL</b>
	// * Inverse method of {@link #getResourceID(Resource)}
	// * @param idString
	// * @return null if the passed idString was the {@link #CONTEXT_NULL}
	// constant
	// */
	// private Resource getContextResource(String idString) {
	// if (CONTEXT_NULL.equals(idString))
	// return null;
	// else
	// return getResource(idString);
	// }

	/**
	 * Evaluates the given query only for the given resource.
	 */
	public SearchHits search(Resource resource, SearchRequestBuilder request, QueryBuilder query) {
		// rewrite the query
		QueryBuilder idQuery = QueryBuilders.termQuery(SearchFields.URI_FIELD_NAME,
				SearchFields.getResourceID(resource));
		QueryBuilder combinedQuery = QueryBuilders.boolQuery().must(idQuery).must(query);
		return search(request, combinedQuery);
	}

	@Override
	protected Iterable<? extends DocumentDistance> geoQuery(final IRI geoProperty, Point p, final IRI units,
			double distance, String distanceVar, Var contextVar)
		throws MalformedQueryException, IOException
	{
		double unitDist;
		final DistanceUnit unit;
		if (GEOF.UOM_METRE.equals(units)) {
			unit = DistanceUnit.METERS;
			unitDist = distance;
		}
		else if (GEOF.UOM_DEGREE.equals(units)) {
			unit = DistanceUnit.KILOMETERS;
			unitDist = unit.getDistancePerDegree() * distance;
		}
		else if (GEOF.UOM_RADIAN.equals(units)) {
			unit = DistanceUnit.KILOMETERS;
			unitDist = DistanceUtils.radians2Dist(distance, DistanceUtils.EARTH_MEAN_RADIUS_KM);
		}
		else if (GEOF.UOM_UNITY.equals(units)) {
			unit = DistanceUnit.KILOMETERS;
			unitDist = distance * Math.PI * DistanceUtils.EARTH_MEAN_RADIUS_KM;
		}
		else {
			throw new MalformedQueryException("Unsupported units: " + units);
		}

		double lat = p.getY();
		double lon = p.getX();
		final String fieldName = toGeoPointFieldName(SearchFields.getPropertyField(geoProperty));
		QueryBuilder qb = QueryBuilders.functionScoreQuery(
				QueryBuilders.geoDistanceQuery(fieldName).point(lat, lon).distance(unitDist, unit),
				ScoreFunctionBuilders.linearDecayFunction(fieldName, GeohashUtils.encodeLatLon(lat, lon),
						new DistanceUnit.Distance(unitDist, unit)));
		if (contextVar != null) {
			qb = addContextTerm(qb, (Resource)contextVar.getValue());
		}

		SearchRequestBuilder request = client.prepareSearch();
		SearchHits hits = search(request, qb);
		final GeoPoint srcPoint = new GeoPoint(lat, lon);
		return Iterables.transform(hits, new Function<SearchHit, DocumentDistance>() {

			@Override
			public DocumentDistance apply(SearchHit hit) {
				return new ElasticsearchDocumentDistance(hit, geoContextMapper, fieldName, units, srcPoint,
						unit);
			}
		});
	}

	private QueryBuilder addContextTerm(QueryBuilder qb, Resource ctx) {
		BoolQueryBuilder combinedQuery = QueryBuilders.boolQuery();
		QueryBuilder idQuery = QueryBuilders.termQuery(SearchFields.CONTEXT_FIELD_NAME,
				SearchFields.getContextID(ctx));
		if (ctx != null) {
			// the specified named graph
			combinedQuery.must(idQuery);
		}
		else {
			// not the unnamed graph
			combinedQuery.mustNot(idQuery);
		}
		combinedQuery.must(qb);
		return combinedQuery;
	}

	@Override
	protected Iterable<? extends DocumentResult> geoRelationQuery(String relation, IRI geoProperty,
			Shape shape, Var contextVar)
		throws MalformedQueryException, IOException
	{
		ShapeRelation spatialOp = toSpatialOp(relation);
		if (spatialOp == null) {
			return null;
		}
		final String fieldName = toGeoShapeFieldName(SearchFields.getPropertyField(geoProperty));
		GeoShapeQueryBuilder fb = QueryBuilders.geoShapeQuery(fieldName,
				ElasticsearchSpatialSupport.getSpatialSupport().toShapeBuilder(shape));
		fb.relation(spatialOp);
		QueryBuilder qb = QueryBuilders.matchAllQuery();
		if (contextVar != null) {
			qb = addContextTerm(qb, (Resource)contextVar.getValue());
		}

		SearchRequestBuilder request = client.prepareSearch();
		SearchHits hits = search(request, QueryBuilders.boolQuery().must(qb).filter(fb));
		return Iterables.transform(hits, new Function<SearchHit, DocumentResult>() {

			@Override
			public DocumentResult apply(SearchHit hit) {
				return new ElasticsearchDocumentResult(hit, geoContextMapper);
			}
		});
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

	/**
	 * Evaluates the given query and returns the results as a TopDocs instance.
	 */
	public SearchHits search(SearchRequestBuilder request, QueryBuilder query) {
		String[] types = getTypes();
		int nDocs;
		if (maxDocs > 0) {
			nDocs = maxDocs;
		}
		else {
			long docCount = client.prepareSearch(indexName).setTypes(types).setSource(
					new SearchSourceBuilder().size(0).query(query)).get().getHits().getTotalHits();
			nDocs = Math.max((int)Math.min(docCount, Integer.MAX_VALUE), 1);
		}
		SearchResponse response = request.setIndices(indexName).setTypes(types).setVersion(true).setQuery(
				query).setSize(nDocs).execute().actionGet();
		return response.getHits();
	}

	private QueryStringQueryBuilder prepareQuery(IRI propertyURI, QueryStringQueryBuilder query) {
		// check out which query parser to use, based on the given property URI
		if (propertyURI == null)
			// if we have no property given, we create a default query parser which
			// has the TEXT_FIELD_NAME as the default field
			query.defaultField(SearchFields.TEXT_FIELD_NAME).analyzer(queryAnalyzer);
		else
			// otherwise we create a query parser that has the given property as
			// the default field
			query.defaultField(toPropertyFieldName(SearchFields.getPropertyField(propertyURI))).analyzer(
					queryAnalyzer);
		return query;
	}

	/**
	 * @param contexts
	 * @param sail
	 *        - the underlying native sail where to read the missing triples from after deletion
	 * @throws SailException
	 */
	@Override
	public synchronized void clearContexts(Resource... contexts)
		throws IOException
	{

		// logger.warn("Clearing contexts operation did not change the index: contexts are not indexed at the moment");

		logger.debug("deleting contexts: {}", Arrays.toString(contexts));
		// these resources have to be read from the underlying rdf store
		// and their triples have to be added to the luceneindex after deletion of
		// documents
		// HashSet<Resource> resourcesToUpdate = new HashSet<Resource>();

		// remove all contexts passed
		for (Resource context : contexts) {
			// attention: context can be NULL!
			String contextString = SearchFields.getContextID(context);
			// IndexReader reader = getIndexReader();

			// now check all documents, and remember the URI of the resources
			// that were in multiple contexts
			// TermDocs termDocs = reader.termDocs(contextTerm);
			// try {
			// while (termDocs.next()) {
			// Document document = readDocument(reader, termDocs.doc());
			// // does this document have any other contexts?
			// Field[] fields = document.getFields(CONTEXT_FIELD_NAME);
			// for (Field f : fields)
			// {
			// if
			// (!contextString.equals(f.stringValue())&&!f.stringValue().equals("null"))
			// // there is another context
			// {
			// logger.debug("test new contexts: {}", f.stringValue());
			// // is it in the also contexts (lucky us if it is)
			// Resource otherContextOfDocument =
			// getContextResource(f.stringValue()); // can return null
			// boolean isAlsoDeleted = false;
			// for (Resource c: contexts){
			// if (c==null) {
			// if (otherContextOfDocument == null)
			// isAlsoDeleted = true;
			// } else
			// if (c.equals(otherContextOfDocument))
			// isAlsoDeleted = true;
			// }
			// // the otherContextOfDocument is now eihter marked for deletion or
			// not
			// if (!isAlsoDeleted) {
			// // get ID of document
			// Resource r = getResource(document);
			// resourcesToUpdate.add(r);
			// }
			// }
			// }
			// }
			// } finally {
			// termDocs.close();
			// }

			// now delete all documents from the deleted context
			DeleteByQueryAction.INSTANCE.newRequestBuilder(client).source(indexName).filter(
					QueryBuilders.termQuery(SearchFields.CONTEXT_FIELD_NAME, contextString)).get();
		}

		// now add those again, that had other contexts also.
		// SailConnection con = sail.getConnection();
		// try {
		// // for each resource, add all
		// for (Resource resource : resourcesToUpdate) {
		// logger.debug("re-adding resource {}", resource);
		// ArrayList<Statement> toAdd = new ArrayList<Statement>();
		// CloseableIteration<? extends Statement, SailException> it =
		// con.getStatements(resource, null, null, false);
		// while (it.hasNext()) {
		// Statement s = it.next();
		// toAdd.add(s);
		// }
		// addDocument(resource, toAdd);
		// }
		// } finally {
		// con.close();
		// }

	}

	/**
	 * 
	 */
	@Override
	public synchronized void clear()
		throws IOException
	{
		doAcknowledgedRequest(client.admin().indices().prepareDelete(indexName));
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

	private static void doAcknowledgedRequest(
			ActionRequestBuilder<?, ? extends AcknowledgedResponse, ?> request)
		throws IOException
	{
		boolean ok = request.execute().actionGet().isAcknowledged();
		if (!ok) {
			throw new IOException("Request not acknowledged: " + request.get().getClass().getName());
		}
	}

	private static long doIndexRequest(ActionRequestBuilder<?, ? extends IndexResponse, ?> request)
		throws IOException
	{
		IndexResponse response = request.execute().actionGet();
		boolean ok = response.status().equals(RestStatus.CREATED);
		if (!ok) {
			throw new IOException("Document not created: " + request.get().getClass().getName());
		}
		return response.getVersion();
	}

	private static long doUpdateRequest(ActionRequestBuilder<?, ? extends UpdateResponse, ?> request)
		throws IOException
	{
		UpdateResponse response = request.execute().actionGet();
		boolean isUpsert = response.status().equals(RestStatus.CREATED);
		if (isUpsert) {
			throw new IOException("Unexpected upsert: " + request.get().getClass().getName());
		}
		return response.getVersion();
	}
}
