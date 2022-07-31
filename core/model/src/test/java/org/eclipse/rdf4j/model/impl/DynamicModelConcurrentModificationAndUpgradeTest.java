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

package org.eclipse.rdf4j.model.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.Test;

public class DynamicModelConcurrentModificationAndUpgradeTest {

	/**
	 * Add multiple statements while forcing an upgrade to make sure we then get an exception because the underlying
	 * storage was upgraded
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testConcurrentAddAndUpgrade() throws InterruptedException {

		for (int i = 0; i < 100; i++) {
			Exception exception = runTest();

			if (exception != null) {
				assertThat(exception).isInstanceOf(UnsupportedOperationException.class);
				return;
			}
		}

		fail("There should have been an UnsupportedOperationException earlier");

	}

	private Exception runTest() throws InterruptedException {
		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		List<Statement> statements = Arrays.asList(
				vf.createStatement(vf.createBNode(), RDF.TYPE, RDFS.RESOURCE),
				vf.createStatement(vf.createBNode(), RDF.TYPE, RDFS.RESOURCE),
				vf.createStatement(vf.createBNode(), RDF.TYPE, RDFS.RESOURCE),
				vf.createStatement(vf.createBNode(), RDF.TYPE, RDFS.RESOURCE),
				vf.createStatement(vf.createBNode(), RDF.TYPE, RDFS.RESOURCE));

		DynamicModel model = new DynamicModel(new LinkedHashModelFactory());

		CountDownLatch countDownLatch2 = new CountDownLatch(1);
		CountDownLatch countDownLatch1 = new CountDownLatch(1);

		final Exception[] exception = new Exception[1];

		Runnable addAll = () -> {
			try {
				model.addAll(new Collection<>() {
					@Override
					public int size() {
						return statements.size();
					}

					@Override
					public boolean isEmpty() {
						return statements.isEmpty();
					}

					@Override
					public boolean contains(Object o) {
						return statements.contains(o);
					}

					@Override
					public Iterator<Statement> iterator() {
						return new Iterator<>() {

							final Iterator<Statement> iterator = statements.iterator();

							@Override
							public boolean hasNext() {
								try {
									countDownLatch1.countDown();
									countDownLatch2.await();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								Thread.yield();
								return iterator.hasNext();
							}

							@Override
							public Statement next() {
								try {
									countDownLatch2.await();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								Thread.yield();
								return iterator.next();
							}
						};
					}

					@Override
					public Object[] toArray() {
						return statements.toArray();
					}

					@Override
					public <T> T[] toArray(T[] a) {
						return statements.toArray(a);
					}

					@Override
					public boolean add(Statement statement) {
						return false;
					}

					@Override
					public boolean remove(Object o) {
						return false;
					}

					@Override
					public boolean containsAll(Collection<?> c) {
						return statements.containsAll(c);
					}

					@Override
					public boolean addAll(Collection<? extends Statement> c) {
						return false;
					}

					@Override
					public boolean removeAll(Collection<?> c) {
						return false;
					}

					@Override
					public boolean retainAll(Collection<?> c) {
						return false;
					}

					@Override
					public void clear() {
						System.out.println();
					}
				});
			} catch (Exception e) {
				exception[0] = e;
			}
		};

		Runnable upgrade = () -> {
			try {
				countDownLatch1.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			model.filter(null, RDF.TYPE, null);
			countDownLatch2.countDown();
		};

		Thread addAllThread = new Thread(addAll);
		Thread upgradeThread = new Thread(upgrade);

		addAllThread.start();
		upgradeThread.start();

		addAllThread.join();
		upgradeThread.join();

		return exception[0];
	}

}
