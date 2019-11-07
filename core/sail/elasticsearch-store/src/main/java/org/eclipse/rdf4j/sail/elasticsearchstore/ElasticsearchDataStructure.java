package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.SailException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
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
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

public class ElasticsearchDataStructure extends DataStructureInterface {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchDataStructure.class);

	private static final String STATEMENT = "statement";
	private final String index;
	private final String hostname;
	private final int port;

	public ElasticsearchDataStructure(String hostname, int port, String index) {
		super();
		this.hostname = hostname;
		this.port = port;
		this.index = index;

		createIndex();

	}

	private void createIndex() {

		CreateIndexRequest request = new CreateIndexRequest(index);

		try {
			String mapping = IOUtils.toString(ElasticsearchDataStructure.class.getClassLoader()
					.getResourceAsStream("elasticsearchStoreMapping.json"), StandardCharsets.UTF_8);
			request.mapping(STATEMENT,
					mapping,
					XContentType.JSON);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		try (Client client = getClient()) {
			CreateIndexResponse createIndexResponse = client.admin().indices().create(request).actionGet();
		}

	}

	@Override
	public void addStatement(Statement statement) {

		XContentBuilder builder;

		try {
			builder = jsonBuilder()
					.startObject()
					.field("subject", statement.getSubject().stringValue())
					.field("predicate", statement.getPredicate().toString())
					.field("object", statement.getObject().toString());
			Resource context = statement.getContext();

			if (context != null) {
				builder.field("context", context.toString());
			}

			if (statement.getSubject() instanceof IRI) {
				builder.field("subject_IRI", true);
			} else {
				builder.field("subject_BNODE", true);
			}

			builder.endObject();

		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

		IndexResponse response;
		try (Client client = getClient()) {

			response = client.prepareIndex(index, STATEMENT)
					.setSource(builder)
					.get();

			System.out.println(response.toString());

		}

	}

	@Override
	public void removeStatement(Statement statement) {

	}

	@Override
	public CloseableIteration<? extends Statement, SailException> getStatements(Resource subject, IRI predicate,
			Value object, Resource... context) {

		try (Client client = getClient()) {

			boolean matchAll = true;

			BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

			if (subject != null) {
				matchAll = false;
				boolQueryBuilder.must(QueryBuilders.termQuery("subject", subject.stringValue()));
				if (subject instanceof IRI) {
					boolQueryBuilder.must(QueryBuilders.termQuery("subject_IRI", true));
				} else {
					boolQueryBuilder.must(QueryBuilders.termQuery("subject_BNODE", true));
				}
			}

			if (predicate != null) {
				matchAll = false;
				boolQueryBuilder.must(QueryBuilders.termQuery("predicate", predicate.toString()));
			}

			if (object != null) {
				matchAll = false;
				boolQueryBuilder.must(QueryBuilders.termQuery("object", object.toString()));
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
					IRI objectRes = vf.createIRI(sourceAsMap.get("object").toString());

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
	}

	@Override
	public void flush() {

		try (Client client = getClient()) {
			client.admin()
					.indices()
					.prepareRefresh(index)
					.get();
		}

	}

	private Client getClient() {

		LocalDateTime tenMinFromNow = LocalDateTime.now().plusMinutes(10);

		logger.info("Waiting for Elasticsearch to start");

		while (true) {
			if (LocalDateTime.now().isAfter(tenMinFromNow)) {
				logger.error("Could not connect to Elasticsearch after 10 minutes of trying!");

				throw new RuntimeException("Could not connect to Elasticsearch after 10 minutes of trying!");

			}
			try {
				Settings settings = Settings.builder().put("cluster.name", "cluster1").build();
				TransportClient client = new PreBuiltTransportClient(settings);
				client.addTransportAddress(new TransportAddress(InetAddress.getByName(hostname), port));

				ClusterHealthRequest request = new ClusterHealthRequest();

				ClusterHealthResponse clusterHealthResponse = client.admin().cluster().health(request).actionGet();

				ClusterHealthStatus status = clusterHealthResponse.getStatus();

				logger.info("Cluster status: {}", status.name());

				if (status.equals(ClusterHealthStatus.GREEN) || status.equals(ClusterHealthStatus.YELLOW)) {
					logger.info("Elasticsearch started!");
					return client;
				} else {
					client.close();
				}
			} catch (Throwable e) {
				logger.info("Unable to connect to elasticsearch cluster due to {}", e.getClass().getSimpleName());
				e.printStackTrace();
			}

			logger.info(".");

			try {
				Thread.sleep(1000);
			} catch (InterruptedException ignored) {

			}
		}

	}

}
