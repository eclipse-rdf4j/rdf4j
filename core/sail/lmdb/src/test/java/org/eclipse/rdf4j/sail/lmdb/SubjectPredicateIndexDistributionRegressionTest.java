/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SubjectPredicateIndexDistributionRegressionTest {

	private static final String DATASET_RESOURCE = "benchmarkFiles/datagovbe-valid.ttl";
	private static final IRI DATASET_IRI = SimpleValueFactory.getInstance()
			.createIRI("http://data.gov.be/dataset/brussels/3fded591-0cda-485f-97e7-3b982c7ff34b");
	private static final String FILTERED_MULTIPLE_SUBSELECT_QUERY = String.join("\n",
			"PREFIX ex: <http://example.com/ns#>",
			"PREFIX owl: <http://www.w3.org/2002/07/owl#>",
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>",
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
			"PREFIX sh: <http://www.w3.org/ns/shacl#>",
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>",
			"PREFIX dcat: <http://www.w3.org/ns/dcat#>",
			"PREFIX dc: <http://purl.org/dc/terms/>",
			"PREFIX skos:  <http://www.w3.org/2004/02/skos/core#>",
			"PREFIX foaf:  <http://xmlns.com/foaf/0.1/>",
			"PREFIX dct: <http://purl.org/dc/terms/>",
			"",
			"SELECT ?type1 ?type2 ?language2 ?mbox ?count ?identifier2 WHERE {",
			"  VALUES ?a { <" + DATASET_IRI + "> }",
			"  {",
			"    SELECT * WHERE {",
			"      ?a a ?type2.",
			"      ?b a ?type1.",
			"",
			"      ?b dcat:dataset ?a.",
			"",
			"      ?a dcat:distribution ?mbox.",
			"      ?a dct:language ?language.",
			"      FILTER (?type1 != ?type2)",
			"      ?a dct:identifier ?identifier.",
			"    }",
			"  }",
			"",
			"  {",
			"    SELECT DISTINCT ?a (COUNT(?dist) AS ?count) ?language2 WHERE {",
			"      ?a a ?type2.",
			"      ?a dcat:distribution ?dist.",
			"      ?a dct:language ?language2.",
			"    } GROUP BY ?a ?language2 HAVING (?count > 2)",
			"  }",
			"}");

	@Test
	void countLanguage(@TempDir Path tempDir) throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,ospc,psoc");
		SailRepository lmdbRepository = new SailRepository(new LmdbStore(tempDir.resolve("lmdb").toFile(), config));
		SailRepository memoryRepository = new SailRepository(new MemoryStore());

		lmdbRepository.init();
		memoryRepository.init();

		try (SailRepositoryConnection lmdbConn = lmdbRepository.getConnection();
				SailRepositoryConnection memoryConn = memoryRepository.getConnection()) {
			loadDataset(lmdbConn);
			loadDataset(memoryConn);

			long countl = QueryResults.count(lmdbConn.getStatements(DATASET_IRI, DCTERMS.LANGUAGE, null, true));
			long countm = QueryResults.count(memoryConn.getStatements(DATASET_IRI, DCTERMS.LANGUAGE, null, true));

			assertEquals(countl, countm);

			var eng = SimpleValueFactory.getInstance()
					.createIRI("http://publications.europa.eu/resource/authority/language/ENG");
			assertTrue(lmdbConn.hasStatement(DATASET_IRI, DCTERMS.LANGUAGE, eng, true),
					"LMDB store should contain the ENG language statement");
			assertTrue(memoryConn.hasStatement(DATASET_IRI, DCTERMS.LANGUAGE, eng, true),
					"Memory store should contain the ENG language statement");

			List<String> memoryRows = evaluateMultipleSubselect(memoryConn);
			List<String> lmdbRows = evaluateMultipleSubselect(lmdbConn);

			Collections.sort(memoryRows);
			Collections.sort(lmdbRows);

			assertEquals(memoryRows, lmdbRows);
		} finally {
			lmdbRepository.shutDown();
			memoryRepository.shutDown();
		}
	}

	@Test
	void countLanguageDifferentIndexes1(@TempDir Path tempDir) throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,ospc");
		SailRepository lmdbRepository = new SailRepository(new LmdbStore(tempDir.resolve("lmdb").toFile(), config));
		SailRepository memoryRepository = new SailRepository(new MemoryStore());

		lmdbRepository.init();
		memoryRepository.init();

		try (SailRepositoryConnection lmdbConn = lmdbRepository.getConnection();
				SailRepositoryConnection memoryConn = memoryRepository.getConnection()) {
			loadDataset(lmdbConn);
			loadDataset(memoryConn);

			long countl = QueryResults.count(lmdbConn.getStatements(DATASET_IRI, DCTERMS.LANGUAGE, null, true));
			long countm = QueryResults.count(memoryConn.getStatements(DATASET_IRI, DCTERMS.LANGUAGE, null, true));

			assertEquals(countl, countm);

			var eng = SimpleValueFactory.getInstance()
					.createIRI("http://publications.europa.eu/resource/authority/language/ENG");
			assertTrue(lmdbConn.hasStatement(DATASET_IRI, DCTERMS.LANGUAGE, eng, true),
					"LMDB store should contain the ENG language statement");
			assertTrue(memoryConn.hasStatement(DATASET_IRI, DCTERMS.LANGUAGE, eng, true),
					"Memory store should contain the ENG language statement");

			List<String> memoryRows = evaluateMultipleSubselect(memoryConn);
			List<String> lmdbRows = evaluateMultipleSubselect(lmdbConn);

			Collections.sort(memoryRows);
			Collections.sort(lmdbRows);

			assertEquals(memoryRows, lmdbRows);
		} finally {
			lmdbRepository.shutDown();
			memoryRepository.shutDown();
		}
	}

	@Test
	void countLanguageDifferentIndexes2(@TempDir Path tempDir) throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc");
		SailRepository lmdbRepository = new SailRepository(new LmdbStore(tempDir.resolve("lmdb").toFile(), config));
		SailRepository memoryRepository = new SailRepository(new MemoryStore());

		lmdbRepository.init();
		memoryRepository.init();

		try (SailRepositoryConnection lmdbConn = lmdbRepository.getConnection();
				SailRepositoryConnection memoryConn = memoryRepository.getConnection()) {
			loadDataset(lmdbConn);
			loadDataset(memoryConn);

			long countl = QueryResults.count(lmdbConn.getStatements(DATASET_IRI, DCTERMS.LANGUAGE, null, true));
			long countm = QueryResults.count(memoryConn.getStatements(DATASET_IRI, DCTERMS.LANGUAGE, null, true));

			assertEquals(countl, countm);

			var eng = SimpleValueFactory.getInstance()
					.createIRI("http://publications.europa.eu/resource/authority/language/ENG");
			assertTrue(lmdbConn.hasStatement(DATASET_IRI, DCTERMS.LANGUAGE, eng, true),
					"LMDB store should contain the ENG language statement");
			assertTrue(memoryConn.hasStatement(DATASET_IRI, DCTERMS.LANGUAGE, eng, true),
					"Memory store should contain the ENG language statement");

			List<String> memoryRows = evaluateMultipleSubselect(memoryConn);
			List<String> lmdbRows = evaluateMultipleSubselect(lmdbConn);

			Collections.sort(memoryRows);
			Collections.sort(lmdbRows);

			assertEquals(memoryRows, lmdbRows);
		} finally {
			lmdbRepository.shutDown();
			memoryRepository.shutDown();
		}
	}

	@Test
	void countLanguageDifferentIndexes3(@TempDir Path tempDir) throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,psoc");
		SailRepository lmdbRepository = new SailRepository(new LmdbStore(tempDir.resolve("lmdb").toFile(), config));
		SailRepository memoryRepository = new SailRepository(new MemoryStore());

		lmdbRepository.init();
		memoryRepository.init();

		try (SailRepositoryConnection lmdbConn = lmdbRepository.getConnection();
				SailRepositoryConnection memoryConn = memoryRepository.getConnection()) {
			loadDataset(lmdbConn);
			loadDataset(memoryConn);

			long countl = QueryResults.count(lmdbConn.getStatements(DATASET_IRI, DCTERMS.LANGUAGE, null, true));
			long countm = QueryResults.count(memoryConn.getStatements(DATASET_IRI, DCTERMS.LANGUAGE, null, true));

			assertEquals(countl, countm);

			var eng = SimpleValueFactory.getInstance()
					.createIRI("http://publications.europa.eu/resource/authority/language/ENG");
			assertTrue(lmdbConn.hasStatement(DATASET_IRI, DCTERMS.LANGUAGE, eng, true),
					"LMDB store should contain the ENG language statement");
			assertTrue(memoryConn.hasStatement(DATASET_IRI, DCTERMS.LANGUAGE, eng, true),
					"Memory store should contain the ENG language statement");

			List<String> memoryRows = evaluateMultipleSubselect(memoryConn);
			List<String> lmdbRows = evaluateMultipleSubselect(lmdbConn);

			Collections.sort(memoryRows);
			Collections.sort(lmdbRows);

			assertEquals(memoryRows, lmdbRows);
		} finally {
			lmdbRepository.shutDown();
			memoryRepository.shutDown();
		}
	}

	@Test
	void countLanguageDifferentIndexes4(@TempDir Path tempDir) throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("ospc,psoc");
		SailRepository lmdbRepository = new SailRepository(new LmdbStore(tempDir.resolve("lmdb").toFile(), config));
		SailRepository memoryRepository = new SailRepository(new MemoryStore());

		lmdbRepository.init();
		memoryRepository.init();

		try (SailRepositoryConnection lmdbConn = lmdbRepository.getConnection();
				SailRepositoryConnection memoryConn = memoryRepository.getConnection()) {
			loadDataset(lmdbConn);
			loadDataset(memoryConn);

			long countl = QueryResults.count(lmdbConn.getStatements(DATASET_IRI, DCTERMS.LANGUAGE, null, true));
			long countm = QueryResults.count(memoryConn.getStatements(DATASET_IRI, DCTERMS.LANGUAGE, null, true));

			assertEquals(countl, countm);

			var eng = SimpleValueFactory.getInstance()
					.createIRI("http://publications.europa.eu/resource/authority/language/ENG");
			assertTrue(lmdbConn.hasStatement(DATASET_IRI, DCTERMS.LANGUAGE, eng, true),
					"LMDB store should contain the ENG language statement");
			assertTrue(memoryConn.hasStatement(DATASET_IRI, DCTERMS.LANGUAGE, eng, true),
					"Memory store should contain the ENG language statement");

			List<String> memoryRows = evaluateMultipleSubselect(memoryConn);
			List<String> lmdbRows = evaluateMultipleSubselect(lmdbConn);

			Collections.sort(memoryRows);
			Collections.sort(lmdbRows);

			assertEquals(memoryRows, lmdbRows);
		} finally {
			lmdbRepository.shutDown();
			memoryRepository.shutDown();
		}
	}

	@Test
	void lmdbExposesAllLanguagesForSimpleDataset(@TempDir Path tempDir) throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,ospc,psoc");
		SailRepository lmdbRepository = new SailRepository(
				new LmdbStore(tempDir.resolve("lmdb-simple").toFile(), config));
		SailRepository memoryRepository = new SailRepository(new MemoryStore());

		lmdbRepository.init();
		memoryRepository.init();

		try (SailRepositoryConnection lmdbConn = lmdbRepository.getConnection();
				SailRepositoryConnection memoryConn = memoryRepository.getConnection()) {
			loadSimpleDataset(lmdbConn);
			loadSimpleDataset(memoryConn);

			List<String> lmdbLanguages = evaluateLanguages(lmdbConn);
			List<String> memoryLanguages = evaluateLanguages(memoryConn);

			Collections.sort(lmdbLanguages);
			Collections.sort(memoryLanguages);

			assertEquals(memoryLanguages, lmdbLanguages);
		} finally {
			lmdbRepository.shutDown();
			memoryRepository.shutDown();
		}
	}

	private static void loadDataset(SailRepositoryConnection connection) throws IOException {
		connection.begin(IsolationLevels.NONE);
		try (InputStream data = getResource(DATASET_RESOURCE)) {
			connection.add(data, "", RDFFormat.TURTLE);
		}
		connection.commit();
	}

	private static InputStream getResource(String name) {
		InputStream stream = SubjectPredicateIndexDistributionRegressionTest.class.getClassLoader()
				.getResourceAsStream(name);
		if (stream == null) {
			throw new IllegalStateException("Missing resource: " + name);
		}
		return stream;
	}

	private static List<String> evaluateMultipleSubselect(SailRepositoryConnection connection) {
		try (var result = connection.prepareTupleQuery(FILTERED_MULTIPLE_SUBSELECT_QUERY).evaluate()) {
			return QueryResults.asList(result)
					.stream()
					.map(SubjectPredicateIndexDistributionRegressionTest::formatBindingSet)
					.collect(Collectors.toList());
		}
	}

	private static String formatBindingSet(BindingSet bindingSet) {
		return bindingSet.getBindingNames()
				.stream()
				.sorted()
				.map(name -> name + "=" + bindingSet.getValue(name))
				.collect(Collectors.joining(", "));
	}

	private static void loadSimpleDataset(SailRepositoryConnection connection) throws IOException {
		var vf = SimpleValueFactory.getInstance();
		var dataset = vf.createIRI("urn:dataset:simple");
		var catalog = vf.createIRI("urn:catalog:simple");
		connection.begin(IsolationLevels.NONE);
		connection.add(dataset, org.eclipse.rdf4j.model.vocabulary.RDF.TYPE, DCAT.DATASET);
		connection.add(catalog, org.eclipse.rdf4j.model.vocabulary.RDF.TYPE, DCAT.CATALOG);
		connection.add(catalog, DCAT.DATASET, dataset);
		connection.add(dataset, DCTERMS.LANGUAGE, vf.createIRI("urn:lang:ENG"));
		connection.add(dataset, DCTERMS.LANGUAGE, vf.createIRI("urn:lang:FRA"));
		connection.add(dataset, DCTERMS.LANGUAGE, vf.createIRI("urn:lang:NLD"));
		connection.add(dataset, DCAT.DISTRIBUTION, vf.createIRI("urn:dist:one"));
		connection.add(dataset, DCAT.DISTRIBUTION, vf.createIRI("urn:dist:two"));
		connection.add(dataset, DCAT.DISTRIBUTION, vf.createIRI("urn:dist:three"));
		connection.commit();
	}

	private static List<String> evaluateLanguages(SailRepositoryConnection connection) {
		try (var iter = connection.getStatements(SimpleValueFactory.getInstance().createIRI("urn:dataset:simple"),
				DCTERMS.LANGUAGE, null, true)) {
			return Iterations.stream(iter)
					.map(Statement::getObject)
					.map(Value::stringValue)
					.collect(Collectors.toList());
		}
	}
}
