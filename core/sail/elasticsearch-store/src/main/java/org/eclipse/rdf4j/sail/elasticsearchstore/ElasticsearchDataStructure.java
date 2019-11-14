/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * @author Håvard Mikkelsen Ottestad
 */
class ElasticsearchDataStructure extends DataStructureInterface {

	private static final String mapping;

	private final int BUFFER_THRESHOLD = 1024 * 16;
	private Set<Statement> addStatementBuffer = Collections.synchronizedSet(new HashSet<>());
	private Set<ElasticsearchId> deleteStatementBuffer = Collections.synchronizedSet(new HashSet<>());

	private final static ElasticsearchValueFactory vf = ElasticsearchValueFactory.getInstance();

	static {
		try {
			mapping = IOUtils.toString(ElasticsearchDataStructure.class.getClassLoader()
					.getResourceAsStream("elasticsearchStoreMapping.json"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchDataStructure.class);

	private static final String ELASTICSEARCH_TYPE = "statement";
	private final String index;
	private final String hostname;
	private final int port;
	private int scrollTimeout = 60000;

	ElasticsearchDataStructure(String hostname, int port, String index) {
		super();
		this.hostname = hostname;
		this.port = port;
		this.index = index;
	}

	private void createIndex() {

		CreateIndexRequest request = new CreateIndexRequest(index);

		request.mapping(ELASTICSEARCH_TYPE, mapping, XContentType.JSON);

		try (Client client = getClient()) {
			boolean indexExistsAlready = client.admin()
					.indices()
					.exists(new IndicesExistsRequest(index))
					.actionGet()
					.isExists();

			if (!indexExistsAlready) {
				client.admin().indices().create(request).actionGet();
			}
		}

	}

	@Override
	synchronized public void addStatement(Client client, Statement statement) {
		if (addStatementBuffer.size() >= BUFFER_THRESHOLD) {
			flushAddStatementBuffer(client);
		}

		addStatementBuffer.add(statement);

	}

	@Override
	synchronized public void removeStatement(Client client, Statement statement) {

		ElasticsearchId elasticsearchIdStatement;

		if (statement instanceof ElasticsearchId) {

			elasticsearchIdStatement = (ElasticsearchId) statement;

		} else {

			String id = sha256(statement);

			if (statement.getContext() == null) {
				elasticsearchIdStatement = vf.createStatement(id, statement.getSubject(), statement.getPredicate(),
						statement.getPredicate());
			} else {
				elasticsearchIdStatement = vf.createStatement(id, statement.getSubject(), statement.getPredicate(),
						statement.getPredicate(), statement.getContext());
			}

		}

		if (deleteStatementBuffer.size() >= BUFFER_THRESHOLD) {
			flushRemoveStatementBuffer(client);
		}

		deleteStatementBuffer.add(elasticsearchIdStatement);

	}

	@Override
	synchronized public void clear(Client client, Resource[] contexts) {

		BulkByScrollResponse response = DeleteByQueryAction.INSTANCE.newRequestBuilder(client)
				.filter(getQueryBuilder(null, null, null, contexts))
				.abortOnVersionConflict(false)
				.source(index)
				.get();

		long deleted = response.getDeleted();
	}

	@Override
	void flushThrough(Client client) {
		// no underlying store to flush to
	}

	CloseableIteration<SearchHit, RuntimeException> getScrollingIterator(Client client, QueryBuilder queryBuilder) {

		return new CloseableIteration<SearchHit, RuntimeException>() {

			Iterator<SearchHit> items;
			String scrollId;
			long itemsRetrieved = 0;
			int size = 1000;

			{

				SearchResponse scrollResp = client.prepareSearch(index)
						.setScroll(new TimeValue(scrollTimeout))
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
								.setScroll(new TimeValue(scrollTimeout))
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

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Client client, Resource subject,
			IRI predicate,
			Value object, Resource... context) {

		QueryBuilder queryBuilder = getQueryBuilder(subject, predicate, object, context);

		return new CloseableIteration<Statement, SailException>() {

			CloseableIteration<SearchHit, RuntimeException> iterator = getScrollingIterator(client, queryBuilder);

			Statement next;

			public void calculateNext() throws SailException {
				if (next != null) {
					return;
				}

				while (iterator.hasNext() && next == null) {
					SearchHit nextSearchHit = iterator.next();

					Map<String, Object> sourceAsMap = nextSearchHit.getSourceAsMap();

					String id = nextSearchHit.getId();

					Statement statement = sourceToStatement(sourceAsMap, id);

					// we use hash to lookup the object value because the object can be bigger than what elasticsearch
					// allows as max for keyword (32766 bytes), so it needs to be stored in a text field that is not
					// index. The hash is stored in an integer field and is index. The code below does hash collision
					// check.
					if (object != null
							&& object.stringValue().hashCode() == statement.getObject().stringValue().hashCode()
							&& !object.equals(statement.getObject())) {
						continue;
					}

					next = statement;

				}
			}

			@Override
			public boolean hasNext() throws SailException {
				calculateNext();
				return next != null;
			}

			@Override
			public Statement next() throws SailException {

				calculateNext();
				Statement tempNext = next;
				next = null;
				return tempNext;
			}

			@Override
			public void remove() throws SailException {

				throw new IllegalStateException("Does not support removing from iterator");

			}

			@Override
			public void close() throws SailException {
				iterator.close();

			}
		};

	}

	private QueryBuilder getQueryBuilder(Resource subject, IRI predicate, Value object, Resource[] contexts) {
		boolean matchAll = true;

		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

		if (subject != null) {
			matchAll = false;
			boolQueryBuilder.must(QueryBuilders.termQuery("subject", subject.stringValue()));
			if (subject instanceof IRI) {
				boolQueryBuilder.must(QueryBuilders.termQuery("subject_IRI", true));
			} else {
				boolQueryBuilder.must(QueryBuilders.termQuery("subject_BNode", true));
			}
		}

		if (predicate != null) {
			matchAll = false;
			boolQueryBuilder.must(QueryBuilders.termQuery("predicate", predicate.stringValue()));
		}

		if (object != null) {
			matchAll = false;
			boolQueryBuilder.must(QueryBuilders.termQuery("object_Hash", object.stringValue().hashCode()));
			if (object instanceof IRI) {
				boolQueryBuilder.must(QueryBuilders.termQuery("object_IRI", true));
			} else if (object instanceof BNode) {
				boolQueryBuilder.must(QueryBuilders.termQuery("object_BNode", true));
			} else {
				boolQueryBuilder.must(
						QueryBuilders.termQuery("object_Datatype", ((Literal) object).getDatatype().stringValue()));
				if (((Literal) object).getLanguage().isPresent()) {
					boolQueryBuilder
							.must(QueryBuilders.termQuery("object_Lang", ((Literal) object).getLanguage().get()));
				}
			}
		}

		if (contexts != null && contexts.length > 0) {
			matchAll = false;

			BoolQueryBuilder contextQueryBuilder = new BoolQueryBuilder();

			for (Resource context : contexts) {

				if (context == null) {

					contextQueryBuilder.should(new BoolQueryBuilder().mustNot(QueryBuilders.existsQuery("context")));

				} else if (context instanceof IRI) {

					contextQueryBuilder.should(
							new BoolQueryBuilder()
									.must(QueryBuilders.termQuery("context", context.stringValue()))
									.must(QueryBuilders.termQuery("context_IRI", true)));

				} else { // BNode
					contextQueryBuilder.should(
							new BoolQueryBuilder()
									.must(QueryBuilders.termQuery("context", context.stringValue()))
									.must(QueryBuilders.termQuery("context_BNode", true)));
				}

			}

			boolQueryBuilder.must(contextQueryBuilder);

		}

		QueryBuilder queryBuilder;

		if (matchAll) {
			queryBuilder = matchAllQuery();
		} else {
			queryBuilder = boolQueryBuilder;

		}

		return QueryBuilders.constantScoreQuery(queryBuilder);
	}

	@Override
	public void flush(Client client) {

		flushAddStatementBuffer(client);
		flushRemoveStatementBuffer(client);

		client.admin()
				.indices()
				.prepareRefresh(index)
				.get();

	}

	synchronized private void flushAddStatementBuffer(Client client) {

		if (addStatementBuffer.isEmpty()) {
			return;
		}

		BulkRequestBuilder bulkRequest = client.prepareBulk();

		int failures = 0;

		do {

			addStatementBuffer

					.stream()
					.parallel()
					.map(statement -> {
						XContentBuilder builder;

						try {
							builder = jsonBuilder()
									.startObject()
									.field("subject", statement.getSubject().stringValue())
									.field("predicate", statement.getPredicate().stringValue())
									.field("object", statement.getObject().stringValue())
									.field("object_Hash", statement.getObject().stringValue().hashCode());

							Resource context = statement.getContext();

							if (context != null) {
								builder.field("context", context.stringValue());

								if (context instanceof IRI) {
									builder.field("context_IRI", true);
								} else {
									builder.field("context_BNode", true);
								}
							}

							if (statement.getSubject() instanceof IRI) {
								builder.field("subject_IRI", true);
							} else {
								builder.field("subject_BNode", true);
							}

							if (statement.getObject() instanceof IRI) {
								builder.field("object_IRI", true);
							} else if (statement.getObject() instanceof BNode) {
								builder.field("object_BNode", true);
							} else {
								builder.field("object_Datatype",
										((Literal) statement.getObject()).getDatatype().stringValue());
								if (((Literal) statement.getObject()).getLanguage().isPresent()) {
									builder.field("object_Lang", ((Literal) statement.getObject()).getLanguage().get());

								}
							}

							builder.endObject();

							return new BuilderAndSha(sha256(statement), builder);

						} catch (IOException e) {
							throw new IllegalStateException(e);
						}

					})
					.collect(Collectors.toList())
					.forEach(builderAndSha -> {

						bulkRequest.add(client.prepareIndex(index, ELASTICSEARCH_TYPE, builderAndSha.getSha256())
								.setSource(builderAndSha.getBuilder())
								.setOpType(DocWriteRequest.OpType.CREATE));

					});

			BulkResponse bulkResponse = bulkRequest.get();
			if (bulkResponse.hasFailures()) {

				List<BulkItemResponse> bulkItemResponses = getBulkItemResponses(bulkResponse);

				logger.info("Elasticsearch has failures when adding data, retrying. Message: {}",
						bulkResponse.buildFailureMessage());

				boolean onlyVersionConflicts = bulkItemResponses.stream()
						.allMatch(resp -> resp.getFailure().getCause() instanceof VersionConflictEngineException);
				if (onlyVersionConflicts) {
					// probably trying to add duplicates, or we have a hash conflict

					Set<String> failedIDs = bulkItemResponses.stream()
							.map(BulkItemResponse::getId)
							.collect(Collectors.toSet());

					// clean up addedStatements
					addStatementBuffer = addStatementBuffer.stream()
							.filter(statement -> failedIDs.contains(sha256(statement))) // we only want to retry failed
							// statements
							// filter out duplicates
							.filter(statement -> {

								String sha256 = sha256(statement);
								Statement statementById = getStatementById(client, sha256);

								return !statement.equals(statementById);
							})

							// now we only have conflicts
							.map(statement -> {
								// TODO handle conflict. Probably by doing something to change to id, mark it as a
								// conflict. Store all the conflicts in memory, to check against and refresh them from
								// disc when we boot

								return statement;

							})
							.collect(Collectors.toSet());

				} else {
					failures++;
					if (failures > 10) {
						throw new RuntimeException("Elasticsearch has failed " + failures
								+ " times when adding data, retrying. Message: " + bulkResponse.buildFailureMessage());
					}

					try {
						Thread.sleep(failures * 100);
					} catch (InterruptedException ignored) {
					}
				}

			} else {
				failures = 0;
			}

		} while (failures > 0);

		logger.info("Added {} statements", addStatementBuffer.size());

		addStatementBuffer = Collections.synchronizedSet(new HashSet<>(BUFFER_THRESHOLD));

	}

	private Statement getStatementById(Client client, String sha256) {
		Map<String, Object> source = client.prepareGet(index, ELASTICSEARCH_TYPE, sha256).get().getSource();

		return sourceToStatement(source, sha256);

	}

	private List<BulkItemResponse> getBulkItemResponses(BulkResponse bulkResponse) {
		List<BulkItemResponse> bulkItemResponses = new ArrayList<>();
		Iterator<BulkItemResponse> iterator = bulkResponse.iterator();

		while (iterator.hasNext()) {
			bulkItemResponses.add(iterator.next());
		}
		return bulkItemResponses;
	}

	synchronized private void flushRemoveStatementBuffer(Client client) {

		if (deleteStatementBuffer.isEmpty()) {
			return;
		}

		BulkRequestBuilder bulkRequest = client.prepareBulk();

		int failures = 0;

		do {

			deleteStatementBuffer.forEach(statement -> {

				bulkRequest.add(client.prepareDelete(index, ELASTICSEARCH_TYPE, statement.getElasticsearchId()));

			});

			BulkResponse bulkResponse = bulkRequest.get();
			if (bulkResponse.hasFailures()) {
				failures++;
				if (failures < 10) {
					logger.warn("Elasticsearch has failures when adding data, retrying. Message: {}",
							bulkResponse.buildFailureMessage());
				} else {
					throw new RuntimeException("Elasticsearch has failed " + failures
							+ " times when adding data, retrying. Message: " + bulkResponse.buildFailureMessage());
				}

			} else {
				failures = 0;
			}

		} while (failures > 0);

		deleteStatementBuffer = Collections.synchronizedSet(new HashSet<>(BUFFER_THRESHOLD));

	}

	@Override
	String getHostname() {
		return hostname;
	}

	@Override
	int getPort() {
		return port;
	}

	@Override
	String getClustername() {
		return "cluster1";
	}

	@Override
	void init() {

		createIndex();
	}

	private Client getClient() {
		try {
			Settings settings = Settings.builder().put("cluster.name", "cluster1").build();
			TransportClient client = new PreBuiltTransportClient(settings);
			client.addTransportAddress(new TransportAddress(InetAddress.getByName(hostname), port));
			return client;
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setElasticsearchScrollTimeout(int timeout) {
		this.scrollTimeout = timeout;
	}

	String sha256(Statement statement) {

		String originalString = statement.toString();

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			byte[] hash = digest.digest(originalString.getBytes(StandardCharsets.UTF_8));

			StringBuilder hexString = new StringBuilder();
			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

	}

	private static Statement sourceToStatement(Map<String, Object> sourceAsMap, String id) {

		Resource subjectRes;
		if (sourceAsMap.containsKey("subject_IRI")) {
			subjectRes = vf.createIRI(sourceAsMap.get("subject").toString());
		} else {
			subjectRes = vf.createBNode(sourceAsMap.get("subject").toString());
		}

		IRI predicateRes = vf.createIRI(sourceAsMap.get("predicate").toString());

		Value objectRes;

		String objectString = sourceAsMap.get("object").toString();

		if (sourceAsMap.containsKey("object_IRI")) {
			objectRes = vf.createIRI(objectString);
		} else if (sourceAsMap.containsKey("object_BNode")) {
			objectRes = vf.createBNode(objectString);
		} else {
			if (sourceAsMap.containsKey("object_Lang")) {
				objectRes = vf.createLiteral(objectString, sourceAsMap.get("object_Lang").toString());

			} else {
				objectRes = vf.createLiteral(objectString,
						vf.createIRI(sourceAsMap.get("object_Datatype").toString()));

			}

		}

		Resource contextRes = null;
		if (sourceAsMap.containsKey("context_IRI")) {
			contextRes = vf.createIRI(sourceAsMap.get("context").toString());
		} else if (sourceAsMap.containsKey("context_BNode")) {
			contextRes = vf.createBNode(sourceAsMap.get("context").toString());
		}

		Statement statement;

		if (contextRes != null) {
			statement = vf.createStatement(id, subjectRes, predicateRes, objectRes, contextRes);
		} else {
			statement = vf.createStatement(id, subjectRes, predicateRes, objectRes);
		}
		return statement;
	}

}

class BuilderAndSha {
	private final String sha256;
	private final XContentBuilder builder;

	BuilderAndSha(String sha256, XContentBuilder builder) {
		this.sha256 = sha256;
		this.builder = builder;
	}

	String getSha256() {
		return sha256;
	}

	XContentBuilder getBuilder() {
		return builder;
	}
}
