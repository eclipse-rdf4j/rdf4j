/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.iterator;

import java.util.Collection;
import java.util.Iterator;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;

/**
 * An iteration to access a materialized {@link Collection} of BindingSets.
 * 
 * @author Andreas Schwarte
 */
public class CollectionIteration<E, X extends Exception> extends AbstractCloseableIteration<E, X> {

	
	protected final Collection<E> collection;
	protected Iterator<E> iterator;	
	
	/**
	 * @param collection
	 */
	public CollectionIteration(Collection<E> collection) {
		super();
		this.collection = collection;
		iterator = collection.iterator();
	}
	

	public boolean hasNext() throws X {
		return iterator.hasNext();
	}

	public E next() throws X {
		return iterator.next();
	}

	public void remove() throws X {
		throw new UnsupportedOperationException("Remove not supported on CollectionIteration");		
	}
	

}
