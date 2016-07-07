/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ReaderMonitor holds IndexReader and IndexSearcher. When ReaderMonitor is closed it do not close IndexReader
 * and IndexSearcher as long as someone reads from them. Variable readingCount remember how many times it was
 * read.
 * 
 * @author Tomasz Trela, DFKI Gmbh
 */
public abstract class AbstractReaderMonitor {

	private final AtomicInteger readingCount = new AtomicInteger(0);

	private boolean doClose = false;

	// Remember index to be able to remove itself from the index list
	final private AbstractLuceneIndex index;

	private final AtomicBoolean closed = new AtomicBoolean(false);

	protected AbstractReaderMonitor(AbstractLuceneIndex index) {
		this.index = index;
	}

	public int getReadingCount() {
		return readingCount.get();
	}

	/**
	 * 
	 */
	public void beginReading() {
		readingCount.incrementAndGet();
	}

	/**
	 * called by the iterator
	 * 
	 * @throws IOException
	 */
	public void endReading()
		throws IOException
	{
		if (readingCount.decrementAndGet() == 0 && doClose) {
			// when endReading is called on CurrentMonitor and it should be closed,
			// close it
			close();// close Lucene index remove them self from Lucene index
			synchronized (index.oldmonitors) {
				index.oldmonitors.remove(this); // if its not in the list, then this
												// is a no-operation
			}
		}
	}

	/**
	 * This method is called in LecenIndex invalidateReaders or on commit
	 * 
	 * @return <code>true</code> if the close succeeded, <code>false</code> otherwise.
	 * @throws IOException
	 */
	public boolean closeWhenPossible()
		throws IOException
	{
		doClose = true;
		if (readingCount.get() == 0) {
			close();
		}
		return closed.get();
	}

	public void close()
		throws IOException
	{
		if (!closed.getAndSet(true)) {
			handleClose();
		}
	}

	/**
	 * @throws IOException
	 */
	protected abstract void handleClose()
		throws IOException;
}
