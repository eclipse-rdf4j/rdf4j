package org.eclipse.rdf4j.common.iteration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

public class AutoClosingIterationTest {

	@Test
	public void testClosingStreamWithFilter() {

		CloseableIterationForTesting iterator = getIterator(Arrays.asList("a", "b", "c"));

		List<String> collect = iterator.stream()
				.filter(s -> s.equals("a"))
				.collect(Collectors.toList());

		assertTrue(iterator.closed);

	}

	@Test
	public void testClosingStreamWithAssertionError() {

		CloseableIterationForTesting iterator = getIterator(Arrays.asList("a", "b", "c"));

		try {
			List<String> collect = iterator
					.stream()
					.filter(s -> {
						assertEquals("a", s);
						return s.equals("a");
					})
					.collect(Collectors.toList());
		} catch (Throwable ignored) {

		}

		assertTrue(iterator.closed);

	}

	@Test
	public void testClosingStreamWithAssertionErrorFinally() throws Exception {

		CloseableIterationForTesting iterator = getIterator(Arrays.asList("a", "b", "c"));

		try (iterator) {
			List<String> collect = iterator
					.stream()
					.filter(s -> {
						assertEquals("a", s);
						return s.equals("a");
					})
					.collect(Collectors.toList());
		} catch (Throwable ignored) {

		}

		assertTrue(iterator.closed);

	}

	private CloseableIterationForTesting getIterator(List<String> list) {
		return new CloseableIterationForTesting(list);
	}

	static class CloseableIterationForTesting implements CloseableIteration<String, Exception> {

		public boolean closed = false;
		Iterator<String> iterator;

		public CloseableIterationForTesting(List<String> list) {
			iterator = list.iterator();
		}

		@Override
		public void close() throws Exception {
			closed = true;
		}

		@Override
		public boolean hasNext() throws Exception {
			return iterator.hasNext();
		}

		@Override
		public String next() throws Exception {
			return iterator.next();
		}

		@Override
		public void remove() throws Exception {

		}
	}

}
