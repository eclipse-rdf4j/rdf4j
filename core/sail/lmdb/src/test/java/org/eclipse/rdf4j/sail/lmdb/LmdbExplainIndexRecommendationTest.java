package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LmdbExplainIndexRecommendationTest {

	private static final String PERSON_QUERY = "PREFIX foaf: <" + FOAF.NAMESPACE + ">\n"
			+ "SELECT ?person WHERE { ?person a foaf:Person . }";

	private static final String NAMED_GRAPH_QUERY = "PREFIX foaf: <" + FOAF.NAMESPACE + ">\n"
			+ "PREFIX ex: <http://example.com/>\n"
			+ "SELECT ?country WHERE { GRAPH ex:namedGraph { ?country a ex:Country . } }";

	@TempDir
	File dataDir;

	@BeforeEach
	void resetRecommendations() {
		LmdbRecordIterator.resetIndexRecommendationTracker();
	}

	@AfterEach
	void cleanup() {
		LmdbRecordIterator.resetIndexRecommendationTracker();
	}

	@ParameterizedTest
	@MethodSource("allTripleIndexPermutations")
	void explainPlanCoversAllIndexPermutations(String index, boolean directLookup) {
		String plan = runExplainQuery(index, PERSON_QUERY, connection -> connection.add(
				connection.getValueFactory().createIRI("http://example.com/alice"), RDF.TYPE, FOAF.PERSON));

		if (directLookup) {
			assertThat(plan).contains("[index: " + index + "]");
			assertThat(plan).doesNotContain("(scan; consider");
		} else {
			assertThat(plan).contains("[index: " + index + " (scan; consider posc)]");
		}
	}

	@Test
	void tracksAllCandidateIndexesWhenRecommending() {
		String plan = runExplainQuery("psoc", PERSON_QUERY, connection -> connection.add(
				connection.getValueFactory().createIRI("http://example.com/alice"), RDF.TYPE, FOAF.PERSON));

		assertThat(plan).contains("[index: psoc (scan; consider posc)]");

		ConcurrentMap<String, LongAdder> tracked = LmdbRecordIterator.getRecommendedIndexTracker();
		assertThat(tracked.keySet()).containsExactlyInAnyOrder("posc", "pocs", "opsc", "opcs");
		assertThat(tracked.values()).allSatisfy(adder -> assertThat(adder.sum()).isEqualTo(1));
	}

	@Test
	void recommendationPrefersIndexesWithHigherDemand() {
		runExplainQuery("psoc", PERSON_QUERY, connection -> connection.add(
				connection.getValueFactory().createIRI("http://example.com/alice"), RDF.TYPE, FOAF.PERSON));

		String plan = runExplainQuery("cspo", NAMED_GRAPH_QUERY, connection -> {
			IRI namedGraph = connection.getValueFactory().createIRI("http://example.com/namedGraph");
			connection.add(connection.getValueFactory().createIRI("http://example.com/country"), RDF.TYPE,
					connection.getValueFactory().createIRI("http://example.com/Country"), namedGraph);
		});

		assertThat(plan).contains("(scan; consider opcs)");
	}

	private static Stream<Arguments> allTripleIndexPermutations() {
		List<String> permutations = new ArrayList<>();
		permute("spoc".toCharArray(), new boolean[4], new StringBuilder(), permutations);
		Set<String> directIndexes = Set.of("posc", "pocs", "opsc", "opcs");
		return permutations.stream().map(index -> Arguments.of(index, directIndexes.contains(index)));
	}

	private static void permute(char[] chars, boolean[] used, StringBuilder current, List<String> result) {
		if (current.length() == chars.length) {
			result.add(current.toString());
			return;
		}

		for (int i = 0; i < chars.length; i++) {
			if (!used[i]) {
				used[i] = true;
				current.append(chars[i]);
				permute(chars, used, current, result);
				current.deleteCharAt(current.length() - 1);
				used[i] = false;
			}
		}
	}

	private String runExplainQuery(String index, String query, RepositoryConnectionConsumer consumer) {
		File storeDir = new File(dataDir, index + "-" + query.hashCode() + "-" + System.nanoTime());
		storeDir.mkdirs();

		SailRepository repository = new SailRepository(new LmdbStore(storeDir, new LmdbStoreConfig(index)));
		repository.init();

		try (SailRepositoryConnection connection = repository.getConnection()) {
			consumer.accept(connection);
			Explanation explanation = connection.prepareTupleQuery(query).explain(Explanation.Level.Optimized);
			return explanation.toString();
		} catch (RepositoryException e) {
			throw new RuntimeException(e);
		} finally {
			repository.shutDown();
		}
	}

	@FunctionalInterface
	private interface RepositoryConnectionConsumer {
		void accept(SailRepositoryConnection connection) throws RepositoryException;
	}
}
