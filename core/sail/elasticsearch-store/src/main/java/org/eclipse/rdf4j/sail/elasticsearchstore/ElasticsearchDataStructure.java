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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.DataStructureInterface;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatement;
import org.elasticsearch.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.clients.elasticsearch._types.Conflicts;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;

/**
 * @author Håvard Mikkelsen Ottestad
 */
class ElasticsearchDataStructure implements DataStructureInterface {

	private static final String MAPPING;

	private int BUFFER_THRESHOLD = 1024 * 16;
	private final ClientProvider clientProvider;
	private Set<ExtensibleStatement> addStatementBuffer = new HashSet<>();
	private Set<ElasticsearchId> deleteStatementBuffer = new HashSet<>();

	private final static ElasticsearchValueFactory vf = (ElasticsearchValueFactory) ElasticsearchValueFactory
			.getInstance();

	static {
		try {
			MAPPING = IOUtils.toString(ElasticsearchDataStructure.class.getClassLoader()
					.getResourceAsStream("elasticsearchStoreMapping.json"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchDataStructure.class);

	private static final String ELASTICSEARCH_TYPE = "statement";
	private final String index;
	private int scrollTimeout = 60000;

	ElasticsearchDataStructure(ClientProvider clientProvider, String index) {
		super();
		this.index = index;
		this.clientProvider = clientProvider;
	}

	@Override
	synchronized public void addStatement(ExtensibleStatement statement) {
		if (addStatementBuffer.size() >= BUFFER_THRESHOLD) {
			flushAddStatementBuffer();
		}

		addStatementBuffer.add(statement);

	}

	@Override
	synchronized public void removeStatement(ExtensibleStatement statement) {

		ElasticsearchId elasticsearchIdStatement;

		if (statement instanceof ElasticsearchId) {

			elasticsearchIdStatement = (ElasticsearchId) statement;

		} else {

			String id = sha256(statement);

			if (statement.getContext() == null) {
				elasticsearchIdStatement = vf.createStatement(id, statement.getSubject(), statement.getPredicate(),
						statement.getPredicate(), statement.isInferred());
			} else {
				elasticsearchIdStatement = vf.createStatement(id, statement.getSubject(), statement.getPredicate(),
						statement.getPredicate(), statement.getContext(), statement.isInferred());
			}

		}

		if (deleteStatementBuffer.size() >= BUFFER_THRESHOLD) {
			flushRemoveStatementBuffer();
		}

		deleteStatementBuffer.add(elasticsearchIdStatement);

	}

	@Override
	public void addStatement(Collection<ExtensibleStatement> statements) {
		addStatementBuffer.addAll(statements);
		if (addStatementBuffer.size() >= BUFFER_THRESHOLD) {
			flushAddStatementBuffer();
		}
	}

	@Override
	synchronized public void clear(boolean inferred, Resource[] contexts) {

		DeleteByQueryRequest.Builder builder = new DeleteByQueryRequest.Builder();
		DeleteByQueryRequest build = builder.index(index)
				.query(getQuery(null, null, null, inferred, contexts))
				.conflicts(Conflicts.Proceed)
				.build();

		try {
			Long deleted = clientProvider.getClient().deleteByQuery(build).deleted();
		} catch (IOException e) {
			throw new SailException(e);
		}

	}

	@Override
	public void flushForCommit() {
		// no underlying store to flush to
	}

	@Override
	public CloseableIteration<? extends ExtensibleStatement> getStatements(Resource subject,
			IRI predicate,
			Value object, boolean inferred, Resource... context) {

		Query queryBuilder = getQuery(subject, predicate, object, inferred, context);

		return new LookAheadIteration<>() {

			final CloseableIteration<SearchHit> iterator = ElasticsearchHelper
					.getScrollingIterator(queryBuilder, clientProvider.getClient(), index, scrollTimeout);

			@Override
			protected ExtensibleStatement getNextElement() throws SailException {

				ExtensibleStatement next = null;

				while (next == null && iterator.hasNext()) {
					SearchHit nextSearchHit = iterator.next();

					Map<String, Object> sourceAsMap = nextSearchHit.getSourceAsMap();

					String id = nextSearchHit.getId();

					ExtensibleStatement statement = sourceToStatement(sourceAsMap, id, subject, predicate, object);

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

				return next;
			}

			@Override
			public void remove() throws SailException {

				throw new IllegalStateException("Does not support removing from iterator");

			}

			@Override
			protected void handleClose() throws SailException {
				super.handleClose();
				iterator.close();
			}

		};

	}

	private Query getQuery(Resource subject, IRI predicate, Value object, boolean inferred, Resource[] contexts) {

		BoolQuery.Builder mainQuery = new BoolQuery.Builder();

		if (subject != null) {
			mainQuery.must(b -> b.term(t -> t.field("subject").value(subject.stringValue())));

			if (subject instanceof IRI) {
				mainQuery.must(b -> b.term(t -> t.field("subject_IRI").value(true)));
			} else {
				mainQuery.must(b -> b.term(t -> t.field("subject_BNode").value(true)));
			}
		}

		if (predicate != null) {
			mainQuery.must(b -> b.term(t -> t.field("predicate").value(predicate.stringValue())));
		}

		if (object != null) {
			mainQuery.must(b -> b.term(t -> t.field("object_Hash").value(object.stringValue().hashCode())));

			if (object instanceof IRI) {
				mainQuery.must(b -> b.term(t -> t.field("object_IRI").value(true)));
			} else if (object instanceof BNode) {
				mainQuery.must(b -> b.term(t -> t.field("object_BNode").value(true)));
			} else {
				mainQuery.must(b -> b
						.term(t -> t.field("object_Datatype").value(((Literal) object).getDatatype().stringValue())));

				if (((Literal) object).getLanguage().isPresent()) {
					mainQuery.must(
							b -> b.term(t -> t.field("object_Lang").value(((Literal) object).getLanguage().get())));
				}
			}
		}

		if (contexts != null && contexts.length > 0) {

			for (Resource context : contexts) {
				if (context == null) {
					mainQuery.should(b -> b.bool(bb -> bb.mustNot(mb -> mb.exists(a -> a.field("context")))));
				} else if (context instanceof IRI) {
					mainQuery.should(b -> b.bool(bb -> {
						bb.must(mb -> mb.term(t -> t.field("context").value(context.stringValue())));
						bb.must(mb -> mb.term(t -> t.field("context_IRI").value(true)));
					}));
				} else { // BNode
					mainQuery.should(b -> b.bool(bb -> {
						bb.must(mb -> mb.term(t -> t.field("context").value(context.stringValue())));
						bb.must(mb -> mb.term(t -> t.field("context_BNode").value(true)));
					}));
				}
			}
		}

		mainQuery.must(b -> b.term(t -> t.field("inferred").value(inferred)));

		return mainQuery.build()._toQuery();
	}

	@Override
	public void flushForReading() {

		flushAddStatementBuffer();
		flushRemoveStatementBuffer();

		refreshIndex();
	}

	private void flushAddStatementBuffer() {

		Set<ExtensibleStatement> workingBuffer = null;

		try {
			synchronized (this) {
				if (addStatementBuffer.isEmpty()) {
					return;
				}
				workingBuffer = new HashSet<>(addStatementBuffer);
				addStatementBuffer = new HashSet<>(Math.min(addStatementBuffer.size(), BUFFER_THRESHOLD));
			}

			int failures = 0;

			do {
				BulkRequestBuilder bulkRequest = clientProvider.getClient().prepareBulk();

				workingBuffer

						.stream()
						.parallel()
						.map(statement -> {

							Map<String, Object> jsonMap = statementToJsonMap(statement);

							return new BuilderAndSha(sha256(statement), jsonMap);

						})
						.collect(Collectors.toList())
						.forEach(builderAndSha -> {

							bulkRequest.add(clientProvider.getClient()
									.prepareIndex(index, ELASTICSEARCH_TYPE, builderAndSha.getSha256())
									.setSource(builderAndSha.getMap())
									.setOpType(DocWriteRequest.OpType.CREATE));

						});

				BulkResponse bulkResponse = bulkRequest.get();
				if (bulkResponse.hasFailures()) {

					List<BulkItemResponse> bulkItemResponses = getBulkItemResponses(bulkResponse);

					boolean onlyVersionConflicts = bulkItemResponses.stream()
							.filter(BulkItemResponse::isFailed)
							.allMatch(resp -> resp.getFailure().getCause() instanceof VersionConflictEngineException);
					if (onlyVersionConflicts) {
						// probably trying to add duplicates, or we have a hash conflict

						Set<String> failedIDs = bulkItemResponses.stream()
								.filter(BulkItemResponse::isFailed)
								.map(BulkItemResponse::getId)
								.collect(Collectors.toSet());

						// clean up addedStatements
						workingBuffer = workingBuffer.stream()
								.filter(statement -> failedIDs.contains(sha256(statement))) // we only want to retry
								// failed
								// statements
								// filter out duplicates
								.filter(statement -> {

									String sha256 = sha256(statement);
									ExtensibleStatement statementById = getStatementById(sha256);

									return !statement.equals(statementById);
								})

								// now we only have conflicts
								.map(statement -> {
									// TODO handle conflict. Probably by doing something to change to id, mark it as a
									// conflict. Store all the conflicts in memory, to check against and refresh them
									// from
									// disc when we boot

									return statement;

								})
								.collect(Collectors.toSet());

						if (!workingBuffer.isEmpty()) {
							failures++;
						}

					} else {
						failures++;

						logger.info("Elasticsearch has failures when adding data, retrying. Message: {}",
								bulkResponse.buildFailureMessage());

					}

					if (failures > 10) {
						throw new RuntimeException("Elasticsearch has failed " + failures
								+ " times when adding data, retrying. Message: "
								+ bulkResponse.buildFailureMessage());
					}

					try {
						Thread.sleep(failures * 100);
					} catch (InterruptedException ignored) {
					}

				} else {
					failures = 0;
				}

			} while (failures > 0);

			logger.debug("Added {} statements", workingBuffer.size());

			workingBuffer = Collections.emptySet();

		} finally {
			if (workingBuffer != null && !workingBuffer.isEmpty()) {
				synchronized (this) {
					addStatementBuffer.addAll(workingBuffer);
				}
			}
		}
	}

	private Map<String, Object> statementToJsonMap(ExtensibleStatement statement) {
		Map<String, Object> jsonMap = new HashMap<>();

		jsonMap.put("subject", statement.getSubject().stringValue());
		jsonMap.put("predicate", statement.getPredicate().stringValue());
		jsonMap.put("object", statement.getObject().stringValue());
		jsonMap.put("object_Hash", statement.getObject().stringValue().hashCode());
		jsonMap.put("inferred", statement.isInferred());

		Resource context = statement.getContext();

		if (context != null) {
			jsonMap.put("context", context.stringValue());

			if (context instanceof IRI) {
				jsonMap.put("context_IRI", true);
			} else {
				jsonMap.put("context_BNode", true);
			}
		}

		if (statement.getSubject() instanceof IRI) {
			jsonMap.put("subject_IRI", true);
		} else {
			jsonMap.put("subject_BNode", true);
		}

		if (statement.getObject() instanceof IRI) {
			jsonMap.put("object_IRI", true);
		} else if (statement.getObject() instanceof BNode) {
			jsonMap.put("object_BNode", true);
		} else {
			jsonMap.put("object_Datatype",
					((Literal) statement.getObject()).getDatatype().stringValue());
			if (((Literal) statement.getObject()).getLanguage().isPresent()) {
				jsonMap.put("object_Lang", ((Literal) statement.getObject()).getLanguage().get());

			}
		}
		return jsonMap;
	}

	private ExtensibleStatement getStatementById(String sha256) {
		Map<String, Object> source = clientProvider.getClient()
				.prepareGet(index, ELASTICSEARCH_TYPE, sha256)
				.get()
				.getSource();

		return sourceToStatement(source, sha256, null, null, null);

	}

	private List<BulkItemResponse> getBulkItemResponses(BulkResponse bulkResponse) {
		return Arrays.asList(bulkResponse.getItems());
	}

	synchronized private void flushRemoveStatementBuffer() {

		if (deleteStatementBuffer.isEmpty()) {
			return;
		}

		BulkRequestBuilder bulkRequest = clientProvider.getClient().prepareBulk();

		int failures = 0;

		do {

			deleteStatementBuffer.forEach(statement -> {

				bulkRequest.add(clientProvider.getClient()
						.prepareDelete(index, ELASTICSEARCH_TYPE, statement.getElasticsearchId()));

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

		logger.debug("Removed {} statements", deleteStatementBuffer.size());

		deleteStatementBuffer = Collections.synchronizedSet(new HashSet<>(BUFFER_THRESHOLD));

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

		refreshIndex();

	}

	private void refreshIndex() {
		clientProvider.getClient().admin().indices().prepareRefresh(index).get();
	}

	void setElasticsearchScrollTimeout(int timeout) {
		this.scrollTimeout = timeout;
	}

	@Override
	public synchronized boolean removeStatementsByQuery(Resource subj, IRI pred, Value obj,
			boolean inferred, Resource[] contexts) {

		// delete single statement
		if (subj != null && pred != null && obj != null && contexts.length == 1) {
			ExtensibleStatement statement;

			if (contexts[0] == null) {
				statement = vf.createStatement(subj, pred, obj, inferred);
			} else {
				statement = vf.createStatement(subj, pred, obj, contexts[0], inferred);
			}

			String id = sha256(statement);

			boolean exists = clientProvider.getClient().prepareGet(index, ELASTICSEARCH_TYPE, id).get().isExists();
			if (exists) {

				if (contexts[0] == null) {
					statement = vf.createStatement(id, subj, pred, obj, inferred);
				} else {
					statement = vf.createStatement(id, subj, pred, obj, contexts[0], inferred);
				}

				// don't actually delete it just yet, we can just call remove and it will be removed at some point
				// before or during flush
				removeStatement(statement);
			}
			return exists;

		}

		// Elasticsearch delete by query is slow. It's still faster when deleting a lot of data. We assume that
		// getStatement and bulk delete is faster up to 1000 statements. If there are more, then we instead use
		// elasticsearch delete by query.
		try (CloseableIteration<? extends ExtensibleStatement> statements = getStatements(subj, pred,
				obj,
				inferred, contexts)) {
			List<ExtensibleStatement> statementsToDelete = new ArrayList<>();
			for (int i = 0; i < 1000 && statements.hasNext(); i++) {
				statementsToDelete.add(statements.next());
			}

			if (!statements.hasNext()) {
				for (ExtensibleStatement statement : statementsToDelete) {
					removeStatement(statement);
				}

				return !statementsToDelete.isEmpty();
			}

		}

		BulkByScrollResponse response = new DeleteByQueryRequestBuilder(clientProvider.getClient(),
				DeleteByQueryAction.INSTANCE)
				.filter(getQuery(subj, pred, obj, inferred, contexts))
				.source(index)
				.abortOnVersionConflict(false)
				.get();

		long deleted = response.getDeleted();
		return deleted > 0;

	}

	String sha256(ExtensibleStatement statement) {

		StringBuilder stringBuilder = new StringBuilder();

		Stream
				.of(statement.getSubject(), statement.getPredicate(), statement.getObject(), statement.getContext(),
						statement.isInferred())
				.forEachOrdered(o -> {

					if (o instanceof IRI) {
						stringBuilder.append("IRI<").append(o.toString()).append(">");
					} else if (o instanceof BNode) {
						stringBuilder.append("Bnode<").append(o.toString()).append(">");
					} else if (o instanceof Literal) {
						stringBuilder.append("Literal<").append(o.toString()).append(">");
					} else if (o instanceof Boolean) {
						stringBuilder.append("Boolean<").append(o).append(">");
					} else if (o == null) {
						stringBuilder.append("Null<>");
					} else {
						throw new IllegalStateException();
					}

				});

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			byte[] hash = digest.digest(stringBuilder.toString().getBytes(StandardCharsets.UTF_8));

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

	private static ExtensibleStatement sourceToStatement(Map<String, Object> sourceAsMap, String id, Resource subject,
			IRI predicate, Value object) {

		Resource subjectRes = subject;
		if (subjectRes == null && sourceAsMap.containsKey("subject_IRI")) {
			subjectRes = vf.createIRI((String) sourceAsMap.get("subject"));
		} else if (subjectRes == null) {
			subjectRes = vf.createBNode((String) sourceAsMap.get("subject"));
		}

		IRI predicateRes = predicate != null ? predicate : vf.createIRI((String) sourceAsMap.get("predicate"));

		Value objectRes;

		String objectString = (String) sourceAsMap.get("object");

		if (sourceAsMap.containsKey("object_IRI")) {
			objectRes = vf.createIRI(objectString);
		} else if (sourceAsMap.containsKey("object_BNode")) {
			objectRes = vf.createBNode(objectString);
		} else {
			if (sourceAsMap.containsKey("object_Lang")) {
				objectRes = vf.createLiteral(objectString, (String) sourceAsMap.get("object_Lang"));

			} else {
				objectRes = vf.createLiteral(objectString,
						vf.createIRI((String) sourceAsMap.get("object_Datatype")));
			}
		}

		Resource contextRes = null;
		if (sourceAsMap.containsKey("context_IRI")) {
			contextRes = vf.createIRI((String) sourceAsMap.get("context"));
		} else if (sourceAsMap.containsKey("context_BNode")) {
			contextRes = vf.createBNode((String) sourceAsMap.get("context"));
		}

		Object inferredNullable = sourceAsMap.get("inferred");

		boolean inferred = false;
		if (inferredNullable != null) {
			inferred = ((Boolean) inferredNullable);
		}

		if (contextRes != null) {
			return vf.createStatement(id, subjectRes, predicateRes, objectRes, contextRes, inferred);
		} else {
			return vf.createStatement(id, subjectRes, predicateRes, objectRes, inferred);
		}
	}

	public void setElasticsearchBulkSize(int size) {
		this.BUFFER_THRESHOLD = size;
	}

	@Override
	public long getEstimatedSize() {
		Client client = clientProvider.getClient();

		IndicesAdminClient indices = client.admin().indices();
		IndicesStatsResponse indicesStatsResponse = indices.prepareStats(index).get();

		return indicesStatsResponse.getTotal().docs.getCount();

	}
}
