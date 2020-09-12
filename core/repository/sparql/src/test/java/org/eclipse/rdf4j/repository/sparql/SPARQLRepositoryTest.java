package org.eclipse.rdf4j.repository.sparql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executors;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.http.client.HttpClientSessionManager;
import org.eclipse.rdf4j.http.client.RDF4JProtocolSession;
import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.junit.Before;
import org.junit.Test;

public class SPARQLRepositoryTest {

	String endpointUrl = "http://example.org/sparql";
	TupleQueryResultFormat customPreferred = TupleQueryResultFormat.CSV;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCustomPreferredTupleQueryResultFormat() {
		SPARQLRepository rep = new SPARQLRepository(endpointUrl);
		rep.setHttpClientSessionManager(new HttpClientSessionManager() {

			@Override
			public void shutDown() {
				// TODO Auto-generated method stub

			}

			@Override
			public HttpClient getHttpClient() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public SPARQLProtocolSession createSPARQLProtocolSession(String queryEndpointUrl,
					String updateEndpointUrl) {
				SPARQLProtocolSession session = new SPARQLProtocolSession(getHttpClient(),
						Executors.newSingleThreadExecutor());
				session.setPreferredTupleQueryResultFormat(customPreferred);
				return session;
			}

			@Override
			public RDF4JProtocolSession createRDF4JProtocolSession(String serverURL) {
				// TODO Auto-generated method stub
				return null;
			}
		});

		assertThat(rep.createHTTPClient().getPreferredTupleQueryResultFormat()).isEqualTo(customPreferred);
	}
}
