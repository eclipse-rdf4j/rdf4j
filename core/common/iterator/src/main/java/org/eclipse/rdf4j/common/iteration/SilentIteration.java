/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.common.iteration;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link IterationWrapper} that silently ignores any errors that occur during processing.
 *
 * @author Jeen Broekstra
 */
public class SilentIteration<T> extends IterationWrapper<T> {

	private static final Logger logger = LoggerFactory.getLogger(SilentIteration.class);

	public SilentIteration(CloseableIteration<T> iter) {
		super(iter);
	}

	@Override
	public boolean hasNext() {
		try {
			return super.hasNext();
		} catch (Exception e) {
			if (logger.isTraceEnabled()) {
				logger.trace("Suppressed error in SILENT iteration: " + e.getMessage(), e);
			}
		}
		return false;
	}

	@Override
	public T next() {
		try {
			return super.next();
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

	@Override
	protected void handleClose() {
		try {
			super.handleClose();
		} catch (Exception e) {
			if (logger.isTraceEnabled()) {
				logger.trace("Suppressed error in SILENT iteration: " + e.getMessage(), e);
			}
		}
	}
}
