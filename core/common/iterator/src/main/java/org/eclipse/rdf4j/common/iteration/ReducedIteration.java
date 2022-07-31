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
package org.eclipse.rdf4j.common.iteration;

/**
 * Removes consecutive duplicates from the object stream.
 *
 * @author Arjohn Kampman
 */
@Deprecated(since = "4.1.0")
public class ReducedIteration<E, X extends Exception> extends FilterIteration<E, X> {

	private E previousObject;

	public ReducedIteration(Iteration<? extends E, ? extends X> delegate) {
		super(delegate);
	}

	@Override
	protected boolean accept(E nextObject) {
		if (nextObject.equals(previousObject)) {
			return false;
		} else {
			previousObject = nextObject;
			return true;
		}
	}
}
