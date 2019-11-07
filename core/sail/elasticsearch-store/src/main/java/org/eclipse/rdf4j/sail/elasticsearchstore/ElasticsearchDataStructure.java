package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

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
					.field("subject", statement.getSubject().toString())
					.field("predicate", statement.getPredicate().toString())
					.field("object", statement.getObject().toString());
			Resource context = statement.getContext();

			if (context != null) {
				builder.field("context", context.toString());
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
		return null;
	}

	@Override
	public void flush() {

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
