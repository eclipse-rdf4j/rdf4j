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
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author james
 */
public class OrderIteratorTest {

	private static class IterationStub extends AbstractCloseableIteration<BindingSet> {

		int hasNextCount = 0;

		int nextCount = 0;

		int removeCount = 0;
		private Iterator<? extends BindingSet> iter;

		public IterationStub(Iterator<BindingSet> iterator) {
			this.iter = iterator;
		}

		@Override
		public boolean hasNext() throws QueryEvaluationException {
			hasNextCount++;
			if (isClosed()) {
				return false;
			}

			boolean result = iter.hasNext();
			if (!result) {
				close();
			}
			return result;
		}

		@Override
		public BindingSet next() throws QueryEvaluationException {
			nextCount++;
			if (isClosed()) {
				throw new NoSuchElementException("Iteration has been closed");
			}

			return iter.next();
		}

		@Override
		public void remove() {
			removeCount++;
		}

		@Override
		protected void handleClose() {

		}
	}

	class SizeComparator implements Comparator<BindingSet> {

		@Override
		public int compare(BindingSet o1, BindingSet o2) {
			return Integer.valueOf(o1.size()).compareTo(Integer.valueOf(o2.size()));
		}
	}

	class BindingSetSize implements BindingSet {

		private static final long serialVersionUID = -7968068342865378845L;

		private final int size;

		public BindingSetSize(int size) {
			super();
			this.size = size;
		}

		@Override
		public Binding getBinding(String bindingName) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<String> getBindingNames() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Value getValue(String bindingName) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasBinding(String bindingName) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Iterator<Binding> iterator() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "#" + size;
		}
	}

	private IterationStub iteration;

	private OrderIterator order;

	private List<BindingSet> list;

	private final BindingSet b1 = new BindingSetSize(1);

	private final BindingSet b2 = new BindingSetSize(2);

	private final BindingSet b3 = new BindingSetSize(3);

	private final BindingSet b4 = new BindingSetSize(4);

	private final BindingSet b5 = new BindingSetSize(5);

	private SizeComparator cmp;

	@Test
	public void testFirstHasNext() {
		order.hasNext();
		assertEquals(list.size() + 1, iteration.hasNextCount);
		assertEquals(list.size(), iteration.nextCount);
		assertEquals(0, iteration.removeCount);
	}

	@Test
	public void testHasNext() {
		order.hasNext();
		order.next();
		order.hasNext();
		assertEquals(list.size() + 1, iteration.hasNextCount);
		assertEquals(list.size(), iteration.nextCount);
		assertEquals(0, iteration.removeCount);
	}

	@Test
	public void testFirstNext() {
		order.next();
		assertEquals(list.size() + 1, iteration.hasNextCount);
		assertEquals(list.size(), iteration.nextCount);
		assertEquals(0, iteration.removeCount);
	}

	@Test
	public void testNext() {
		order.next();
		order.next();
		assertEquals(list.size() + 1, iteration.hasNextCount);
		assertEquals(list.size(), iteration.nextCount);
		assertEquals(0, iteration.removeCount);
	}

	@Test
	public void testRemove() {
		try {
			order.remove();
			fail();
		} catch (UnsupportedOperationException e) {
		}

	}

	@Test
	public void testSorting() {
		List<BindingSet> sorted = new ArrayList<>(list);
		Collections.sort(sorted, cmp);
		for (BindingSet b : sorted) {
			assertEquals(b, order.next());
		}
		assertFalse(order.hasNext());
	}

	@BeforeEach
	protected void setUp() {
		list = Arrays.asList(b3, b5, b2, b1, b4, b2);
		cmp = new SizeComparator();
		iteration = new IterationStub(list.iterator());
		order = new OrderIterator(iteration, cmp);
	}

}
