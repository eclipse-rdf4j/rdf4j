/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PeekMarkIteratorTest {

	@Test
	public void test() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		assertEquals("a", stringPeekMarkIterator.peek());
		assertEquals("a", stringPeekMarkIterator.next());
		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		assertEquals("d", stringPeekMarkIterator.peek());
		stringPeekMarkIterator.reset();
		assertEquals("b", stringPeekMarkIterator.peek());
		assertEquals("b", stringPeekMarkIterator.next());
		assertEquals("c", stringPeekMarkIterator.next());
		stringPeekMarkIterator.reset();
		assertEquals("b", stringPeekMarkIterator.peek());
		assertEquals("b", stringPeekMarkIterator.next());
		assertEquals("c", stringPeekMarkIterator.next());
		stringPeekMarkIterator.reset();
		assertEquals("b", stringPeekMarkIterator.next());
		stringPeekMarkIterator.mark();
		assertEquals("c", stringPeekMarkIterator.next());
		assertEquals("d", stringPeekMarkIterator.next());
		assertEquals("e", stringPeekMarkIterator.next());
		assertEquals("f", stringPeekMarkIterator.next());
	}

	@Test
	public void test2() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		assertEquals("a", stringPeekMarkIterator.peek());
		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // a
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.reset();
		assertEquals("a", stringPeekMarkIterator.peek());
		assertEquals("a", stringPeekMarkIterator.next());
		assertEquals("b", stringPeekMarkIterator.next());
		assertEquals("c", stringPeekMarkIterator.next());
		assertEquals("d", stringPeekMarkIterator.next());
		assertEquals("e", stringPeekMarkIterator.next());
		assertEquals("f", stringPeekMarkIterator.next());
	}

	@Test
	public void test3() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // a
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.next(); // d
		stringPeekMarkIterator.reset();
		stringPeekMarkIterator.next(); // a
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.reset();
		assertEquals("c", stringPeekMarkIterator.next());
		assertEquals("d", stringPeekMarkIterator.next());
		assertEquals("e", stringPeekMarkIterator.next());
		assertEquals("f", stringPeekMarkIterator.next());

	}

	@Test
	public void test4() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // a
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		assertTrue(stringPeekMarkIterator.isMarked());
		assertTrue(stringPeekMarkIterator.isResettable());
		stringPeekMarkIterator.next(); // d
		stringPeekMarkIterator.next(); // e

		stringPeekMarkIterator.reset();
		assertFalse(stringPeekMarkIterator.isMarked());
		assertTrue(stringPeekMarkIterator.isResettable());
		stringPeekMarkIterator.next(); // a
		stringPeekMarkIterator.next(); // b
		assertTrue(stringPeekMarkIterator.isResettable());
		stringPeekMarkIterator.mark();
		assertTrue(stringPeekMarkIterator.isMarked());
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.peek(); // d
		assertTrue(stringPeekMarkIterator.isMarked());

		stringPeekMarkIterator.reset();

		assertEquals("c", stringPeekMarkIterator.next());
		assertEquals("d", stringPeekMarkIterator.next());
		assertEquals("e", stringPeekMarkIterator.next());
		assertEquals("f", stringPeekMarkIterator.next());

	}

	@Test
	public void testResetPossible0() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // a
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.next(); // d

		assertTrue(stringPeekMarkIterator.isResettable());
		stringPeekMarkIterator.reset();

		stringPeekMarkIterator.next(); // a
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.next(); // d
		stringPeekMarkIterator.peek(); // e

		assertTrue(stringPeekMarkIterator.isResettable());
		stringPeekMarkIterator.reset();

		assertEquals("a", stringPeekMarkIterator.next());
		assertEquals("b", stringPeekMarkIterator.next());
		assertEquals("c", stringPeekMarkIterator.next());
		assertEquals("d", stringPeekMarkIterator.next());
		assertEquals("e", stringPeekMarkIterator.next());
		assertEquals("f", stringPeekMarkIterator.next());
		assertFalse(stringPeekMarkIterator.isResettable());

	}

	@Test
	public void testResetPossible0HasNext() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // a
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.next(); // d

		stringPeekMarkIterator.reset();

		stringPeekMarkIterator.next(); // a
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.next(); // d
		assertTrue(stringPeekMarkIterator.hasNext()); // e
		assertTrue(stringPeekMarkIterator.hasNext()); // e

		stringPeekMarkIterator.reset();

		assertEquals("a", stringPeekMarkIterator.next());
		assertEquals("b", stringPeekMarkIterator.next());
		assertEquals("c", stringPeekMarkIterator.next());
		assertEquals("d", stringPeekMarkIterator.next());
		assertEquals("e", stringPeekMarkIterator.next());
		assertEquals("f", stringPeekMarkIterator.next());

	}

	@Test
	public void testHasNext() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		stringPeekMarkIterator.hasNext();
		stringPeekMarkIterator.hasNext(); // a
		assertEquals("a", stringPeekMarkIterator.peek()); // a

	}

	@Test
	public void testClose1() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		stringPeekMarkIterator.hasNext();
		stringPeekMarkIterator.hasNext(); // a
		stringPeekMarkIterator.mark();
		assertEquals("a", stringPeekMarkIterator.peek()); // a
		assertEquals("a", stringPeekMarkIterator.next());
		assertEquals("b", stringPeekMarkIterator.next());
		stringPeekMarkIterator.close();
		Assertions.assertThrows(IllegalStateException.class, stringPeekMarkIterator::reset);

	}

	@Test
	public void testClose2() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		stringPeekMarkIterator.hasNext();
		stringPeekMarkIterator.hasNext(); // a
		assertEquals("a", stringPeekMarkIterator.peek()); // a
		assertEquals("a", stringPeekMarkIterator.next());
		assertEquals("b", stringPeekMarkIterator.next());
		stringPeekMarkIterator.close();
		Assertions.assertThrows(IllegalStateException.class, stringPeekMarkIterator::mark);

	}

	@Test
	public void testClose3() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		stringPeekMarkIterator.hasNext();
		stringPeekMarkIterator.hasNext(); // a
		assertEquals("a", stringPeekMarkIterator.peek()); // a
		assertEquals("a", stringPeekMarkIterator.next());
		assertEquals("b", stringPeekMarkIterator.next());
		stringPeekMarkIterator.close();
		assertFalse(stringPeekMarkIterator.hasNext());

	}

	@Test
	public void testClose4() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		stringPeekMarkIterator.hasNext();
		stringPeekMarkIterator.hasNext(); // a
		assertEquals("a", stringPeekMarkIterator.peek()); // a
		assertEquals("a", stringPeekMarkIterator.next());
		assertEquals("b", stringPeekMarkIterator.next());
		stringPeekMarkIterator.close();
		Assertions.assertThrows(NoSuchElementException.class, stringPeekMarkIterator::next);

	}

	@Test
	public void testClose5() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		stringPeekMarkIterator.hasNext();
		stringPeekMarkIterator.hasNext(); // a
		assertEquals("a", stringPeekMarkIterator.peek()); // a
		assertEquals("a", stringPeekMarkIterator.next());
		assertEquals("b", stringPeekMarkIterator.next());
		stringPeekMarkIterator.close();
		assertNull(stringPeekMarkIterator.peek());

	}

	@Test
	public void testResetWithoutMark() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		assertEquals("a", stringPeekMarkIterator.peek());
		stringPeekMarkIterator.next(); // a
		stringPeekMarkIterator.next(); // b

		assertFalse(stringPeekMarkIterator.isResettable());

		Assertions.assertThrows(IllegalStateException.class, stringPeekMarkIterator::reset);

	}

	@Test
	public void testResetTooLate() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		assertEquals("a", stringPeekMarkIterator.peek());
		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // a
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.reset();
		assertEquals("a", stringPeekMarkIterator.next());
		assertEquals("b", stringPeekMarkIterator.next());
		assertEquals("c", stringPeekMarkIterator.next());
		assertFalse(stringPeekMarkIterator.isResettable());
		Assertions.assertThrows(IllegalStateException.class, stringPeekMarkIterator::reset);
	}

	@Test
	public void testResetTooLate2() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		assertEquals("a", stringPeekMarkIterator.peek());
		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // a
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.reset();
		assertEquals("a", stringPeekMarkIterator.next());
		assertEquals("b", stringPeekMarkIterator.next());
		assertEquals("c", stringPeekMarkIterator.peek());
		assertEquals("c", stringPeekMarkIterator.next());
		assertFalse(stringPeekMarkIterator.isResettable());
		Assertions.assertThrows(IllegalStateException.class, stringPeekMarkIterator::reset);
	}

	@Test
	public void testResetTooLate3() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		assertEquals("a", stringPeekMarkIterator.peek());
		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // a
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.reset();
		assertEquals("a", stringPeekMarkIterator.next());
		assertEquals("b", stringPeekMarkIterator.next());
		assertEquals("c", stringPeekMarkIterator.next());
		assertEquals("d", stringPeekMarkIterator.next());
		assertFalse(stringPeekMarkIterator.isResettable());
		Assertions.assertThrows(IllegalStateException.class, stringPeekMarkIterator::reset);
	}

	@Test
	public void testReadToEndMarkReset() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // a
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.next(); // d
		stringPeekMarkIterator.next(); // e
		stringPeekMarkIterator.next(); // f
		assertFalse(stringPeekMarkIterator.hasNext());

		stringPeekMarkIterator.reset();

		assertEquals("a", stringPeekMarkIterator.next());
		assertEquals("b", stringPeekMarkIterator.next());
		assertEquals("c", stringPeekMarkIterator.next());
		assertEquals("d", stringPeekMarkIterator.next());
		assertEquals("e", stringPeekMarkIterator.next());
		assertEquals("f", stringPeekMarkIterator.next());
		assertFalse(stringPeekMarkIterator.hasNext());

	}

	@Test
	public void testReadToEndMarkReset2() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		stringPeekMarkIterator.next(); // a
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.next(); // d
		stringPeekMarkIterator.next(); // e
		stringPeekMarkIterator.next(); // f
		assertFalse(stringPeekMarkIterator.hasNext());
		stringPeekMarkIterator.mark();
		assertFalse(stringPeekMarkIterator.hasNext());
		stringPeekMarkIterator.reset();
		assertFalse(stringPeekMarkIterator.hasNext());
	}

	@Test
	public void testUnmark() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		assertEquals("a", stringPeekMarkIterator.peek());
		assertEquals("a", stringPeekMarkIterator.next());
		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.next(); // d
		stringPeekMarkIterator.reset();
		assertEquals("b", stringPeekMarkIterator.next());
		stringPeekMarkIterator.unmark();
		assertEquals("c", stringPeekMarkIterator.next());
		stringPeekMarkIterator.mark();
		assertEquals("d", stringPeekMarkIterator.next());
		assertEquals("e", stringPeekMarkIterator.next());
		stringPeekMarkIterator.reset();
		assertEquals("d", stringPeekMarkIterator.next());
		assertEquals("e", stringPeekMarkIterator.next());
	}

	@Test
	public void testUnmark2() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		assertEquals("a", stringPeekMarkIterator.peek());
		assertEquals("a", stringPeekMarkIterator.next());
		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.next(); // d
		stringPeekMarkIterator.reset();
		assertEquals("b", stringPeekMarkIterator.next());
		stringPeekMarkIterator.unmark();
		assertEquals("c", stringPeekMarkIterator.next());
		assertEquals("d", stringPeekMarkIterator.next());
		assertEquals("e", stringPeekMarkIterator.next());
	}

	@Test
	public void testUnmark3() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		assertEquals("a", stringPeekMarkIterator.peek());
		assertEquals("a", stringPeekMarkIterator.next());
		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.next(); // d
		stringPeekMarkIterator.unmark();
		assertFalse(stringPeekMarkIterator.isResettable());

		Assertions.assertThrows(IllegalStateException.class, stringPeekMarkIterator::reset);
	}

	@Test
	public void testUnmark4() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		assertEquals("a", stringPeekMarkIterator.peek());
		assertEquals("a", stringPeekMarkIterator.next());
		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.next(); // d
		stringPeekMarkIterator.reset();
		assertEquals("b", stringPeekMarkIterator.peek());
		stringPeekMarkIterator.unmark();
		stringPeekMarkIterator.mark();
		assertEquals("b", stringPeekMarkIterator.next());
		stringPeekMarkIterator.reset();
		assertEquals("b", stringPeekMarkIterator.peek());

	}

	@Test
	public void testUnmark5() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		assertEquals("a", stringPeekMarkIterator.peek());
		assertEquals("a", stringPeekMarkIterator.next());
		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.next(); // d
		stringPeekMarkIterator.reset();
		stringPeekMarkIterator.unmark();
		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.reset();
		assertEquals("b", stringPeekMarkIterator.peek());

	}

	@Test
	public void testUnmark6() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		assertEquals("a", stringPeekMarkIterator.peek());
		assertEquals("a", stringPeekMarkIterator.next());
		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.next(); // d
		stringPeekMarkIterator.reset();
		stringPeekMarkIterator.unmark();
		stringPeekMarkIterator.peek();
		stringPeekMarkIterator.mark();
		assertEquals("b", stringPeekMarkIterator.next());
		assertEquals("c", stringPeekMarkIterator.next());
		assertEquals("d", stringPeekMarkIterator.next());
		assertEquals("e", stringPeekMarkIterator.next());
		stringPeekMarkIterator.reset();
		assertEquals("b", stringPeekMarkIterator.next());
		assertEquals("c", stringPeekMarkIterator.next());
		assertEquals("d", stringPeekMarkIterator.next());
		assertEquals("e", stringPeekMarkIterator.next());

	}

	@Test
	public void testUnmark7() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		assertEquals("a", stringPeekMarkIterator.peek());
		assertEquals("a", stringPeekMarkIterator.next());
		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.next(); // d
		stringPeekMarkIterator.unmark();
		assertEquals("e", stringPeekMarkIterator.next());
		assertEquals("f", stringPeekMarkIterator.next());
	}

	@Test
	public void testUnmark8() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		assertEquals("a", stringPeekMarkIterator.peek());
		assertEquals("a", stringPeekMarkIterator.next());
		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.next(); // d
		stringPeekMarkIterator.reset();
		stringPeekMarkIterator.unmark();
		assertEquals("b", stringPeekMarkIterator.next());
		assertEquals("c", stringPeekMarkIterator.next());
		assertEquals("d", stringPeekMarkIterator.next());
		assertEquals("e", stringPeekMarkIterator.next());
		assertEquals("f", stringPeekMarkIterator.next());

	}

	@Test
	public void testUnmark9() {

		PeekMarkIterator<String> stringPeekMarkIterator = new PeekMarkIterator<>(
				new CloseableIteratorIteration<>(List.of("a", "b", "c", "d", "e", "f").iterator()));

		assertEquals("a", stringPeekMarkIterator.peek());
		assertEquals("a", stringPeekMarkIterator.next());
		stringPeekMarkIterator.mark();
		stringPeekMarkIterator.next(); // b
		stringPeekMarkIterator.next(); // c
		stringPeekMarkIterator.next(); // d
		stringPeekMarkIterator.unmark();
		assertEquals("e", stringPeekMarkIterator.next());
		assertEquals("f", stringPeekMarkIterator.next());

	}

}
