/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.memory;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.LookAheadIteration;
import org.eclipse.rdf4j.sail.memory.model.MemStatement;
import org.eclipse.rdf4j.sail.memory.model.MemStatementIterator;

/**
 * A wrapper for a MemStatementIterator that checks if the iterator should be cached and retrieves the cached one if
 * that is the case.
 *
 * @author Håvard M. Ottestad
 */

public class CachingMemStatementIteration<X extends Exception> extends LookAheadIteration<MemStatement, X> {

	private final MemorySailStore memorySailStore;
	private final CloseableIteration<MemStatement, X> iterator;
	private final boolean usesCache;
	private Exception e;

	public CachingMemStatementIteration(MemStatementIterator<X> iterator, MemorySailStore memorySailStore) {
		if (memorySailStore.shouldBeCached(iterator.getMinimal())) {
			CloseableIteration<MemStatement, X> cachedIterator = null;
			try {
				cachedIterator = memorySailStore.cacheIterator(iterator);
			} catch (Exception e) {
				this.e = e;
			} finally {
				try {
					iterator.close();
				} catch (Exception ex) {
					this.e = ex;
				}
			}
			this.iterator = cachedIterator;
			this.usesCache = true;
		} else {
			this.iterator = iterator;
			this.usesCache = false;
		}
		this.memorySailStore = memorySailStore;
	}

	@Override
	protected MemStatement getNextElement() throws X {
		if (e != null) {
			throw ((X) e);
		}
		if (iterator.hasNext()) {
			return iterator.next();
		}
		return null;
	}

	@Override
	protected void handleClose() throws X {
		try {
			super.handleClose();
		} finally {
			try {
				iterator.close();
			} finally {
				if (!usesCache && ((MemStatementIterator<X>) iterator).considerForCaching()) {
					memorySailStore.incrementIteratorFrequencyMap(((MemStatementIterator<X>) iterator).getMinimal());
				}
			}
		}
	}
}
