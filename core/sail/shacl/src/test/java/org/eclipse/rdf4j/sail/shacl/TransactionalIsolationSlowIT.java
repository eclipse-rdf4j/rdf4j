/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.CountDownLatch;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.results.ValidationReport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Tag("slow")
@Isolated
public class TransactionalIsolationSlowIT {

	@Test
	public void testIsolationMultithreaded_READ_COMMITTED() throws Throwable {
		for (int i = 0; i < 1000; i++) {
			ShaclSail shaclSail = new ShaclSail(new MemoryStore());

			SailRepository sailRepository = new SailRepository(shaclSail);
			sailRepository.init();

			try {
				CountDownLatch countDownLatch = new CountDownLatch(2);

				Runnable t1 = () -> {

					try (SailRepositoryConnection connection = sailRepository.getConnection()) {

						connection.begin(IsolationLevels.READ_COMMITTED);
						for (int k = 0; k < 1000; k++) {
							addInTransaction(connection, "ex:steve" + k + " a ex:Person .");

						}

						countDownLatch.countDown();
						try {
							countDownLatch.await();
						} catch (InterruptedException e) {
							throw new IllegalStateException();
						}

						try {
							connection.commit();
						} catch (Throwable ignored) {
						}

					}

				};

				Thread thread1 = new Thread(t1);

				thread1.start();

				Runnable t2 = () -> {
					try (SailRepositoryConnection connection = sailRepository.getConnection()) {

						connection.begin(IsolationLevels.READ_COMMITTED);
						StringReader shaclRules = new StringReader(String.join("\n", "",
								"@prefix ex: <http://example.com/ns#> .",
								"@prefix sh: <http://www.w3.org/ns/shacl#> .",
								"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
								"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

								"ex:PersonShape",
								"        a sh:NodeShape  ;",
								"        sh:targetClass ex:Person ;",
								"        sh:property [",
								"                sh:path ex:age ;",
								"                sh:minCount 1 ;",
								"        ] ;" +
										"        sh:property [",
								"                sh:path ex:age ;",
								"                sh:minCount 1 ;",
								"        ] ;" +
										"        sh:property [",
								"                sh:path ex:age ;",
								"                sh:minCount 1 ;",
								"        ] ;" +
										"        sh:property [",
								"                sh:path ex:age ;",
								"                sh:minCount 1 ;",
								"        ] ;" +
										"        sh:property [",
								"                sh:path ex:age ;",
								"                sh:minCount 1 ;",
								"        ] ;" +
										"        sh:property [",
								"                sh:path ex:age ;",
								"                sh:minCount 1 ;",
								"        ] ;" +
										"        sh:property [",
								"                sh:path ex:age ;",
								"                sh:minCount 1 ;",
								"        ] ;" +
										" ."));

						try {
							connection.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
						} catch (IOException e) {
							throw new IllegalStateException();
						}

						countDownLatch.countDown();
						try {
							countDownLatch.await();
						} catch (InterruptedException e) {
							throw new IllegalStateException();
						}

						try {
							connection.commit();
						} catch (Throwable ignored) {
						}
					}

				};

				Thread thread2 = new Thread(t2);

				thread2.start();

				thread1.join();
				thread2.join();

				try (SailRepositoryConnection connection = sailRepository.getConnection()) {
					connection.begin();
					ValidationReport validationReport = ((ShaclSailConnection) connection.getSailConnection())
							.revalidate();

					if (!validationReport.conforms()) {
						Model statements = validationReport.asModel();
						WriterConfig writerConfig = new WriterConfig();
						writerConfig.set(BasicWriterSettings.PRETTY_PRINT, true);
						writerConfig.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
						Rio.write(statements, System.out, RDFFormat.TRIG, writerConfig);
					}

					assertTrue(validationReport.conforms());

					connection.commit();
				}
			} finally {
				sailRepository.shutDown();
			}

		}
	}

