/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.Binding;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;

import junit.framework.TestCase;

/**
 * @author james
 */
public class OrderIteratorTest extends TestCase {

	class IterationStub extends CloseableIteratorIteration<BindingSet, QueryEvaluationException> {

		int hasNextCount = 0;

		int nextCount = 0;

		int removeCount = 0;

		public IterationStub(Iterator<BindingSet> iterator) {
			super(iterator);
		}

		@Override
		public boolean hasNext() throws QueryEvaluationException {
			hasNextCount++;
			return super.hasNext();
		}

		@Override
		public BindingSet next() throws QueryEvaluationException {
			nextCount++;
			return super.next();
		}

		@Override
		public void remove() {
			removeCount++;
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

	public void testFirstHasNext() throws Exception {
		order.hasNext();
		assertEquals(list.size() + 1, iteration.hasNextCount);
		assertEquals(list.size(), iteration.nextCount);
		assertEquals(0, iteration.removeCount);
	}

	public void testHasNext() throws Exception {
		order.hasNext();
		order.next();
		order.hasNext();
		assertEquals(list.size() + 1, iteration.hasNextCount);
		assertEquals(list.size(), iteration.nextCount);
		assertEquals(0, iteration.removeCount);
	}

	public void testFirstNext() throws Exception {
		order.next();
		assertEquals(list.size() + 1, iteration.hasNextCount);
		assertEquals(list.size(), iteration.nextCount);
		assertEquals(0, iteration.removeCount);
	}

	public void testNext() throws Exception {
		order.next();
		order.next();
		assertEquals(list.size() + 1, iteration.hasNextCount);
		assertEquals(list.size(), iteration.nextCount);
		assertEquals(0, iteration.removeCount);
	}

	public void testRemove() throws Exception {
		try {
			order.remove();
			fail();
		} catch (UnsupportedOperationException e) {
		}

	}

	public void testSorting() throws Exception {
		List<BindingSet> sorted = new ArrayList<>(list);
		Collections.sort(sorted, cmp);
		for (BindingSet b : sorted) {
			assertEquals(b, order.next());
		}
		assertFalse(order.hasNext());
	}

	@Override
	protected void setUp() throws Exception {
		list = Arrays.asList(b3, b5, b2, b1, b4, b2);
		cmp = new SizeComparator();
		iteration = new IterationStub(list.iterator());
		order = new OrderIterator(iteration, cmp);
	}

}
