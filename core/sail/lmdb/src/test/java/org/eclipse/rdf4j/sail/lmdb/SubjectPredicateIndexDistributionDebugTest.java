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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SubjectPredicateIndexDistributionDebugTest {

	private static final String DATASET_RESOURCE = "temp.nquad";
	private static final String DATASET_IRI = "http://data.gov.be/dataset/brussels/3fded591-0cda-485f-97e7-3b982c7ff34b";

	@Test
	void debugListLanguages(@TempDir Path tempDir) throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,ospc,psoc");
		SailRepository lmdbRepository = new SailRepository(new LmdbStore(tempDir.resolve("lmdb").toFile(), config));
		SailRepository memoryRepository = new SailRepository(new MemoryStore());

		lmdbRepository.init();
		memoryRepository.init();

		try (SailRepositoryConnection lmdbConn = lmdbRepository.getConnection();
				SailRepositoryConnection memoryConn = memoryRepository.getConnection()) {
			loadDataset(lmdbConn);
			loadDataset(memoryConn);

			List<String> lmdbLangs = getLanguages(lmdbConn);
			List<String> memLangs = getLanguages(memoryConn);

			System.out.println("LMDB languages:  " + lmdbLangs);
			System.out.println("MEM  languages:  " + memLangs);
		} finally {
			lmdbRepository.shutDown();
			memoryRepository.shutDown();
		}
	}

	@Test
	void debugFindFirstMismatch(@TempDir Path tempDir) throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,ospc,psoc");
		SailRepository lmdbRepository = new SailRepository(new LmdbStore(tempDir.resolve("lmdb").toFile(), config));
		SailRepository referenceRepository = new SailRepository(new MemoryStore());
		SailRepository memoryRepository = new SailRepository(new MemoryStore());

		lmdbRepository.init();
		referenceRepository.init();
		memoryRepository.init();

		try (SailRepositoryConnection memoryConn = referenceRepository.getConnection()) {
			loadDataset(memoryConn);
		}

		try (SailRepositoryConnection lmdbConn = lmdbRepository.getConnection();
				SailRepositoryConnection memoryConn = memoryRepository.getConnection();
				SailRepositoryConnection referenceConn = referenceRepository.getConnection()) {

			try (var statements = referenceConn.getStatements(null, null, null, true)) {
				int count = 0;
				for (Statement stmt : statements) {
					count++;
					lmdbConn.begin(IsolationLevels.NONE);
					memoryConn.begin(IsolationLevels.NONE);
					try {
						lmdbConn.add(stmt);
						memoryConn.add(stmt);
					} finally {
						lmdbConn.commit();
						memoryConn.commit();
					}

					long countLmdb = org.eclipse.rdf4j.query.QueryResults
							.count(lmdbConn.getStatements(SimpleValueFactory.getInstance().createIRI(DATASET_IRI),
									DCTERMS.LANGUAGE, null, true));
					long countMem = org.eclipse.rdf4j.query.QueryResults
							.count(memoryConn.getStatements(SimpleValueFactory.getInstance().createIRI(DATASET_IRI),
									DCTERMS.LANGUAGE, null, true));
					if (countLmdb != countMem) {
						System.out.println("Mismatch after adding statement #" + count + ": " + stmt);
						System.out.println("LMDB count=" + countLmdb + ", MEM count=" + countMem);
						break;
					}
				}
			}
		} finally {
			lmdbRepository.shutDown();
			memoryRepository.shutDown();
			referenceRepository.shutDown();
		}
	}

	@Test
	void debugListLanguagesPsocOnly(@TempDir Path tempDir) throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("psoc");
		SailRepository lmdbRepository = new SailRepository(new LmdbStore(tempDir.resolve("lmdb").toFile(), config));
		SailRepository memoryRepository = new SailRepository(new MemoryStore());

		lmdbRepository.init();
		memoryRepository.init();

		try (SailRepositoryConnection lmdbConn = lmdbRepository.getConnection();
				SailRepositoryConnection memoryConn = memoryRepository.getConnection()) {
			loadDataset(lmdbConn);
			loadDataset(memoryConn);

			List<String> lmdbLangs = getLanguages(lmdbConn);
			List<String> memLangs = getLanguages(memoryConn);

			System.out.println("[psoc] LMDB languages:  " + lmdbLangs);
			System.out.println("[psoc] MEM  languages:  " + memLangs);
		} finally {
			lmdbRepository.shutDown();
			memoryRepository.shutDown();
		}
	}

	@Test
	void debugListLanguagesNoDupsortRead(@TempDir Path tempDir) throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,ospc,psoc");
		config.setDupsortRead(false);
		SailRepository lmdbRepository = new SailRepository(new LmdbStore(tempDir.resolve("lmdb").toFile(), config));
		SailRepository memoryRepository = new SailRepository(new MemoryStore());

		lmdbRepository.init();
		memoryRepository.init();

		try (SailRepositoryConnection lmdbConn = lmdbRepository.getConnection();
				SailRepositoryConnection memoryConn = memoryRepository.getConnection()) {
			loadDataset(lmdbConn);
			loadDataset(memoryConn);

			List<String> lmdbLangs = getLanguages(lmdbConn);
			List<String> memLangs = getLanguages(memoryConn);

			System.out.println("[no-dupsort] LMDB languages:  " + lmdbLangs);
			System.out.println("[no-dupsort] MEM  languages:  " + memLangs);
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
		InputStream stream = SubjectPredicateIndexDistributionDebugTest.class
				.getClassLoader()
				.getResourceAsStream(name);
		assertNotNull(stream, "Missing resource: " + name);
		return stream;
	}

	private static List<String> getLanguages(SailRepositoryConnection connection) {
		var vf = SimpleValueFactory.getInstance();
		var dataset = vf.createIRI(DATASET_IRI);
		try (var iter = connection.getStatements(dataset, DCTERMS.LANGUAGE, null, true)) {
			return Iterations.stream(iter)
					.map(Statement::getObject)
					.map(Value::stringValue)
					.sorted()
					.collect(Collectors.toList());
		}
	}
}
