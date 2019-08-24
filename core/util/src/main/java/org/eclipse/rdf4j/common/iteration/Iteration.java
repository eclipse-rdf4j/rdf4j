/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.NoSuchElementException;

/**
 * An Iteration is a typed Iterator-like object that can throw (typed) Exceptions while iterating. This is used in cases
 * where the iteration is lazy and evaluates over a (remote) connection, for example accessing a database. In such cases
 * an error can occur at any time and needs to be communicated through a checked exception, something
 * {@link java.util.Iterator} can not do (it can only throw {@link RuntimeException}s.
 * 
 * @param <E> Object type of objects contained in the iteration.
 * @param <X> Exception type that is thrown when a problem occurs during iteration.
 * @see java.util.Iterator
 * @author jeen
 * @author Herko ter Horst
 */
public interface Iteration<E, X extends Exception> {

	/**
	 * Returns <tt>true</tt> if the iteration has more elements. (In other words, returns <tt>true</tt> if {@link #next}
	 * would return an element rather than throwing a <tt>NoSuchElementException</tt>.)
	 * 
	 * @return <tt>true</tt> if the iteration has more elements.
	 * @throws X
	 */
	public boolean hasNext() throws X;

	/**
	 * Returns the next element in the iteration.
	 * 
	 * @return the next element in the iteration.
	 * @throws NoSuchElementException if the iteration has no more elements or if it has been closed.
	 */
	public E next() throws X;

	/**
	 * Removes from the underlying collection the last element returned by the iteration (optional operation). This
	 * method can be called only once per call to next.
	 * 
	 * @throws UnsupportedOperationException if the remove operation is not supported by this Iteration.
	 * @throws IllegalStateException         If the Iteration has been closed, or if <tt>next()</tt> has not yet been
	 *                                       called, or <tt>remove()</tt> has already been called after the last call to
	 *                                       <tt>next()</tt>.
	 */
	public void remove() throws X;
}
