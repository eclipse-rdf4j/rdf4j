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
import java.util.Objects;
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
	 * Get a List containing all elements obtained from the specified Iteration.
	 *
	 * @param iteration the Iteration to get the elements from
	 * @return a List containing all elements obtained from the specified Iteration.
	 */
	public static <E, X extends Exception> List<E> asList(CloseableIteration<? extends E, X> iteration) throws X {
		Objects.requireNonNull(iteration);
		// stream.collect is slightly slower than addAll for lists
		List<E> list = new ArrayList<>();

		// addAll closes the iteration
		return addAll(iteration, list);
	}

	/**
	 * Get a Set containing all elements obtained from the specified Iteration.
	 *
	 * @param iteration the Iteration to get the elements from
	 * @return a Set containing all elements obtained from the specified Iteration.
	 */
	public static <E, X extends Exception> Set<E> asSet(CloseableIteration<? extends E, ? extends X> iteration)
			throws X {
		Objects.requireNonNull(iteration);
		try (Stream<? extends E> stream = iteration.stream()) {
			return stream.collect(Collectors.toSet());
		}
	}

	/**
	 * Adds all elements from the supplied Iteration to the specified collection. If the supplied Iteration is an
	 * instance of {@link CloseableIteration} it is automatically closed after consumption.
	 *
	 * @param iteration  An Iteration containing elements to add to the container. If the Iteration is an instance of
	 *                   {@link CloseableIteration} it is automatically closed after consumption.
	 * @param collection The collection to add the elements to.
	 * @return The <var>collection</var> object that was supplied to this method.
	 */
	public static <E, X extends Exception, C extends Collection<E>> C addAll(
			CloseableIteration<? extends E, X> iteration, C collection) throws X {
		Objects.requireNonNull(iteration);
		try (iteration) {
			while (iteration.hasNext()) {
				collection.add(iteration.next());
			}
		}

		return collection;
	}

	public static <T, X extends Exception, K extends CloseableIteration<T, X>> Stream<T> stream(K iteration) {
		Objects.requireNonNull(iteration);
		Spliterator<T> spliterator = new CloseableIterationSpliterator<>(iteration);

		return StreamSupport.stream(spliterator, false).onClose(() -> {
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
	 * Converts an Iteration to a string by concatenating all of the string representations of objects in the Iteration,
	 * divided by a separator.
	 *
	 * @param iteration An Iteration over arbitrary objects that are expected to implement {@link Object#toString()}.
	 * @param separator The separator to insert between the object strings.
	 * @return A String representation of the objects provided by the supplied Iteration.
	 */
	public static <X extends Exception> String toString(CloseableIteration<?, X> iteration, String separator) throws X {
		Objects.requireNonNull(iteration);
		StringBuilder sb = new StringBuilder();
		toString(iteration, separator, sb);
		return sb.toString();
	}

	/**
	 * Converts an Iteration to a string by concatenating all of the string representations of objects in the Iteration,
	 * divided by a separator.
	 *
	 * @param iteration An Iteration over arbitrary objects that are expected to implement {@link Object#toString()}.
	 * @param separator The separator to insert between the object strings.
	 * @param sb        A StringBuilder to append the Iteration string to.
	 */
	public static <X extends Exception> void toString(CloseableIteration<?, X> iteration, String separator,
			StringBuilder sb)
			throws X {
		Objects.requireNonNull(iteration);
		while (iteration.hasNext()) {
			sb.append(iteration.next());

			if (iteration.hasNext()) {
				sb.append(separator);
			}
		}

	}

	/**
	 * Get a Set containing all elements obtained from the specified Iteration.
	 *
	 * @param iteration the Iteration to get the elements from
	 * @param setMaker  the Supplier that constructs a new set
	 * @return a Set containing all elements obtained from the specified Iteration.
	 */
	public static <E, X extends Exception> Set<E> asSet(CloseableIteration<? extends E, ? extends X> iteration,
			Supplier<Set<E>> setMaker) throws X {
		Objects.requireNonNull(iteration);

		Set<E> set = setMaker.get();
		while (iteration.hasNext()) {
			set.add(iteration.next());
		}
		return set;
	}
}
