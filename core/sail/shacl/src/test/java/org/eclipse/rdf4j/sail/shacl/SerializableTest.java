/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import static junit.framework.TestCase.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.junit.Ignore;
import org.junit.Test;

public class SerializableTest {

	@Test
	public void testMaxCountSnapshot() throws IOException, InterruptedException {
		for (int i = 0; i < 10; i++) {
			SailRepository repo = Utils.getInitializedShaclRepository("shaclMax.ttl", false);

			Sail sail = repo.getSail();
//			((ShaclSail) sail).setGlobalLogValidationExecution(true);

			multithreadedMaxCountViolation(IsolationLevels.SNAPSHOT, repo);

			try (SailRepositoryConnection connection = repo.getConnection()) {
				connection.begin();

				ValidationReport revalidate = ((ShaclSailConnection) connection.getSailConnection()).revalidate();
//				Rio.write(revalidate.asModel(), System.out, RDFFormat.TURTLE);

				assertTrue(revalidate.conforms());

				connection.commit();
			}

			repo.shutDown();
		}

	}

	@Test
	public void testMaxCountSerializable() throws IOException, InterruptedException {

		SailRepository repo = Utils.getInitializedShaclRepository("shaclMax.ttl", false);

		multithreadedMaxCountViolation(IsolationLevels.SERIALIZABLE, repo);

		try (SailRepositoryConnection connection = repo.getConnection()) {
			connection.begin();

			ValidationReport revalidate = ((ShaclSailConnection) connection.getSailConnection()).revalidate();
//			Rio.write(revalidate.asModel(), System.out, RDFFormat.TURTLE);

			assertTrue(revalidate.conforms());

			connection.commit();
		}
		repo.shutDown();

	}

	@Test
	public void testMaxCount2Serializable() throws IOException, InterruptedException {

		SailRepository repo = Utils.getInitializedShaclRepository("shaclMax.ttl", false);

		multithreadedMaxCount2Violation(IsolationLevels.SERIALIZABLE, repo);

		try (SailRepositoryConnection connection = repo.getConnection()) {
			connection.begin();

			ValidationReport revalidate = ((ShaclSailConnection) connection.getSailConnection()).revalidate();
//			Rio.write(revalidate.asModel(), System.out, RDFFormat.TURTLE);

			assertTrue(revalidate.conforms());

			connection.commit();
		}
		repo.shutDown();

	}

	@Test
	public void testMaxCount2Snapshot() throws IOException, InterruptedException {

		SailRepository repo = Utils.getInitializedShaclRepository("shaclMax.ttl", false);

		multithreadedMaxCount2Violation(IsolationLevels.SNAPSHOT, repo);

		try (SailRepositoryConnection connection = repo.getConnection()) {
			connection.begin();

			ValidationReport revalidate = ((ShaclSailConnection) connection.getSailConnection()).revalidate();
//			Rio.write(revalidate.asModel(), System.out, RDFFormat.TURTLE);

			assertTrue(revalidate.conforms());

			connection.commit();
		}
		repo.shutDown();

	}

	@Test(expected = ShaclSailValidationException.class)
	public void serializableParallelValidation() throws Throwable {

		SailRepository repo = Utils
				.getInitializedShaclRepository("test-cases/complex/targetShapeAndQualifiedShape/shacl.ttl", false);

		ShaclSail sail = (ShaclSail) repo.getSail();

		sail.setParallelValidation(true);

		sail.setEclipseRdf4jShaclExtensions(true);

		try (SailRepositoryConnection connection = repo.getConnection()) {
			connection.begin(IsolationLevels.SERIALIZABLE);

			connection.prepareUpdate(IOUtils.toString(
					SerializableTest.class.getClassLoader()
							.getResource("test-cases/complex/targetShapeAndQualifiedShape/invalid/case1/query1.rq"),
					StandardCharsets.UTF_8)).execute();

			connection.commit();
		} catch (RepositoryException e) {
			throw e.getCause();
		}
		repo.shutDown();

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

}
