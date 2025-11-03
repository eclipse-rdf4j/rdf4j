package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmdbExplainIndexRecommendationTest {

	@TempDir
	File dataDir;

	private SailRepository repository;

	@BeforeEach
	void setUp() throws SailException {
		Sail sail = new LmdbStore(dataDir, new LmdbStoreConfig("psoc"));
		repository = new SailRepository(sail);
		repository.init();
	}

	@AfterEach
	void tearDown() {
		if (repository != null) {
			repository.shutDown();
		}
	}

	@Test
	void recommendsBetterIndexInExplainPlan() {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.add(connection.getValueFactory().createIRI("http://example.com/alice"), RDF.TYPE, FOAF.PERSON);

			Explanation explanation = connection
					.prepareTupleQuery("PREFIX foaf: <" + FOAF.NAMESPACE + ">\n" +
							"SELECT ?person WHERE { ?person a foaf:Person . }")
					.explain(Explanation.Level.Optimized);

			String plan = explanation.toString();

			assertThat(plan).contains("[index: psoc (scan; consider posc)]");
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		}
	}
}
