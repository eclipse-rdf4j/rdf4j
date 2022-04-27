/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An iteration wrapper that silently ignores any errors that occur during processing.
 *
 * @author Jeen Broekstra
 */
public class SilentIteration<T, E extends Exception> implements CloseableIteration<T, E> {

	private static final Logger logger = LoggerFactory.getLogger(SilentIteration.class);

	private final CloseableIteration<T, E> delegate;

	public SilentIteration(CloseableIteration<T, E> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void close() throws E {
		try {
			delegate.close();
		} catch (Exception e) {
			if (logger.isTraceEnabled()) {
				logger.trace("Suppressed error in SILENT iteration: " + e.getMessage(), e);
			}
		}
	}

	@Override
	public void remove() throws E {
		try {
			delegate.remove();
		} catch (Exception e) {
			if (logger.isTraceEnabled()) {
				logger.trace("Suppressed error in SILENT iteration: " + e.getMessage(), e);
			}
		}
	}

	@Override
	public boolean hasNext() throws E {
		try {
			return delegate.hasNext();
		} catch (Exception e) {
			if (logger.isTraceEnabled()) {
				logger.trace("Suppressed error in SILENT iteration: " + e.getMessage(), e);
			}
		}
		return false;
	}

	@Override
	public T next() throws E {
		try {
			return delegate.next();
		} catch (NoSuchElementException e) {
			// pass through
			throw e;
		} catch (Exception e) {
			if (logger.isTraceEnabled()) {
				logger.trace("Converted error in SILENT iteration: " + e.getMessage(), e);
			}
			throw new NoSuchElementException(e.getMessage());
		}
	}

}
