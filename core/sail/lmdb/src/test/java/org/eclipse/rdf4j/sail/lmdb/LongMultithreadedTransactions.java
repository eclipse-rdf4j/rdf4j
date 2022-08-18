/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.util.Random;
import java.util.stream.IntStream;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailConflictException;
import org.eclipse.rdf4j.sail.SailConnection;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LongMultithreadedTransactions {
	@Rule
	public final TemporaryFolder tmpDir = new TemporaryFolder();

	NotifyingSail getBaseSail() {
		return new LmdbStore(tmpDir.getRoot());
	}

	@Test
	@Ignore
	public void test() {

		ValueFactory vf = SimpleValueFactory.getInstance();

		NotifyingSail baseSail = getBaseSail();

		Random r = new Random();

		IntStream.range(0, 10000).parallel().forEach(i -> {

			try (SailConnection connection = baseSail.getConnection()) {

				executeATransaction(vf, r, i, connection);
				executeATransaction(vf, r, i, connection);
				executeATransaction(vf, r, i, connection);
				executeATransaction(vf, r, i, connection);
				executeATransaction(vf, r, i, connection);
				executeATransaction(vf, r, i, connection);
				executeATransaction(vf, r, i, connection);
				executeATransaction(vf, r, i, connection);
				executeATransaction(vf, r, i, connection);
				executeATransaction(vf, r, i, connection);

			}

		});

	}

	@Test
	@Ignore
	public void test1() {

		ValueFactory vf = SimpleValueFactory.getInstance();

		NotifyingSail baseSail = getBaseSail();

		Random r = new Random();

		try (NotifyingSailConnection connection0 = baseSail.getConnection()) {
			try (NotifyingSailConnection connection1 = baseSail.getConnection()) {
				try (NotifyingSailConnection connection2 = baseSail.getConnection()) {
					try (NotifyingSailConnection connection3 = baseSail.getConnection()) {
						connection0.begin(IsolationLevels.SERIALIZABLE);
						connection1.begin(IsolationLevels.SERIALIZABLE);
						connection2.begin(IsolationLevels.SERIALIZABLE);
						connection3.begin(IsolationLevels.SERIALIZABLE);

						boolean b1 = connection0.hasStatement(null, null, null, true);
						boolean b2 = connection1.hasStatement(null, null, null, true);
						boolean b3 = connection2.hasStatement(null, null, null, true);
						boolean b4 = connection3.hasStatement(null, null, null, true);

						System.out.println(b1);
						System.out.println(b2);
						System.out.println(b3);
						System.out.println(b4);

						connection0.addStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral("a"));
						connection1.addStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral("a"));
						connection2.addStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral("a"));
						connection3.addStatement(vf.createBNode(), RDFS.LABEL, vf.createLiteral("a"));

						IntStream.range(0, 4).parallel().forEach(i -> {
							if (i == 0) {
								try {
									connection0.prepare();
								} catch (Exception e) {
									e.printStackTrace();
									connection0.rollback();
									throw e;
								}
							}
							if (i == 1) {
								connection1.addStatement(vf.createIRI("http://example.com/" + i), RDFS.LABEL,
										vf.createLiteral("a"));
							}
							if (i == 2) {
								try {
									connection2.prepare();
								} catch (Exception e) {
									e.printStackTrace();
									connection2.rollback();
									throw e;
								}
							}
							if (i == 3) {
								connection3.addStatement(vf.createIRI("http://example.com/" + i), RDFS.LABEL,
										vf.createLiteral("a"));
							}

						});

						connection1.prepare();
						connection3.prepare();

						connection0.commit();
						connection1.commit();
						connection2.commit();
						connection3.commit();

					}
				}
			}
		}

	}

	private void executeATransaction(ValueFactory vf, Random r, int i, SailConnection connection) {
		connection.begin(IsolationLevels.SERIALIZABLE);

		boolean b = connection.hasStatement(null, null, null, true);

		if (i % 10 == 0) {

			connection.removeStatements(null, null, null);
			connection.flush();

		}

		{
			int function = r.nextInt(9);

			IRI iri = vf.createIRI("http://example.com/" + r.nextInt(10));

			int i1 = r.nextInt(i);

			for (int k = 0; k < 1000; k++) {

				switch (function) {
				case 0:
					connection.addStatement(iri, RDFS.LABEL, vf.createLiteral(k + "_" + i1));
					break;
				case 1:
					connection.hasStatement(iri, RDFS.LABEL, vf.createLiteral(k + "_" + i1), true);
					break;
				case 2:
					connection.hasStatement(null, RDFS.LABEL, vf.createLiteral(k + "_" + i1), true);
					break;
				case 3:
					connection.hasStatement(iri, null, vf.createLiteral(k + "_" + i1), true);
					break;
				case 4:
					connection.hasStatement(iri, RDFS.LABEL, null, true);
					break;
				case 5:
					connection.hasStatement(null, null, vf.createLiteral(k + "_" + i1), true);
					break;
				case 6:
					connection.hasStatement(iri, null, null, true);
					break;
				case 7:
					connection.hasStatement(null, RDFS.LABEL, null, true);
					break;
				case 8:
					connection.removeStatements(iri, RDFS.LABEL, vf.createLiteral(k + "_" + i1));
					break;
				}
				Thread.yield();
				connection.flush();

			}

		}

		{
			int function = r.nextInt(9);

			IRI iri = vf.createIRI("http://example.com/" + r.nextInt(10));

			int i1 = r.nextInt(i);

			for (int k = 0; k < 1000; k++) {

				switch (function) {
				case 0:
					connection.addStatement(iri, RDFS.LABEL, vf.createLiteral(k + "_" + i1));
					break;
				case 1:
					connection.hasStatement(iri, RDFS.LABEL, vf.createLiteral(k + "_" + i1), true);
					break;
				case 2:
					connection.hasStatement(null, RDFS.LABEL, vf.createLiteral(k + "_" + i1), true);
					break;
				case 3:
					connection.hasStatement(iri, null, vf.createLiteral(k + "_" + i1), true);
					break;
				case 4:
					connection.hasStatement(iri, RDFS.LABEL, null, true);
					break;
				case 5:
					connection.hasStatement(null, null, vf.createLiteral(k + "_" + i1), true);
					break;
				case 6:
					connection.hasStatement(iri, null, null, true);
					break;
				case 7:
					connection.hasStatement(null, RDFS.LABEL, null, true);
					break;
				case 8:
					connection.removeStatements(iri, RDFS.LABEL, vf.createLiteral(k + "_" + i1));
					break;
				}
				Thread.yield();
			}

		}

		{
			int function = r.nextInt(9);

			IRI iri = vf.createIRI("http://example.com/" + r.nextInt(10));

			int i1 = r.nextInt(i);

			for (int k = 0; k < 1000; k++) {

				switch (function) {
				case 0:
					connection.addStatement(iri, RDFS.LABEL, vf.createLiteral(k + "_" + i1));
					break;
				case 1:
					connection.hasStatement(iri, RDFS.LABEL, vf.createLiteral(k + "_" + i1), true);
					break;
				case 2:
					connection.hasStatement(null, RDFS.LABEL, vf.createLiteral(k + "_" + i1), true);
					break;
				case 3:
					connection.hasStatement(iri, null, vf.createLiteral(k + "_" + i1), true);
					break;
				case 4:
					connection.hasStatement(iri, RDFS.LABEL, null, true);
					break;
				case 5:
					connection.hasStatement(null, null, vf.createLiteral(k + "_" + i1), true);
					break;
				case 6:
					connection.hasStatement(iri, null, null, true);
					break;
				case 7:
					connection.hasStatement(null, RDFS.LABEL, null, true);
					break;
				case 8:
					connection.removeStatements(iri, RDFS.LABEL, vf.createLiteral(k + "_" + i1));
					break;
				}
				Thread.yield();
			}

		}
		try {
			Thread.yield();
			connection.prepare();
			Thread.yield();
			connection.commit();
			System.out.println(b);

		} catch (SailConflictException ignore) {

			connection.rollback();
			executeATransaction(vf, r, i, connection);
		}
	}
}
