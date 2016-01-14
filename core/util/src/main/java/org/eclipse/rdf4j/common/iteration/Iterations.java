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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class consists exclusively of static methods that operate on or return
 * Iterations. It is the Iteration-equivalent of <tt>java.util.Collections</tt>.
 */
public class Iterations {

	/**
	 * Get a List containing all elements obtained from the specified Iteration.
	 * 
	 * @param iter
	 *        the Iteration to get the elements from
	 * @return a List containing all elements obtained from the specified
	 *         Iteration.
	 */
	public static <E, X extends Exception> List<E> asList(Iteration<? extends E, X> iter)
		throws X
	{
		List<E> result = new ArrayList<E>();
		addAll(iter, result);
		return result;
	}

	/**
	 * Get a Set containing all elements obtained from the specified Iteration.
	 * 
	 * @param iter
	 *        the Iteration to get the elements from
	 * @return a Set containing all elements obtained from the specified
	 *         Iteration.
	 */
	public static <E, X extends Exception> Set<E> asSet(Iteration<? extends E, X> iter)
		throws X
	{
		Set<E> result = new HashSet<E>();
		addAll(iter, result);
		return result;
	}

	/**
	 * Adds all elements from the supplied Iteration to the specified collection.
	 * If the supplied Iteration is an instance of {@link CloseableIteration} it
	 * is automatically closed after consumption.
	 * 
	 * @param iter
	 *        An Iteration containing elements to add to the container. If the
	 *        Iteration is an instance of {@link CloseableIteration} it is
	 *        automatically closed after consumption.
	 * @param collection
	 *        The collection to add the elements to.
	 * @return The <tt>collection</tt> object that was supplied to this method.
	 */
	public static <E, X extends Exception, C extends Collection<E>> C addAll(Iteration<? extends E, X> iter,
			C collection)
		throws X
	{
		try {
			while (iter.hasNext()) {
				collection.add(iter.next());
			}
		}
		finally {
			closeCloseable(iter);
		}

		return collection;
	}

	/**
	 * Get a sequential {@link Stream} with the supplied {@link Iteration} as its
	 * source. If the source iteration is a {@link CloseableIteration}, it will
	 * be automatically closed by the stream when done. Any checked exceptions
	 * thrown at any point during stream processing will be propagated wrapped in
	 * a {@link RuntimeException}.
	 * 
	 * @param iteration
	 *        a source {@link Iteration} for the stream.
	 * @return a sequential {@link Stream} object which can be used to process
	 *         the data from the source iteration.
	 * @since 4.0
	 */
	public static <T> Stream<T> stream(Iteration<T, ? extends Exception> iteration) {
		Spliterator<T> spliterator = new IterationSpliterator<T>(iteration);

		return StreamSupport.stream(spliterator, false).onClose(() -> {
			try {
				Iterations.closeCloseable(iteration);
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	/**
	 * Closes the supplied Iteration if it is an instance of
	 * {@link CloseableIteration}, otherwise the request is ignored.
	 * 
	 * @param iter
	 *        The Iteration that should be closed.
	 */
	public static <X extends Exception> void closeCloseable(Iteration<?, X> iter)
		throws X
	{
		if (iter instanceof CloseableIteration<?, ?>) {
			((CloseableIteration<?, X>)iter).close();
		}
	}

	/**
	 * Converts an Iteration to a string by concatenating all of the string
	 * representations of objects in the Iteration, divided by a separator.
	 * 
	 * @param iter
	 *        An Iteration over arbitrary objects that are expected to implement
	 *        {@link Object#toString()}.
	 * @param separator
	 *        The separator to insert between the object strings.
	 * @return A String representation of the objects provided by the supplied
	 *         Iteration.
	 */
	public static <X extends Exception> String toString(Iteration<?, X> iter, String separator)
		throws X
	{
		StringBuilder sb = new StringBuilder();
		toString(iter, separator, sb);
		return sb.toString();
	}

	/**
	 * Converts an Iteration to a string by concatenating all of the string
	 * representations of objects in the Iteration, divided by a separator.
	 * 
	 * @param iter
	 *        An Iteration over arbitrary objects that are expected to implement
	 *        {@link Object#toString()}.
	 * @param separator
	 *        The separator to insert between the object strings.
	 * @param sb
	 *        A StringBuilder to append the Iteration string to.
	 */
	public static <X extends Exception> void toString(Iteration<?, X> iter, String separator, StringBuilder sb)
		throws X
	{
		while (iter.hasNext()) {
			sb.append(iter.next());

			if (iter.hasNext()) {
				sb.append(separator);
			}
		}

	}
}
