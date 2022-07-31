/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.testsuite.repository.optimistic;

import static org.junit.Assert.assertNull;

import java.util.concurrent.CountDownLatch;

import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.testsuite.repository.OptimisticIsolationTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DeadLockTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
	}

	@AfterClass
	public static void afterClass() throws Exception {
		System.setProperty("org.eclipse.rdf4j.repository.debug", "false");
	}

	private Repository repo;

	private RepositoryConnection a;

	private RepositoryConnection b;

	private final IsolationLevel level = IsolationLevels.SNAPSHOT_READ;

	private final String NS = "http://rdf.example.org/";

	private IRI PAINTER;

	private IRI PICASSO;

	private IRI REMBRANDT;

	@Before
	public void setUp() throws Exception {
		repo = OptimisticIsolationTest.getEmptyInitializedRepository(DeadLockTest.class);
		ValueFactory uf = repo.getValueFactory();
		PAINTER = uf.createIRI(NS, "Painter");
		PICASSO = uf.createIRI(NS, "picasso");
		REMBRANDT = uf.createIRI(NS, "rembrandt");
		a = repo.getConnection();
		b = repo.getConnection();
	}

	@After
	public void tearDown() throws Exception {
		try {
			a.close();
		} finally {
			try {
				b.close();
			} finally {
				repo.shutDown();
			}
		}
	}

	@Test
	public void test() throws Exception {
		final CountDownLatch start = new CountDownLatch(2);
		final CountDownLatch end = new CountDownLatch(2);
		final CountDownLatch commit = new CountDownLatch(1);
		final Exception e1 = new Exception();
		new Thread(() -> {
			try {
				start.countDown();
				a.begin(level);
				a.add(PICASSO, RDF.TYPE, PAINTER);
				commit.await();
				a.commit();
			} catch (Exception e) {
				e1.initCause(e);
				a.rollback();
			} finally {
				end.countDown();
			}
		}).start();
		final Exception e2 = new Exception();
		new Thread(() -> {
			try {
				start.countDown();
				b.begin(level);
				b.add(REMBRANDT, RDF.TYPE, PAINTER);
				commit.await();
				b.commit();
			} catch (Exception e) {
				e2.initCause(e);
				b.rollback();
			} finally {
				end.countDown();
			}
		}).start();
		start.await();
		commit.countDown();
		Thread.sleep(500);
		end.await();
		assertNull(e1.getCause());
		assertNull(e2.getCause());
	}

}
