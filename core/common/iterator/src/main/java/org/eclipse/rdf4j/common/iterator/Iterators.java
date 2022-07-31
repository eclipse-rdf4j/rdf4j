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

package org.eclipse.rdf4j.common.iterator;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This class consists exclusively of static methods that operate on or return iterators. It is the Iterator-equivalent
 * of {@link Collections}.
 *
 * @deprecated use Commons Collections or Guava instead.
 */
@Deprecated(since = "4.1.0", forRemoval = true)
public class Iterators {

	/**
	 * Get a List containing all elements obtained from the specified iterator.
	 *
	 * @param iter the iterator to get the elements from
	 * @return a List containing all elements obtained from the specified iterator.
	 */
	public static <E> List<E> asList(Iterator<? extends E> iter) {
		List<E> result = new ArrayList<>();
		addAll(iter, result);
		return result;
	}

	/**
	 * Adds all elements from the supplied iterator to the specified collection.
	 *
	 * @param iter       An iterator containing elements to add to the container.
	 * @param collection The collection to add the elements to.
	 * @return The <var>collection</var> object that was supplied to this method.
	 */
	public static <E, C extends Collection<E>> C addAll(Iterator<? extends E> iter, C collection) {
		while (iter.hasNext()) {
			collection.add(iter.next());
		}

		return collection;
	}

	/**
	 * Converts an iterator to a string by concatenating all of the string representations of objects in the iterator,
	 * divided by a separator.
	 *
	 * @param iter      An iterator over arbitrary objects that are expected to implement {@link Object#toString()}.
	 * @param separator The separator to insert between the object strings.
	 * @return A String representation of the objects provided by the supplied iterator.
	 */
	public static String toString(Iterator<?> iter, String separator) {
		StringBuilder sb = new StringBuilder();
		toString(iter, separator, sb);
		return sb.toString();
	}

	/**
	 * Converts an iterator to a string by concatenating all of the string representations of objects in the iterator,
	 * divided by a separator.
	 *
	 * @param iter      An iterator over arbitrary objects that are expected to implement {@link Object#toString()}.
	 * @param separator The separator to insert between the object strings.
	 * @param sb        A StringBuilder to append the iterator string to.
	 */
	public static void toString(Iterator<?> iter, String separator, StringBuilder sb) {
		while (iter.hasNext()) {
			sb.append(iter.next());

			if (iter.hasNext()) {
				sb.append(separator);
			}
		}
	}

	/**
	 * Closes the given iterator if it implements {@link java.io.Closeable} else do nothing.
	 *
	 * @param iter The iterator to close.
	 * @throws IOException If an underlying I/O error occurs.
	 */
	public static void close(Iterator<?> iter) throws IOException {
		if (iter instanceof Closeable) {
			((Closeable) iter).close();
		}
	}

	/**
	 * Closes the given iterator, swallowing any IOExceptions, if it implements {@link java.io.Closeable} else do
	 * nothing.
	 *
	 * @param iter The iterator to close.
	 */
	public static void closeSilently(Iterator<?> iter) {
		if (iter instanceof Closeable) {
			try {
				((Closeable) iter).close();
			} catch (IOException ioe) {
				// ignore
			}
		}
	}
}
