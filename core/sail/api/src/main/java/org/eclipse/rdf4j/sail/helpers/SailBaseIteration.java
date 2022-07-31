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
package org.eclipse.rdf4j.sail.helpers;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.IterationWrapper;

/**
 * An iteration extension that keeps a reference to the AbstractSailConnection from which it originates and signals when
 * it is closed.
 *
 * @author Jeen Broekstra
 */
class SailBaseIteration<T, E extends Exception> extends IterationWrapper<T, E> {

	private final AbstractSailConnection connection;

	/**
	 * Creates a new memory-store specific iteration object.
	 *
	 * @param iter       the wrapped iteration over sail objects.
	 * @param connection the connection from which this iteration originates.
	 */
	public SailBaseIteration(CloseableIteration<? extends T, ? extends E> iter, AbstractSailConnection connection) {
		super(iter);
		this.connection = connection;
	}

	@Override
	public boolean hasNext() throws E {
		if (isClosed()) {
			return false;
		}

		if (super.hasNext()) {
			return true;
		}

		// auto-close when exhausted
		close();
		return false;
	}

	@Override
	protected void handleClose() throws E {
		try {
			super.handleClose();
		} finally {
			connection.iterationClosed(this);
		}
	}

	@Deprecated
	protected void forceClose() throws E {
		close();
	}
}