	@Test
	public void testIsolationMultithreaded_SNAPSHOT() throws Throwable {
		for (int i = 0; i < 1000; i++) {
			ShaclSail shaclSail = new ShaclSail(new MemoryStore());

			SailRepository sailRepository = new SailRepository(shaclSail);
			sailRepository.init();

			try {

				CountDownLatch countDownLatch = new CountDownLatch(2);

				Runnable t1 = () -> {

					try (SailRepositoryConnection connection = sailRepository.getConnection()) {

						connection.begin(IsolationLevels.SNAPSHOT);
						for (int k = 0; k < 1000; k++) {
							addInTransaction(connection, "ex:steve" + k + " a ex:Person .");

						}

						countDownLatch.countDown();
						try {
							countDownLatch.await();
						} catch (InterruptedException e) {
							throw new IllegalStateException();
						}

						try {
							connection.commit();
						} catch (Throwable ignored) {
						}

					}

				};

				Thread thread1 = new Thread(t1);

				thread1.start();

				Runnable t2 = () -> {
					try (SailRepositoryConnection connection = sailRepository.getConnection()) {

						connection.begin(IsolationLevels.SNAPSHOT);
						StringReader shaclRules = new StringReader(String.join("\n", "",
								"@prefix ex: <http://example.com/ns#> .",
								"@prefix sh: <http://www.w3.org/ns/shacl#> .",
								"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
								"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",

								"ex:PersonShape",
								"        a sh:NodeShape  ;",
								"        sh:targetClass ex:Person ;",
								"        sh:property [",
								"                sh:path ex:age ;",
								"                sh:minCount 1 ;",
								"        ] ;" +
										"        sh:property [",
								"                sh:path ex:age ;",
								"                sh:minCount 1 ;",
								"        ] ;" +
										"        sh:property [",
								"                sh:path ex:age ;",
								"                sh:minCount 1 ;",
								"        ] ;" +
										"        sh:property [",
								"                sh:path ex:age ;",
								"                sh:minCount 1 ;",
								"        ] ;" +
										"        sh:property [",
								"                sh:path ex:age ;",
								"                sh:minCount 1 ;",
								"        ] ;" +
										"        sh:property [",
								"                sh:path ex:age ;",
								"                sh:minCount 1 ;",
								"        ] ;" +
										"        sh:property [",
								"                sh:path ex:age ;",
								"                sh:minCount 1 ;",
								"        ] ;" +
										" ."));

						try {
							connection.add(shaclRules, "", RDFFormat.TRIG, RDF4J.SHACL_SHAPE_GRAPH);
						} catch (IOException e) {
							throw new IllegalStateException();
						}

						countDownLatch.countDown();
						try {
							countDownLatch.await();
						} catch (InterruptedException e) {
							throw new IllegalStateException();
						}

						try {
							connection.commit();
						} catch (Throwable ignored) {
						}
					}

				};

				Thread thread2 = new Thread(t2);

				thread2.start();

				thread1.join();
				thread2.join();

				try (SailRepositoryConnection connection = sailRepository.getConnection()) {
					connection.begin();
					ValidationReport validationReport = ((ShaclSailConnection) connection.getSailConnection())
							.revalidate();

					if (!validationReport.conforms()) {
						Model statements = validationReport.asModel();
						WriterConfig writerConfig = new WriterConfig();
						writerConfig.set(BasicWriterSettings.PRETTY_PRINT, true);
						writerConfig.set(BasicWriterSettings.INLINE_BLANK_NODES, true);
						Rio.write(statements, System.out, RDFFormat.TRIG, writerConfig);
					}

					assertTrue(validationReport.conforms());

					connection.commit();
				}
			} finally {
				sailRepository.shutDown();
			}

		}
	}

	private void addInTransaction(SailRepositoryConnection connection, String data) {
		data = String.join("\n", "",
				"@prefix ex: <http://example.com/ns#> .",
				"@prefix foaf: <http://xmlns.com/foaf/0.1/>.",
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
				data);

		StringReader stringReader = new StringReader(data);

		try {
			connection.add(stringReader, "", RDFFormat.TRIG);
		} catch (IOException e) {
			throw new IllegalStateException();
		}
	}

}
