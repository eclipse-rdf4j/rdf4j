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
package org.eclipse.rdf4j.sail.s3;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.s3.config.S3StoreConfig;
import org.eclipse.rdf4j.sail.s3.storage.S3ObjectStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;

/**
 * Integration test for S3 persistence using a real MinIO container via Testcontainers. Suffixed IT so it runs with
 * {@code mvn verify} (Failsafe), not {@code mvn test}.
 */
@Testcontainers
class S3PersistenceMinioIT {

	private static final String BUCKET = "test-bucket";
	private static final String ACCESS_KEY = "minioadmin";
	private static final String SECRET_KEY = "minioadmin";
	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@Container
	static final GenericContainer<?> MINIO = new GenericContainer<>("minio/minio:latest")
			.withExposedPorts(9000)
			.withEnv("MINIO_ROOT_USER", ACCESS_KEY)
			.withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
			.withCommand("server", "/data");

	private static String endpoint;

	@BeforeAll
	static void createBucket() throws Exception {
		endpoint = "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
		MinioClient client = MinioClient.builder()
				.endpoint(endpoint)
				.credentials(ACCESS_KEY, SECRET_KEY)
				.build();
		if (!client.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())) {
			client.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build());
		}
	}

	private S3ObjectStore createStore(String prefix) {
		return new S3ObjectStore(BUCKET, endpoint, "us-east-1", prefix, ACCESS_KEY, SECRET_KEY, true);
	}

	@Test
	void writeFlushShutdownRestart() throws Exception {
		String prefix = "test-" + System.nanoTime() + "/";

		IRI s = VF.createIRI("http://example.org/s1");
		IRI p = VF.createIRI("http://example.org/p1");
		IRI o = VF.createIRI("http://example.org/o1");

		// Write and flush
		{
			S3ObjectStore objectStore = createStore(prefix);
			S3StoreConfig config = new S3StoreConfig();
			S3SailStore sailStore = new S3SailStore(config, objectStore);

			var source = sailStore.getExplicitSailSource();
			var sink = source.sink(org.eclipse.rdf4j.common.transaction.IsolationLevels.NONE);
			sink.approve(s, p, o, null);
			sink.flush();
			sailStore.close();
		}

		// Restart and verify
		{
			S3ObjectStore objectStore = createStore(prefix);
			S3StoreConfig config = new S3StoreConfig();
			S3SailStore sailStore = new S3SailStore(config, objectStore);

			var source = sailStore.getExplicitSailSource();
			var dataset = source.dataset(org.eclipse.rdf4j.common.transaction.IsolationLevels.NONE);

			CloseableIteration<? extends Statement> iter = dataset.getStatements(null, null, null);
			assertTrue(iter.hasNext());
			Statement stmt = iter.next();
			assertEquals(s.stringValue(), stmt.getSubject().stringValue());
			assertEquals(p.stringValue(), stmt.getPredicate().stringValue());
			assertEquals(o.stringValue(), stmt.getObject().stringValue());
			assertFalse(iter.hasNext());

			iter.close();
			dataset.close();
			sailStore.close();
		}
	}
}
