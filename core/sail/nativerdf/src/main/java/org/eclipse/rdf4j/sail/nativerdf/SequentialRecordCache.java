/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.io.NioFile;
import org.eclipse.rdf4j.sail.nativerdf.btree.RecordIterator;

/**
 * A cache for fixed size byte array records. This cache uses a temporary file
 * to store the records. This file is deleted upon calling {@link #discard()}.
 * 
 * @author Arjohn Kampman
 */
final class SequentialRecordCache extends RecordCache {

	/**
	 * Magic number "Sequential Record Cache" to detect whether the file is
	 * actually a sequential record cache file. The first three bytes of the file
	 * should be equal to this magic number.
	 */
	private static final byte[] MAGIC_NUMBER = new byte[] { 's', 'r', 'c' };

	/**
	 * The file format version number, stored as the fourth byte in sequential
	 * record cache files.
	 */
	private static final byte FILE_FORMAT_VERSION = 1;

	private static final int HEADER_LENGTH = MAGIC_NUMBER.length + 1;

	/*------------*
	 * Attributes *
	 *------------*/

	private final NioFile nioFile;

	private final int recordSize;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SequentialRecordCache(File cacheDir, int recordSize)
		throws IOException
	{
		this(cacheDir, recordSize, Long.MAX_VALUE);
	}

	public SequentialRecordCache(File cacheDir, int recordSize, long maxRecords)
		throws IOException
	{
		super(maxRecords);
		this.recordSize = recordSize;

		File cacheFile = File.createTempFile("txncache", ".dat", cacheDir);
		nioFile = new NioFile(cacheFile);

		// Write file header
		nioFile.writeBytes(MAGIC_NUMBER, 0);
		nioFile.writeByte(FILE_FORMAT_VERSION, MAGIC_NUMBER.length);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void discard()
		throws IOException
	{
		nioFile.delete();
	}

	@Override
	protected void clearInternal()
		throws IOException
	{
		nioFile.truncate(HEADER_LENGTH);
	}

	@Override
	protected void storeRecordInternal(byte[] data)
		throws IOException
	{
		nioFile.writeBytes(data, nioFile.size());
	}

	@Override
	protected RecordIterator getRecordsInternal() {
		return new RecordCacheIterator();
	}

	/*---------------------------------*
	 * Inner class RecordCacheIterator *
	 *---------------------------------*/

	protected class RecordCacheIterator implements RecordIterator {

		private long position = HEADER_LENGTH;

		public byte[] next()
			throws IOException
		{
			if (position + recordSize <= nioFile.size()) {
				byte[] data = new byte[recordSize];
				ByteBuffer buf = ByteBuffer.wrap(data);

				int bytesRead = nioFile.read(buf, position);

				if (bytesRead < 0) {
					throw new NoSuchElementException("No more elements available");
				}

				position += bytesRead;

				return data;
			}

			return null;
		}

		public void set(byte[] value)
			throws IOException
		{
			if (position >= HEADER_LENGTH + recordSize && position <= nioFile.size()) {
				nioFile.writeBytes(value, position - recordSize);
			}
		}

		public void close()
			throws IOException
		{
		}
	}
}
