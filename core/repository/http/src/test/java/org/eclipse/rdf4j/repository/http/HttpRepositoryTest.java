package org.eclipse.rdf4j.repository.http;

import java.io.IOException;

import org.eclipse.rdf4j.http.client.RDF4JProtocolSession;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.Test;

public class HttpRepositoryTest {

	@Test
	public void test() throws IOException {

		HTTPRepository httpRepository = new HTTPRepository("http://localhost:8080/rdf4j-server", "1");

		System.out.println();

		try (RDF4JProtocolSession httpClient = httpRepository.createHTTPClient()) {
			DynamicModel config = new DynamicModelFactory().createEmptyModel();

			httpClient.getRepositoryConfig(new StatementCollector() {
				@Override
				public void handleStatement(Statement st) {
					config.add(st);
				}
			});
			SimpleValueFactory vf = SimpleValueFactory.getInstance();

			Value next = config
					.filter(null, vf.createIRI("http://www.openrdf.org/config/repository/sail#sailImpl"), null)
					.objects()
					.iterator()
					.next();

			config.remove(null, vf.createIRI("http://rdf4j.org/config/sail/shacl#validationEnabled"), null);
			config.add((Resource) next, vf.createIRI("http://rdf4j.org/config/sail/shacl#validationEnabled"),
					BooleanLiteral.FALSE);

			httpClient.updateRepository(new RepositoryConfig());

		}

		httpRepository.shutDown();

	}

}
