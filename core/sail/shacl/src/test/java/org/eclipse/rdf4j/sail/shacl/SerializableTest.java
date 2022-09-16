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

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Isolated
public class SerializableTest {

	@Test
	public void testMaxCountSnapshot() throws IOException, InterruptedException {
		for (int i = 0; i < 10; i++) {
			SailRepository repo = Utils.getInitializedShaclRepository("shaclMax.trig");

			Sail sail = repo.getSail();
//			((ShaclSail) sail).setGlobalLogValidationExecution(true);

			multithreadedMaxCountViolation(IsolationLevels.SNAPSHOT, repo);

			try (SailRepositoryConnection connection = repo.getConnection()) {
				connection.begin();

				ValidationReport revalidate = ((ShaclSailConnection) connection.getSailConnection()).revalidate();
//				Rio.write(revalidate.asModel(), System.out, RDFFormat.TRIG);

				Assertions.assertTrue(revalidate.conforms());

				connection.commit();
			}

			repo.shutDown();
		}

	}

	@Test
	public void testMaxCountSerializable() throws IOException, InterruptedException {

		SailRepository repo = Utils.getInitializedShaclRepository("shaclMax.trig");

		multithreadedMaxCountViolation(IsolationLevels.SERIALIZABLE, repo);

		try (SailRepositoryConnection connection = repo.getConnection()) {
			connection.begin();

			ValidationReport revalidate = ((ShaclSailConnection) connection.getSailConnection()).revalidate();
//			Rio.write(revalidate.asModel(), System.out, RDFFormat.TRIG);

			Assertions.assertTrue(revalidate.conforms());

			connection.commit();
		}
		repo.shutDown();

	}

	@Test
	public void testMaxCount2Serializable() throws IOException, InterruptedException {

		SailRepository repo = Utils.getInitializedShaclRepository("shaclMax.trig");

		multithreadedMaxCount2Violation(IsolationLevels.SERIALIZABLE, repo);

		try (SailRepositoryConnection connection = repo.getConnection()) {
			connection.begin();

			ValidationReport revalidate = ((ShaclSailConnection) connection.getSailConnection()).revalidate();
//			Rio.write(revalidate.asModel(), System.out, RDFFormat.TRIG);

			Assertions.assertTrue(revalidate.conforms());

			connection.commit();
		}
		repo.shutDown();

	}

	@Test
	public void testMaxCount2Snapshot() throws IOException, InterruptedException {

		SailRepository repo = Utils.getInitializedShaclRepository("shaclMax.trig");

		multithreadedMaxCount2Violation(IsolationLevels.SNAPSHOT, repo);

		try (SailRepositoryConnection connection = repo.getConnection()) {
			connection.begin();

			ValidationReport revalidate = ((ShaclSailConnection) connection.getSailConnection()).revalidate();
//			Rio.write(revalidate.asModel(), System.out, RDFFormat.TRIG);

			Assertions.assertTrue(revalidate.conforms());

			connection.commit();
		}
		repo.shutDown();

	}

	@Test
	public void testMaxCount3Snapshot() throws IOException, InterruptedException {

		SailRepository repo = Utils.getInitializedShaclRepository("shaclMax.trig");

		multithreadedMaxCount3Violation(IsolationLevels.SNAPSHOT, repo);

		try (SailRepositoryConnection connection = repo.getConnection()) {
			connection.begin();

			ValidationReport revalidate = ((ShaclSailConnection) connection.getSailConnection()).revalidate();
//			Rio.write(revalidate.asModel(), System.out, RDFFormat.TRIG);

			Assertions.assertTrue(revalidate.conforms());

			connection.commit();
		}
		repo.shutDown();

	}

	@Test
	public void serializableParallelValidation() throws Throwable {

		SailRepository repo = Utils
				.getInitializedShaclRepository("test-cases/complex/targetShapeAndQualifiedShape/shacl.trig");

		ShaclSail sail = (ShaclSail) repo.getSail();
		sail.setShapesGraphs(Set.of(RDF4J.NIL));

		sail.setParallelValidation(true);

		sail.setEclipseRdf4jShaclExtensions(true);

		try (SailRepositoryConnection connection = repo.getConnection()) {
			connection.begin(IsolationLevels.SERIALIZABLE);

			connection.prepareUpdate(IOUtils.toString(
					Objects.requireNonNull(SerializableTest.class.getClassLoader()
							.getResource("test-cases/complex/targetShapeAndQualifiedShape/invalid/case1/query1.rq")),
					StandardCharsets.UTF_8)).execute();

			assertThrows(ShaclSailValidationException.class, () -> {
				try {
					connection.commit();
				} catch (RepositoryException e) {
					throw e.getCause();
				}
			});

		} finally {
			repo.shutDown();
		}

	}

	private void multithreadedMaxCountViolation(IsolationLevels isolationLevel, SailRepository repo)
			throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(2);

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI iri = vf.createIRI("http://example.com/resouce1");

		Runnable runnable1 = () -> {

			try (SailRepositoryConnection connection = repo.getConnection()) {
				connection.begin(isolationLevel);
				countDownLatch.countDown();

				connection.add(iri, RDF.TYPE, RDFS.RESOURCE);
				connection.add(iri, RDFS.LABEL, vf.createLiteral("a"));
				connection.add(iri, RDFS.LABEL, vf.createLiteral("b"));

				try {
					countDownLatch.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}

				try {
					connection.commit();
				} catch (Exception ignored) {

				}
			}

		};

