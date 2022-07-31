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
package org.eclipse.rdf4j.repository.sail;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ExceptionConvertingIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.sail.SailException;

/**
 * @author Herko ter Horst
 */
class SailCloseableIteration<E> extends ExceptionConvertingIteration<E, RepositoryException> {

	public SailCloseableIteration(CloseableIteration<? extends E, ? extends SailException> iter) {
		super(iter);
	}

	@Override
	protected RepositoryException convert(Exception e) {
		if (e instanceof SailException) {
			return new RepositoryException(e);
		} else if (e instanceof RuntimeException) {
			throw (RuntimeException) e;
		} else if (e == null) {
			throw new IllegalArgumentException("e must not be null");
		} else {
			throw new IllegalArgumentException("Unexpected exception type: " + e.getClass());
		}
	}
}
