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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MultipleSubselectRegressionTest {

	private static final String DATASET_RESOURCE = "benchmarkFiles/datagovbe-valid.ttl";
	private static final String DATASET_IRI = "http://data.gov.be/dataset/brussels/3fded591-0cda-485f-97e7-3b982c7ff34b";
	private static final String MULTIPLE_SUB_SELECT_QUERY;

	static {
		try (InputStream query = getResource(MultipleSubselectRegressionTest.class,
				"benchmarkFiles/multiple-sub-select.qr")) {
			MULTIPLE_SUB_SELECT_QUERY = IOUtils.toString(query, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@Test
	void lmdbMatchesMemoryForMultipleSubSelect(@TempDir Path tempDir) throws Exception {
		LmdbStoreConfig config = new LmdbStoreConfig("spoc,ospc,psoc");
		SailRepository lmdbRepository = new SailRepository(new LmdbStore(tempDir.toFile(), config));
		SailRepository memoryRepository = new SailRepository(new MemoryStore());

		lmdbRepository.init();
		memoryRepository.init();

		try (SailRepositoryConnection lmdbConn = lmdbRepository.getConnection();
				SailRepositoryConnection memoryConn = memoryRepository.getConnection()) {
			loadDataset(lmdbConn);
			loadDataset(memoryConn);

			assertEquals(evaluateMultipleSubselectCount(memoryConn), evaluateMultipleSubselectCount(lmdbConn));
			assertEquals(evaluateDatasetLanguages(memoryConn), evaluateDatasetLanguages(lmdbConn));
		} finally {
			lmdbRepository.shutDown();
			memoryRepository.shutDown();
		}
	}

	private static void loadDataset(SailRepositoryConnection connection) throws IOException {
		connection.begin(IsolationLevels.NONE);
		connection.add(getResource(MultipleSubselectRegressionTest.class, DATASET_RESOURCE), "", RDFFormat.TURTLE);
		connection.commit();
	}

	private static long evaluateMultipleSubselectCount(SailRepositoryConnection connection) {
		return connection
				.prepareTupleQuery(MULTIPLE_SUB_SELECT_QUERY)
				.evaluate()
				.stream()
				.count();
	}

	private static List<String> evaluateDatasetLanguages(SailRepositoryConnection connection) {
		String query = String.join("\n",
				"PREFIX dct: <http://purl.org/dc/terms/>",
				"SELECT ?lang WHERE {",
				"  <" + DATASET_IRI + "> dct:language ?lang .",
				"}");

		try (var result = connection.prepareTupleQuery(query).evaluate()) {
			return result
					.stream()
					.map(bs -> bs.getValue("lang").stringValue())
					.sorted()
					.collect(Collectors.toList());
		}
	}

	private static InputStream getResource(Class<?> anchor, String resourceName) {
		InputStream stream = anchor.getClassLoader().getResourceAsStream(resourceName);
		if (stream == null) {
			throw new IllegalStateException("Missing resource: " + resourceName);
		}
		return stream;
	}
}
