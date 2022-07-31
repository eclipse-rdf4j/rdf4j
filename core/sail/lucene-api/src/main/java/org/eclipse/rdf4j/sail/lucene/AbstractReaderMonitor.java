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
package org.eclipse.rdf4j.sail.lucene;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ReaderMonitor holds IndexReader and IndexSearcher. When ReaderMonitor is closed it do not close IndexReader and
 * IndexSearcher as long as someone reads from them. Variable readingCount remember how many times it was read.
 *
 * @author Tomasz Trela, DFKI Gmbh
 */
public abstract class AbstractReaderMonitor {

	private final AtomicInteger readingCount = new AtomicInteger(0);

	private final AtomicBoolean doClose = new AtomicBoolean(false);

	// Remember index to be able to remove itself from the index list
	private final AbstractLuceneIndex index;

	private final AtomicBoolean closed = new AtomicBoolean(false);

	protected AbstractReaderMonitor(AbstractLuceneIndex index) {
		this.index = index;
	}

	public final int getReadingCount() {
		return readingCount.get();
	}

	/**
	 *
	 */
	public final synchronized void beginReading() {
		if (closed.get()) {
			throw new IllegalStateException("Cannot begin reading as we have been closed.");
		}
		// We cannot allow any more readers to be open at this stage, as any
		// decrements towards zero on readingCount could trigger closure/removal
		if (doClose.get()) {
			throw new IllegalStateException("Cannot begin reading as we have moved into closing stages.");
		}
		readingCount.incrementAndGet();
	}

	/**
	 * called by the iterator
	 *
	 * @throws IOException
	 */
	public final synchronized void endReading() throws IOException {
		if (readingCount.decrementAndGet() <= 0 && doClose.get()) {
			// when endReading is called on CurrentMonitor and it should be
			// closed, close it
			close();
			// close Lucene index remove them self from Lucene index
			synchronized (index.oldmonitors) {
				// if its not in the list, then this is a no-operation
				index.oldmonitors.remove(this);
			}
		}
	}

	/**
	 * This method is called in LecenIndex invalidateReaders or on commit
	 *
	 * @return <code>true</code> if the close succeeded, <code>false</code> otherwise.
	 * @throws IOException
	 */
	public final synchronized boolean closeWhenPossible() throws IOException {
		doClose.set(true);
		if (readingCount.get() == 0) {
			close();
		}
		return closed.get();
	}

	public final void close() throws IOException {
		if (closed.compareAndSet(false, true)) {
			handleClose();
		}
	}

	/**
	 * This method is thread-safe (i.e. it is not called concurrently).
	 *
	 * @throws IOException
	 */
	protected abstract void handleClose() throws IOException;
}
