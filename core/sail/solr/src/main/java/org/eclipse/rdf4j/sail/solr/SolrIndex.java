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
package org.eclipse.rdf4j.sail.solr;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SpatialParams;
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
import org.eclipse.rdf4j.sail.lucene.QuerySpec;
import org.eclipse.rdf4j.sail.lucene.SearchDocument;
import org.eclipse.rdf4j.sail.lucene.SearchFields;
import org.eclipse.rdf4j.sail.lucene.util.GeoUnits;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.SpatialRelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Iterables;

/**
 * @see LuceneSail
 */
public class SolrIndex extends AbstractSearchIndex {

	public static final String SERVER_KEY = "server";

	public static final String DISTANCE_FIELD = "_dist";

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private SolrClient client;

	private Function<? super String, ? extends SpatialContext> geoContextMapper;

	@Override
	public void initialize(Properties parameters) throws Exception {
		super.initialize(parameters);
		// slightly hacky cast to cope with the fact that Properties is
		// Map<Object,Object>
		// even though it is effectively Map<String,String>
		this.geoContextMapper = createSpatialContextMapper((Map<String, String>) (Map<?, ?>) parameters);

		String server = parameters.getProperty(SERVER_KEY);
		if (server == null) {
			throw new SailException("Missing " + SERVER_KEY + " parameter");
		}
		int pos = server.indexOf(':');
		if (pos == -1) {
			throw new SailException("Missing scheme in " + SERVER_KEY + " parameter: " + server);
		}
		String scheme = server.substring(0, pos);
		Class<?> clientFactoryCls = Class.forName("org.eclipse.rdf4j.sail.solr.client." + scheme + ".Factory");
		SolrClientFactory clientFactory = (SolrClientFactory) clientFactoryCls.newInstance();
		client = clientFactory.create(server);
	}

	protected Function<? super String, ? extends SpatialContext> createSpatialContextMapper(
			Map<String, String> parameters) {
		// this should really be based on the schema
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		SpatialContext geoContext = SpatialContextFactory.makeSpatialContext(parameters, classLoader);
		return Functions.constant(geoContext);
	}

	public SolrClient getClient() {
		return client;
	}

	@Override
	protected SpatialContext getSpatialContext(String property) {
		return geoContextMapper.apply(property);
	}

	@Override
	public void shutDown() throws IOException {
		if (client != null) {
			client.close();
			client = null;
		}
	}

	// //////////////////////////////// Methods for updating the index

	/**
	 * Returns a Document representing the specified document ID (combination of resource and context), or null when no
	 * such Document exists yet.
	 *
	 * @param id
	 * @return search document
	 * @throws IOException
	 */
	@Override
	protected SearchDocument getDocument(String id) throws IOException {
		SolrDocument doc;
		try {
			doc = (SolrDocument) client
					.query(new SolrQuery().setRequestHandler("/get").set(SearchFields.ID_FIELD_NAME, id))
					.getResponse()
					.get("doc");
		} catch (SolrServerException e) {
			throw new IOException(e);
		}
		return (doc != null) ? new SolrSearchDocument(doc) : null;
	}

	@Override
	protected Iterable<? extends SearchDocument> getDocuments(String resourceId) throws IOException {
		SolrQuery query = new SolrQuery(termQuery(SearchFields.URI_FIELD_NAME, resourceId));
		SolrDocumentList docs;
		try {
			docs = getDocuments(query);
		} catch (SolrServerException e) {
			throw new IOException(e);
		}
		return Iterables.transform(docs, SolrSearchDocument::new);
	}

	@Override
	protected SearchDocument newDocument(String id, String resourceId, String context) {
		return new SolrSearchDocument(id, resourceId, context);
	}

	@Override
	protected SearchDocument copyDocument(SearchDocument doc) {
		SolrDocument document = ((SolrSearchDocument) doc).getDocument();
		SolrDocument newDocument = new SolrDocument();
		newDocument.putAll(document);
		return new SolrSearchDocument(newDocument);
	}

	@Override
	protected void addDocument(SearchDocument doc) throws IOException {
		SolrDocument document = ((SolrSearchDocument) doc).getDocument();
		try {
			client.add(SolrUtil.toSolrInputDocument(document));
		} catch (SolrServerException e) {
			throw new IOException(e);
		}
	}

	@Override
	protected void updateDocument(SearchDocument doc) throws IOException {
		addDocument(doc);
	}

	@Override
	protected void deleteDocument(SearchDocument doc) throws IOException {
		try {
			client.deleteById(doc.getId());
		} catch (SolrServerException e) {
			throw new IOException(e);
		}
	}

