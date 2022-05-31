/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class consists exclusively of static methods that operate on or return Iterations. It is the
 * Iteration-equivalent of <var>java.util.Collections</var>.
 */
public class Iterations {

	/**
	 * Get a List containing all elements obtained from the specified iteration.
	 *
	 * @param iter the iteration to get the elements from
	 * @return a List containing all elements obtained from the specified iteration.
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	public static <E, X extends Exception> List<E> asList(Iteration<? extends E, X> iter) throws X {
		// stream.collect is slightly slower than addAll for lists
		List<E> list = new ArrayList<>();

		// addAll closes the iteration
		return addAll(iter, list);
	}

	/**
	 * Get a List containing all elements obtained from the specified iteration.
	 *
	 * @param iter the {@link CloseableIteration} to get the elements from
	 * @return a List containing all elements obtained from the specified iteration.
	 */
	public static <E, X extends Exception> List<E> asList(CloseableIteration<? extends E, X> iter) throws X {
		// stream.collect is slightly slower than addAll for lists
		List<E> list = new ArrayList<>();

		// addAll closes the iteration
		return addAll(iter, list);
	}

	/**
	 * Get a Set containing all elements obtained from the specified iteration.
	 *
	 * @param iter the iteration to get the elements from
	 * @return a Set containing all elements obtained from the specified iteration.
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	public static <E, X extends Exception> Set<E> asSet(Iteration<? extends E, X> iter) throws X {
		try (Stream<? extends E> stream = iter.stream()) {
			return stream.collect(Collectors.toSet());
		}
	}

	/**
	 * Get a Set containing all elements obtained from the specified iteration.
	 *
	 * @param iter the {@link CloseableIteration} to get the elements from
	 * @return a Set containing all elements obtained from the specified iteration.
	 */
	public static <E, X extends Exception> Set<E> asSet(CloseableIteration<? extends E, X> iter) throws X {
		try (Stream<? extends E> stream = iter.stream()) {
			return stream.collect(Collectors.toSet());
		}
	}

	/**
	 * Adds all elements from the supplied iteration to the specified collection. If the supplied iteration is an
	 * instance of {@link CloseableIteration} it is automatically closed after consumption.
	 *
	 * @param iter       An iteration containing elements to add to the container. If the iteration is an instance of
	 *                   {@link CloseableIteration} it is automatically closed after consumption.
	 * @param collection The collection to add the elements to.
	 * @return The <var>collection</var> object that was supplied to this method.
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	public static <E, X extends Exception, C extends Collection<E>> C addAll(Iteration<? extends E, X> iter,
			C collection) throws X {
		try {
			while (iter.hasNext()) {
				collection.add(iter.next());
			}
		} finally {
			closeCloseable(iter);
		}

		return collection;
	}

	/**
	 * Adds all elements from the supplied {@link CloseableIteration} to the specified collection then closes the
	 * {@link CloseableIteration}.
	 *
	 * @param iter       A {@link CloseableIteration} containing elements to add to the container.
	 * @param collection The collection to add the elements to.
	 * @return The <var>collection</var> object that was supplied to this method.
	 */
	public static <E, X extends Exception, C extends Collection<E>> C addAll(CloseableIteration<? extends E, X> iter,
			C collection) throws X {
		try {
			while (iter.hasNext()) {
				collection.add(iter.next());
			}
		} finally {
			closeCloseable(iter);
		}

		return collection;
	}

