/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.Test;

public class ChangesetTest {

	SimpleValueFactory vf = SimpleValueFactory.getInstance();
	Resource[] allGraph = {};

	@Test
	public void testConcurrency() throws InterruptedException, ExecutionException {

		Changeset changeset = getChangeset();

		CountDownLatch countDownLatch = new CountDownLatch(3);

		ExecutorService executorService = Executors.newFixedThreadPool(3,
				r -> {
					Thread t = Executors.defaultThreadFactory().newThread(r);
					// this thread pool does not need to stick around if the all other threads are done
					t.setDaemon(true);
					return t;
				});

		Runnable addingData = () -> {
			countDownLatch.countDown();
			try {
				countDownLatch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for (int i = 0; i < 100000; i++) {
				changeset.approve(vf.createStatement(vf.createBNode(), RDF.TYPE, RDFS.RESOURCE));
			}

		};

		Runnable readingData = () -> {
			countDownLatch.countDown();
			try {
				countDownLatch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for (int i = 0; i < 100000; i++) {
				changeset.hasApproved(null, RDF.TYPE, RDFS.RESOURCE, allGraph);
			}
		};

		Runnable readingDataIterator = () -> {
			countDownLatch.countDown();
			try {
				countDownLatch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			while (!changeset.hasApproved(null, RDF.TYPE, RDFS.RESOURCE, allGraph)) {
				Thread.yield();
			}

			for (int i = 0; i < 100; i++) {
				Iterator<Statement> approvedStatements = changeset.getApprovedStatements(null, RDF.TYPE, null, allGraph)
						.iterator();

				for (int j = 0; j < 100 && approvedStatements.hasNext(); j++) {
					approvedStatements.next();
				}

			}
		};

		Stream.of(addingData, readingData, readingDataIterator)
				.map(executorService::submit)
				.collect(Collectors.toList())
				.forEach(future -> {
					try {
						future.get();
					} catch (InterruptedException | ExecutionException e) {
						throw new RuntimeException(e);
					}
				});

		executorService.shutdown();
		executorService.shutdownNow();
	}

	private Changeset getChangeset() {
		return new Changeset() {
			@Override
			public void flush() throws SailException {

			}

			@Override
			public Model createEmptyModel() {
				// don't use the dynamic model here, we don't want to test upgrading
				return new LinkedHashModel();
			}
		};
	}

}
