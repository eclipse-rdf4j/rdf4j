/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.iterator;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.util.iterators.Iterators;


/**
 * Wraps an Iteration as an Iterator.
 * If the Iteration is a CloseableIteration then this.close() will close it
 * and it will also be automatically closed when this Iterator is exhausted. 
 * @author Mark
 */
public class CloseableIterationIterator<E> implements Iterator<E>, Closeable {
	private final Iteration<? extends E, ? extends RuntimeException> iteration;

	public CloseableIterationIterator(Iteration<? extends E, ? extends RuntimeException> iteration) {
		this.iteration = iteration;
	}

	@Override
	public boolean hasNext() {
		boolean hasMore = iteration.hasNext();
		if(!hasMore) {
			Iterators.closeSilently(this);
		}
		return hasMore;
	}

	@Override
	public E next() {
		return iteration.next();
	}

	@Override
	public void remove() {
		iteration.remove();
	}

	@Override
	public void close()
		throws IOException
	{
		Iterations.closeCloseable(iteration);
	}
}
