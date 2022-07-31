/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ShutdownTest {

	@Test
	public void shutdownWhileRunningValidation() throws InterruptedException {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.init();

		Future<Object> objectFuture = shaclSail.submitToExecutorService(getSleepingCallable());

		try {
			objectFuture.get(100, TimeUnit.MILLISECONDS);
		} catch (ExecutionException | TimeoutException ignored) {
		}

		Assertions.assertFalse(objectFuture.isDone());
		shaclSail.shutDown();
		Thread.sleep(100);
		Assertions.assertTrue(objectFuture.isDone(), "The thread should have be stopped after calling shutdown()");

	}

	@Test
	public void shutdownAndInitializeThreadPool() throws InterruptedException {

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		for (int j = 0; j < 3; j++) {
			shaclSail.init();

			Future<Object> objectFuture = shaclSail.submitToExecutorService(getSleepingCallable());

			try {
				objectFuture.get(100, TimeUnit.MILLISECONDS);
			} catch (ExecutionException | TimeoutException ignored) {
			}

			Assertions.assertFalse(objectFuture.isDone());
			shaclSail.shutDown();
			Thread.sleep(100);
			Assertions.assertTrue(objectFuture.isDone(), "The thread should have be stopped after calling shutdown()");

		}

	}

	@Test
	public void shutdownWithPersistence(@TempDir Path tempDir) throws IOException {
		IRI shapesGraph = Values.iri("http://example.com/ns#shapesGraph");
		IRI dataGraph = Values.iri("http://example.com/ns#dataGraph");

		SailRepository repository = new SailRepository(new ShaclSail(new NativeStore(tempDir.toFile())));

		try (RepositoryConnection connection = repository.getConnection()) {

			connection.begin();

			StringReader shaclRules = new StringReader(String.join("\n", "",
					"@prefix ex: <http://example.com/ns#> .",
					"@prefix sh: <http://www.w3.org/ns/shacl#> .",
					"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
					"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

					"<" + RDF4J.SHACL_SHAPE_GRAPH + "> { ",
					"	ex:PersonShape",
					"        a sh:NodeShape  ;",
					"        sh:targetClass ex:Person ;",
					"        sh:property [",
					"                sh:path ex:age ;",
					"                sh:datatype xsd:integer ;",
					"        ] .",
					"}",
					"ex:shapesGraph { ",
					"	ex:PersonShape",
					"        a sh:NodeShape  ;",
					"        sh:targetClass ex:Person ;",
					"        sh:property [",
					"                sh:path ex:age ;",
					"                sh:minCount 1 ;",
					"        ] .",
					" ex:dataGraph sh:shapesGraph ex:shapesGraph.",
					"}"

			));

			connection.add(shaclRules, "", RDFFormat.TRIG);

			((ShaclSail) repository.getSail()).setShapesGraphs(Set.of(RDF4J.SHACL_SHAPE_GRAPH, shapesGraph));

			connection.commit();

			{
				connection.begin();
				StringReader data = new StringReader(
						String.join("\n", "",
								"@prefix ex: <http://example.com/ns#> .",
								"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
								"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",

								"ex:pete a ex:Person ."
						)
				);

				connection.add(data, RDFFormat.TURTLE);

				connection.commit();
			}

			Assertions.assertThrows(RepositoryException.class, () -> {
				connection.begin();
				StringReader data = new StringReader(
						String.join("\n", "",
								"@prefix ex: <http://example.com/ns#> .",
								"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
								"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",

								"ex:pete a ex:Person ."
						)
				);

				connection.add(data, RDFFormat.TURTLE, dataGraph);

				connection.commit();
			});

		}

		repository.shutDown();
		repository = new SailRepository(new ShaclSail(new NativeStore(tempDir.toFile())));
		((ShaclSail) repository.getSail()).setShapesGraphs(Set.of(RDF4J.SHACL_SHAPE_GRAPH, shapesGraph));

		try (RepositoryConnection connection = repository.getConnection()) {

			Assertions.assertThrows(RepositoryException.class, () -> {
				connection.begin();
				StringReader data = new StringReader(
						String.join("\n", "",
								"@prefix ex: <http://example.com/ns#> .",
								"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
								"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",

								"ex:pete a ex:Person ."
						)
				);

				connection.add(data, RDFFormat.TURTLE, dataGraph);

				connection.commit();
			});

		}

		try (RepositoryConnection connection = repository.getConnection()) {

			Assertions.assertThrows(RepositoryException.class, () -> {
				connection.begin();
				StringReader data = new StringReader(
						String.join("\n", "",
								"@prefix ex: <http://example.com/ns#> .",
								"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
								"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",

								"ex:pete ex:age [] ."
						)
				);

				connection.add(data, RDFFormat.TURTLE);

				connection.commit();
			});

		}

		repository.shutDown();

	}

	@Test
	public void testThatGarbadgeCollectionWillShutdownTheThreadPool()
			throws InterruptedException, NoSuchFieldException, IllegalAccessException {

		ExecutorService executorServices = startShaclSailAndTask();

		Assertions.assertFalse(executorServices.isShutdown());

		for (int i = 0; i < 100; i++) {
			System.gc();
			if (executorServices.isShutdown()) {
				return;
			}
			System.out.println(i);
			Thread.sleep(100);
		}

		Assertions.fail("Executor service should have been shutdown due to GC");
	}

	private ExecutorService startShaclSailAndTask()
			throws InterruptedException, NoSuchFieldException, IllegalAccessException {
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.init();

		Future<Object> objectFuture = shaclSail.submitToExecutorService(getSleepingCallable());

		try {
			objectFuture.get(100, TimeUnit.MILLISECONDS);
		} catch (ExecutionException | TimeoutException ignored) {
		}

		Class<?> c = ShaclSail.class;
		Field field = c.getDeclaredField("executorService");
		field.setAccessible(true);

		return (ExecutorService) field.get(shaclSail);

	}

	private Callable<Object> getSleepingCallable() {
		return () -> {

			for (int i = 0; i < Integer.MAX_VALUE; i++) {
				Thread.sleep(10);
			}

			return null;
		};
	}

}