		Runnable runnable2 = () -> {

			try (SailRepositoryConnection connection = repo.getConnection()) {
				connection.begin(isolationLevel);
				countDownLatch.countDown();

				connection.add(iri, RDF.TYPE, RDFS.RESOURCE);
				connection.add(iri, RDFS.LABEL, vf.createLiteral("c"));
				connection.add(iri, RDFS.LABEL, vf.createLiteral("d"));
				try {
					countDownLatch.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}

				try {
					connection.commit();
				} catch (Exception ignored) {

				}
			}

		};

		Thread thread1 = new Thread(runnable1);
		Thread thread2 = new Thread(runnable2);

		thread1.start();
		thread2.start();

		thread1.join();
		thread2.join();
	}

	private void multithreadedMaxCount2Violation(IsolationLevels isolationLevel, SailRepository repo)
			throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(2);

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI iri = vf.createIRI("http://example.com/resouce1");

		Runnable runnable1 = () -> {

			try (SailRepositoryConnection connection = repo.getConnection()) {
				connection.begin(isolationLevel);
				connection.add(iri, RDF.TYPE, RDFS.RESOURCE);
				countDownLatch.countDown();
				try {
					countDownLatch.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}

				try {
					connection.commit();
				} catch (Exception ignored) {

				}
			}

		};

		Runnable runnable2 = () -> {

			try (SailRepositoryConnection connection = repo.getConnection()) {
				connection.begin(isolationLevel);
				connection.add(iri, RDFS.LABEL, vf.createLiteral("a"));
				connection.add(iri, RDFS.LABEL, vf.createLiteral("b"));
				connection.add(iri, RDFS.LABEL, vf.createLiteral("c"));
				countDownLatch.countDown();
				try {
					countDownLatch.await();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}

				try {
					connection.commit();
				} catch (Exception ignored) {

				}
			}

		};

		Thread thread1 = new Thread(runnable1);
		Thread thread2 = new Thread(runnable2);

		thread1.start();
		thread2.start();

		thread1.join();
		thread2.join();
	}

	private void multithreadedMaxCount3Violation(IsolationLevels isolationLevel, SailRepository repo)
			throws InterruptedException {
		CountDownLatch syncPoint1 = new CountDownLatch(1);
		CountDownLatch syncPoint2 = new CountDownLatch(1);
		CountDownLatch syncPoint3 = new CountDownLatch(1);
		CountDownLatch syncPoint4 = new CountDownLatch(2);

		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI resource1 = vf.createIRI("http://example.com/resource1");
		IRI resource2 = vf.createIRI("http://example.com/resource2");

		Runnable runnable1 = () -> {

			try (SailRepositoryConnection connection = repo.getConnection()) {
				syncPoint1.countDown();
				connection.begin(isolationLevel);
				connection.add(resource1, RDF.TYPE, RDFS.RESOURCE);
				syncPoint2.await();
				syncPoint4.await();
				connection.commit();
			} catch (Exception e) {
				System.out.println("runnable1: " + e.getMessage());
			}

		};

		Runnable runnable2 = () -> {

			try {
				syncPoint1.await();
				try (SailRepositoryConnection connection = repo.getConnection()) {
					connection.begin(isolationLevel);
					connection.add(resource1, RDFS.LABEL, vf.createLiteral("a"));
					connection.add(resource1, RDFS.LABEL, vf.createLiteral("b"));
					connection.add(resource1, RDFS.LABEL, vf.createLiteral("c"));

					connection.add(resource2, RDF.TYPE, RDFS.RESOURCE);

					syncPoint3.countDown();
					syncPoint4.await();
					syncPoint2.countDown();
					connection.commit();
				}
			} catch (Exception e) {
				System.out.println("runnable2: " + e.getMessage());
			}

		};

		Runnable runnable3 = () -> {
			try {
				syncPoint3.await();
				try (SailRepositoryConnection connection = repo.getConnection()) {
					connection.begin(IsolationLevels.READ_COMMITTED);
					connection.add(resource2, RDFS.LABEL, vf.createLiteral("d"));
					connection.add(resource2, RDFS.LABEL, vf.createLiteral("e"));
					connection.add(resource2, RDFS.LABEL, vf.createLiteral("f"));
					syncPoint4.countDown();
					syncPoint2.await();
					connection.commit();

				}
			} catch (Exception e) {
				System.out.println("runnable3: " + e.getMessage());
			}

		};

		Runnable runnable4 = () -> {

			try {
				syncPoint3.await();
				try (SailRepositoryConnection connection = repo.getConnection()) {
					connection.begin(IsolationLevels.READ_COMMITTED);
					connection.add(resource2, RDFS.LABEL, vf.createLiteral("d"));
					connection.add(resource2, RDFS.LABEL, vf.createLiteral("e"));
					connection.add(resource2, RDFS.LABEL, vf.createLiteral("f"));
					syncPoint4.countDown();
					syncPoint2.await();
					connection.commit();
				}
			} catch (Exception e) {
				System.out.println("runnable4: " + e.getMessage());
			}

		};

		Thread[] threads = {
				new Thread(runnable1),
				new Thread(runnable2),
				new Thread(runnable3),
				new Thread(runnable4)
		};

		for (Thread thread : threads) {
			thread.start();
		}

		for (Thread thread : threads) {
			thread.join();
		}

	}

}