	@Override
	protected BulkUpdater newBulkUpdate() {
		return new SolrBulkUpdater(client);
	}

	static String termQuery(String field, String value) {
		return field + ":\"" + value + "\"";
	}

	/**
	 * Returns a list of Documents representing the specified Resource (empty when no such Document exists yet). Each
	 * document represent a set of statements with the specified Resource as a subject, which are stored in a specific
	 * context
	 */
	private SolrDocumentList getDocuments(SolrQuery query) throws SolrServerException, IOException {
		return search(query).getResults();
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
		try {
			client.commit();
		} catch (SolrServerException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void rollback() throws IOException {
		try {
			client.rollback();
		} catch (SolrServerException e) {
			throw new IOException(e);
		}
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
		SolrQuery q = prepareQuery(propertyURI, new SolrQuery(query));
		if (highlight) {
			q.setHighlight(true);
			String field = (propertyURI != null) ? SearchFields.getPropertyField(propertyURI) : "*";
			q.addHighlightField(field);
			q.setHighlightSimplePre(SearchFields.HIGHLIGHTER_PRE_TAG);
			q.setHighlightSimplePost(SearchFields.HIGHLIGHTER_POST_TAG);
			q.setHighlightSnippets(2);
		}

		QueryResponse response;
		if (q.getHighlight()) {
			q.addField("*");
		} else {
			q.addField(SearchFields.URI_FIELD_NAME);
		}
		q.addField("score");
		try {
			if (subject != null) {
				response = search(subject, q);
			} else {
				response = search(q);
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
	 * Evaluates the given query only for the given resource.
	 *
	 * @param resource
	 * @param query
	 * @return response
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public QueryResponse search(Resource resource, SolrQuery query) throws SolrServerException, IOException {
		// rewrite the query
		String idQuery = termQuery(SearchFields.URI_FIELD_NAME, SearchFields.getResourceID(resource));
		query.setQuery(query.getQuery() + " AND " + idQuery);
		return search(query);
	}

	@Override
	protected Iterable<? extends DocumentDistance> geoQuery(IRI geoProperty, Point p, final IRI units, double distance,
			String distanceVar, Var contextVar) throws MalformedQueryException, IOException {
		double kms = GeoUnits.toKilometres(distance, units);

		String qstr = "{!geofilt score=recipDistance}";
		if (contextVar != null) {
			Resource ctx = (Resource) contextVar.getValue();
			String tq = termQuery(SearchFields.CONTEXT_FIELD_NAME, SearchFields.getContextID(ctx));
			if (ctx != null) {
				qstr = tq + " AND " + qstr;
			} else {
				qstr = "-" + tq + " AND " + qstr;
			}
		}
		SolrQuery q = new SolrQuery(qstr);
		q.set(SpatialParams.FIELD, SearchFields.getPropertyField(geoProperty));
		q.set(SpatialParams.POINT, p.getY() + "," + p.getX());
		q.set(SpatialParams.DISTANCE, Double.toString(kms));
		q.addField(SearchFields.URI_FIELD_NAME);
		// ':' is part of the fl parameter syntax so we can't use the full
		// property field name
		// instead we use wildcard + local part of the property URI
		q.addField("*" + geoProperty.getLocalName());
		// always include the distance - needed for sanity checking
		q.addField(DISTANCE_FIELD + ":geodist()");
		boolean requireContext = (contextVar != null && !contextVar.hasValue());
		if (requireContext) {
			q.addField(SearchFields.CONTEXT_FIELD_NAME);
		}

		QueryResponse response;
		try {
			response = search(q);
		} catch (SolrServerException e) {
			throw new IOException(e);
		}

		SolrDocumentList results = response.getResults();
		return Iterables.transform(results, (SolrDocument document) -> {
			SolrSearchDocument doc = new SolrSearchDocument(document);
			return new SolrDocumentDistance(doc, units);
		});
	}

	@Override
	protected Iterable<? extends DocumentResult> geoRelationQuery(String relation, IRI geoProperty, String wkt,
			Var contextVar) throws MalformedQueryException, IOException {
		String spatialOp = toSpatialOp(relation);
		if (spatialOp == null) {
			return null;
		}
		String qstr = "\"" + spatialOp + "(" + wkt + ")\"";
		if (contextVar != null) {
			Resource ctx = (Resource) contextVar.getValue();
			String tq = termQuery(SearchFields.CONTEXT_FIELD_NAME, SearchFields.getContextID(ctx));
			if (ctx != null) {
				qstr = tq + " AND " + qstr;
			} else {
				qstr = "-" + tq + " AND " + qstr;
			}
		}
		SolrQuery q = new SolrQuery(qstr);
		q.set(CommonParams.DF, SearchFields.getPropertyField(geoProperty));
		q.addField(SearchFields.URI_FIELD_NAME);
		// ':' is part of the fl parameter syntax so we can't use the full
		// property field name
		// instead we use wildcard + local part of the property URI
		q.addField("*" + geoProperty.getLocalName());
		boolean requireContext = (contextVar != null && !contextVar.hasValue());
		if (requireContext) {
			q.addField(SearchFields.CONTEXT_FIELD_NAME);
		}

		QueryResponse response;
		try {
			response = search(q);
		} catch (SolrServerException e) {
			throw new IOException(e);
		}

		SolrDocumentList results = response.getResults();
		return Iterables.transform(results, (SolrDocument document) -> {
			SolrSearchDocument doc = new SolrSearchDocument(document);
			return new SolrDocumentResult(doc);
		});
	}

	private String toSpatialOp(String relation) {
		if (GEOF.SF_INTERSECTS.stringValue().equals(relation)) {
			return "Intersects";
		}
		if (GEOF.SF_DISJOINT.stringValue().equals(relation)) {
			return "IsDisjointTo";
		}
		if (GEOF.EH_COVERED_BY.stringValue().equals(relation)) {
			return "IsWithin";
		}
		return null;
	}

	@Override
	protected Shape parseQueryShape(String property, String value) throws ParseException {
		Shape s = super.parseQueryShape(property, value);
		// workaround to preserve WKT string
		return (s instanceof Point) ? new WktPoint((Point) s, value) : new WktShape<>(s, value);
	}

	protected String toWkt(Shape s) {
		return ((WktShape<?>) s).wkt;
	}

	private static class WktShape<S extends Shape> implements Shape {

		final S s;

		final String wkt;

		WktShape(S s, String wkt) {
			this.s = s;
			this.wkt = wkt;
		}

		@Override
		public SpatialRelation relate(Shape other) {
			return s.relate(other);
		}

		@Override
		public Rectangle getBoundingBox() {
			return s.getBoundingBox();
		}

		@Override
		public boolean hasArea() {
			return s.hasArea();
		}

		@Override
		public double getArea(SpatialContext ctx) {
			return s.getArea(ctx);
		}

		@Override
		public Point getCenter() {
			return s.getCenter();
		}

		@Override
		public Shape getBuffered(double distance, SpatialContext ctx) {
			return s.getBuffered(distance, ctx);
		}

		@Override
		public boolean isEmpty() {
			return s.isEmpty();
		}

		@Override
		public boolean equals(Object other) {
			return s.equals(other);
		}

		@Override
		public SpatialContext getContext() {
			return s.getContext();
		}
	}

	private static class WktPoint extends WktShape<Point> implements Point {

		WktPoint(Point p, String wkt) {
			super(p, wkt);
		}

		@Override
		public void reset(double x, double y) {
			s.reset(x, y);
		}

		@Override
		public double getX() {
			return s.getX();
		}

		@Override
		public double getY() {
			return s.getY();
		}

	}

	/**
	 * Evaluates the given query and returns the results as a TopDocs instance.
	 *
	 * @param query
	 * @return query response
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public QueryResponse search(SolrQuery query) throws SolrServerException, IOException {
		int nDocs;
		if (maxDocs > 0) {
			nDocs = maxDocs;
		} else {
			long docCount = client.query(query.setRows(0)).getResults().getNumFound();
			nDocs = Math.max((int) Math.min(docCount, Integer.MAX_VALUE), 1);
		}
		return client.query(query.setRows(nDocs));
	}

	private SolrQuery prepareQuery(IRI propertyURI, SolrQuery query) {
		// check out which query parser to use, based on the given property URI
		if (propertyURI == null)
		// if we have no property given, we create a default query parser which
		// has the TEXT_FIELD_NAME as the default field
		{
			query.set(CommonParams.DF, SearchFields.TEXT_FIELD_NAME);
		} else
		// otherwise we create a query parser that has the given property as
		// the default field
		{
			query.set(CommonParams.DF, SearchFields.getPropertyField(propertyURI));
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

		try {
			// remove all contexts passed
			for (Resource context : contexts) {
				// attention: context can be NULL!
				String contextString = SearchFields.getContextID(context);
				client.deleteByQuery(termQuery(SearchFields.CONTEXT_FIELD_NAME, contextString));
			}
		} catch (SolrServerException e) {
			throw new IOException(e);
		}
	}

	/**
	 *
	 */
	@Override
	public synchronized void clear() throws IOException {
		try {
			client.deleteByQuery("*:*");
		} catch (SolrServerException e) {
			throw new IOException(e);
		}
	}
}
