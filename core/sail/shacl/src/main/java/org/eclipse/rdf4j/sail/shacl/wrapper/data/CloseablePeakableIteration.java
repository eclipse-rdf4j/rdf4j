/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.wrapper.data;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;

/**
 * @apiNote since 3.0. This feature is for internal use only: its existence, signature or behavior may change without
 *          warning from one release to the next.
 */
@InternalUseOnly
public class CloseablePeakableIteration<E> implements CloseableIteration<E> {

	CloseableIteration<E> parent;

	E peek;

	public CloseablePeakableIteration(CloseableIteration<E> parent) {
		this.parent = parent;
	}

	@Override
	public void close() {
		parent.close();
	}

	@Override
	public boolean hasNext() {
		if (peek != null) {
			return true;
		}
		return parent.hasNext();
	}

	@Override
	public E next() {
		E next;
		if (peek != null) {
			next = peek;
			peek = null;
		} else {
			next = parent.next();
		}

		return next;
	}

	@Override
	public void remove() {
		parent.remove();
	}

	public E peek() {
		if (peek == null) {
			peek = parent.next();
		}

		return peek;
	}
}
