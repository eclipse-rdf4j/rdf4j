/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.btree;

import java.io.IOException;

/**
 * An iterator that iterates over records, for example those in a BTree.
 * 
 * @see BTree
 * @author Arjohn Kampman
 */
public interface RecordIterator {

	/**
	 * Returns the next record in the BTree.
	 * 
	 * @return A record that is stored in the BTree, or <tt>null</tt> if all
	 *         records have been returned.
	 * @exception IOException
	 *            In case an I/O error occurred.
	 */
	public byte[] next()
		throws IOException;

	/**
	 * Replaces the last record returned by {@link #next} with the specified
	 * record.
	 * 
	 * @exception IOException
	 *            In case an I/O error occurred.
	 */
	public void set(byte[] record)
		throws IOException;

	/**
	 * Closes the iterator, freeing any resources that it uses. Once closed, the
	 * iterator will not return any more records.
	 * 
	 * @exception IOException
	 *            In case an I/O error occurred.
	 */
	public void close()
		throws IOException;
}
