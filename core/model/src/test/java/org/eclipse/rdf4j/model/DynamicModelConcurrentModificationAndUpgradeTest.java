/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.model;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.Test;

public class DynamicModelConcurrentModificationAndUpgradeTest {

	@Test
	public void testConcurrentAddAndUpgrade() throws InterruptedException {

		SimpleValueFactory vf = SimpleValueFactory.getInstance();
		List<Statement> statements = Arrays.asList(
				vf.createStatement(vf.createBNode(), RDF.TYPE, RDFS.RESOURCE),
				vf.createStatement(vf.createBNode(), RDF.TYPE, RDFS.RESOURCE),
				vf.createStatement(vf.createBNode(), RDF.TYPE, RDFS.RESOURCE),
				vf.createStatement(vf.createBNode(), RDF.TYPE, RDFS.RESOURCE),
				vf.createStatement(vf.createBNode(), RDF.TYPE, RDFS.RESOURCE));

		DynamicModel model = new DynamicModel(new LinkedHashModelFactory());

		CountDownLatch countDownLatch = new CountDownLatch(1);

		final Exception[] exception = new Exception[1];

		Runnable addAll = () -> {
			try {
				model.addAll(new Collection<Statement>() {
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
						return new Iterator<Statement>() {

							Iterator<Statement> iterator = statements.iterator();

							@Override
							public boolean hasNext() {
								try {
									countDownLatch.await();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								return iterator.hasNext();
							}

							@Override
							public Statement next() {
								try {
									countDownLatch.await();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
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
			model.filter(null, RDF.TYPE, null);
			countDownLatch.countDown();
		};

		Thread addAllThread = new Thread(addAll);
		Thread upgradeThread = new Thread(upgrade);

		addAllThread.start();
		upgradeThread.start();

		addAllThread.join();
		upgradeThread.join();

		assertEquals(UnsupportedOperationException.class, exception[0].getClass());

	}

}