	/**
	 * Get a sequential {@link Stream} with the supplied {@link Iteration} as its source. If the source iteration is a
	 * {@link CloseableIteration}, it will be automatically closed by the stream when done. Any checked exceptions
	 * thrown at any point during stream processing will be propagated wrapped in a {@link RuntimeException}.
	 *
	 * @param iteration a source {@link Iteration} for the stream.
	 * @return a sequential {@link Stream} object which can be used to process the data from the source iteration.
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	public static <T> Stream<T> stream(Iteration<T, ? extends Exception> iteration) {
		Spliterator<T> spliterator = new IterationSpliterator<>(iteration);

		return StreamSupport.stream(spliterator, false).onClose(() -> {
			try {
				Iterations.closeCloseable(iteration);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * Get a sequential {@link Stream} with the supplied {@link CloseableIteration} as its source. The source iteration
	 * will be automatically closed by the stream when done. Any checked exceptions thrown at any point during stream
	 * processing will be propagated wrapped in a {@link RuntimeException}.
	 *
	 * @param iteration a source {@link CloseableIteration} for the stream.
	 * @return a sequential {@link Stream} object which can be used to process the data from the source iteration.
	 */
	public static <T> Stream<T> stream(CloseableIteration<T, ? extends Exception> iteration) {
		return StreamSupport
				.stream(new CloseableIterationSpliterator<>(iteration), false)
				.onClose(() -> {
					try {
						iteration.close();
					} catch (RuntimeException e) {
						throw e;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
	}

	/**
	 * Closes the supplied iteration if it is an instance of {@link CloseableIteration}, otherwise the request is
	 * ignored.
	 *
	 * @param iteration The iteration that should be closed.
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	public static <X extends Exception> void closeCloseable(Iteration<?, X> iteration) throws X {
		if (iteration instanceof CloseableIteration<?, ?>) {
			((CloseableIteration<?, X>) iteration).close();
		}
	}

	/**
	 * Converts an iteration to a string by concatenating all of the string representations of objects in the Iteration,
	 * divided by a separator.
	 *
	 * @param iteration An iteration over arbitrary objects that are expected to implement {@link Object#toString()}.
	 * @param separator The separator to insert between the object strings.
	 * @return A String representation of the objects provided by the supplied iteration.
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	public static <X extends Exception> String toString(Iteration<?, X> iteration, String separator) throws X {
		StringBuilder sb = new StringBuilder();
		toString(iteration, separator, sb);
		return sb.toString();
	}

	/**
	 * Converts a {@link CloseableIteration} to a string by concatenating all the string representations of objects in
	 * the iteration, divided by a separator.
	 *
	 * @param iteration A {@link CloseableIteration} over arbitrary objects that are expected to implement
	 *                  {@link Object#toString()}.
	 * @param separator The separator to insert between the object strings.
	 * @return A String representation of the objects provided by the supplied iteration.
	 */
	public static <X extends Exception> String toString(CloseableIteration<?, X> iteration, String separator) throws X {
		StringBuilder sb = new StringBuilder();
		toString(iteration, separator, sb);
		return sb.toString();
	}

	/**
	 * Converts an iteration to a string by concatenating all the string representations of objects in the Iteration,
	 * divided by a separator.
	 *
	 * @param iteration An iteration over arbitrary objects that are expected to implement {@link Object#toString()}.
	 * @param separator The separator to insert between the object strings.
	 * @param sb        A StringBuilder to append the iteration string to.
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	public static <X extends Exception> void toString(Iteration<?, X> iteration, String separator, StringBuilder sb)
			throws X {
		while (iteration.hasNext()) {
			sb.append(iteration.next());

			if (iteration.hasNext()) {
				sb.append(separator);
			}
		}

	}

	/**
	 * Converts a {@link CloseableIteration} to a string by concatenating all the string representations of objects in
	 * the iteration, divided by a separator.
	 *
	 * @param iteration A {@link CloseableIteration} over arbitrary objects that are expected to implement
	 *                  {@link Object#toString()}.
	 * @param separator The separator to insert between the object strings.
	 * @param sb        A StringBuilder to append the iteration string to.
	 */
	public static <X extends Exception> void toString(CloseableIteration<?, X> iteration, String separator,
			StringBuilder sb)
			throws X {
		while (iteration.hasNext()) {
			sb.append(iteration.next());

			if (iteration.hasNext()) {
				sb.append(separator);
			}
		}

	}

	/**
	 * Get a Set containing all elements obtained from the specified iteration.
	 *
	 * @param iteration the iteration to get the elements from
	 * @param setMaker  the Supplier that constructs a new set
	 * @return a Set containing all elements obtained from the specified iteration.
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	public static <E, X extends Exception> Set<E> asSet(Iteration<? extends E, ? extends X> iteration,
			Supplier<Set<E>> setMaker) throws X {
		Set<E> set = setMaker.get();
		while (iteration.hasNext()) {
			set.add(iteration.next());
		}
		return set;
	}

	/**
	 * Get a Set containing all elements obtained from the specified iteration.
	 *
	 * @param iteration the iteration to get the elements from
	 * @param setMaker  the Supplier that constructs a new set
	 * @return a Set containing all elements obtained from the specified iteration.
	 */
	public static <E, X extends Exception> Set<E> asSet(CloseableIteration<? extends E, ? extends X> iteration,
			Supplier<Set<E>> setMaker) throws X {
		Set<E> set = setMaker.get();
		while (iteration.hasNext()) {
			set.add(iteration.next());
		}
		return set;
	}
}
