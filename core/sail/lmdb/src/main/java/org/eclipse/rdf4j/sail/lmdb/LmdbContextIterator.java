/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import java.io.IOException;

import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A context iterator that wraps a LmdbContextIdIterator and translates IDs to resource objects.
 */
class LmdbContextIterator extends LookAheadIteration<Resource> {

	private final LmdbContextIdIterator contextIdIt;

	private final ValueStore valueStore;

	/**
	 * Creates a new LmdbContextIterator.
	 */
	public LmdbContextIterator(LmdbContextIdIterator contextIdIt, ValueStore valueStore) {
		this.contextIdIt = contextIdIt;
		this.valueStore = valueStore;
	}

	@Override
	public Resource getNextElement() throws SailException {
		try {
			long contextID;
			do {
				long[] record = contextIdIt.next();
				if (record == null) {
					return null;
				}
				contextID = record[0];
			} while (contextID == 0);

			Resource context = (Resource) valueStore.getLazyValue(contextID);
			return context;
		} catch (IOException e) {
			throw causeIOException(e);
		}
	}

	@Override
	protected void handleClose() throws SailException {
		contextIdIt.close();
	}

	private SailException causeIOException(IOException e) {
		return new SailException(e);
	}
}
