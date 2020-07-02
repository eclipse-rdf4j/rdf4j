/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.lucene.util;

import java.util.Iterator;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.IteratorIteration;

/**
 * @deprecated since 3.0. This feature is for internal use only: its existence, signature or behavior may change without
 *             warning from one release to the next.
 */
@Deprecated
@InternalUseOnly
public class IteratorCloseableIteration<E, X extends Exception> extends IteratorIteration<E, X>
		implements CloseableIteration<E, X> {

	public IteratorCloseableIteration(Iterator<? extends E> iter) {
		super(iter);
	}

	@Override
	public void close() throws X {
		// nothing to do
	}
}
