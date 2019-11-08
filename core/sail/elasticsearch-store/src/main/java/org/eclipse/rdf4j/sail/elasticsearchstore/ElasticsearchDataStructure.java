package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.SailException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

public class ElasticsearchDataStructure extends DataStructureInterface {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchDataStructure.class);

	private static final String ELASTICSEARCH_TYPE = "statement";
	private final String index;
	private final String hostname;
	private final int port;

	ElasticsearchDataStructure(String hostname, int port, String index) {
		super();
		this.hostname = hostname;
		this.port = port;
		this.index = index;
	}

	private void createIndex() {

		CreateIndexRequest request = new CreateIndexRequest(index);

		try {
			String mapping = IOUtils.toString(ElasticsearchDataStructure.class.getClassLoader()
					.getResourceAsStream("elasticsearchStoreMapping.json"), StandardCharsets.UTF_8);
			request.mapping(ELASTICSEARCH_TYPE,
					mapping,
					XContentType.JSON);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		try (Client client = getClient()) {

			boolean indexExistsAlready = client.admin()
					.indices()
					.exists(new IndicesExistsRequest(index))
					.actionGet()
					.isExists();
			if (!indexExistsAlready) {
				CreateIndexResponse createIndexResponse = client.admin().indices().create(request).actionGet();
			}
		}

	}

	@Override
	public void addStatement(Client client, Statement statement) {

		XContentBuilder builder;

		try {
			builder = jsonBuilder()
					.startObject()
					.field("subject", statement.getSubject().stringValue())
					.field("predicate", statement.getPredicate().stringValue())
					.field("object", Base64.getEncoder()
							.encodeToString(statement.getObject().stringValue().getBytes(StandardCharsets.UTF_8)));
			Resource context = statement.getContext();

			if (context != null) {
				builder.field("context", context.stringValue());
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
				builder.field("object_Datatype", ((Literal) statement.getObject()).getDatatype().stringValue());
				if (((Literal) statement.getObject()).getLanguage().isPresent()) {
					builder.field("object_Lang", ((Literal) statement.getObject()).getLanguage().get());

				}
			}

			builder.endObject();

		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		IndexResponse response;

		response = client.prepareIndex(index, ELASTICSEARCH_TYPE)
				.setSource(builder)
				.get();

		System.out.println(response.toString());

	}

	@Override
	public void removeStatement(Client client, Statement statement) {

	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Client client, Resource subject,
			IRI predicate,
			Value object, Resource... context) {

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
			boolQueryBuilder.must(QueryBuilders.termQuery("object",
					Base64.getEncoder().encodeToString(object.stringValue().getBytes(StandardCharsets.UTF_8))));
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

		if (context != null && context.length > 0) {
			matchAll = false;
			throw new IllegalStateException("Not implemented yet");
		}

		SearchRequestBuilder searchRequestBuilder = client.prepareSearch(index)
				.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
				.setFrom(0)
				.setSize(9999);

		if (matchAll) {
			searchRequestBuilder.setQuery(matchAllQuery());
		} else {
			searchRequestBuilder.setQuery(boolQueryBuilder);
		}

		logger.info(searchRequestBuilder.toString());

		SearchResponse response = searchRequestBuilder
				.get();

		SearchHits hits = response.getHits();

		if (hits.totalHits > 9999) {
			throw new IllegalStateException("Store only support getting 9999 statements currently");
		}

		return new CloseableIteration<Statement, SailException>() {

			Iterator<SearchHit> iterator = Arrays.asList(hits.getHits()).iterator();

			@Override
			public boolean hasNext() throws SailException {
				return iterator.hasNext();
			}

			@Override
			public Statement next() throws SailException {

				SearchHit next = iterator.next();
				Map<String, Object> sourceAsMap = next.getSourceAsMap();

				ValueFactory vf = SimpleValueFactory.getInstance();
				Resource subjectRes;
				if (sourceAsMap.containsKey("subject_IRI")) {
					subjectRes = vf.createIRI(sourceAsMap.get("subject").toString());
				} else {
					subjectRes = vf.createBNode(sourceAsMap.get("subject").toString());
				}

				IRI predicateRes = vf.createIRI(sourceAsMap.get("predicate").toString());

				Value objectRes;

				String objectString = new String(Base64.getDecoder().decode(sourceAsMap.get("object").toString()),
						StandardCharsets.UTF_8);

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

				return vf.createStatement(subjectRes, predicateRes, objectRes);
			}

			@Override
			public void remove() throws SailException {

				throw new IllegalStateException("Does not support removing from iterator");

			}

			@Override
			public void close() throws SailException {

			}
		};

	}

	@Override
	public void flush(Client client) {

		client.admin()
				.indices()
				.prepareRefresh(index)
				.get();

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

}
