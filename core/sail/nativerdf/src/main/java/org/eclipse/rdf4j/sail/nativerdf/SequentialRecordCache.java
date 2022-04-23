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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.common.io.NioFile;
import org.eclipse.rdf4j.sail.nativerdf.btree.RecordIterator;

/**
 * A cache for fixed size byte array records. This cache uses a temporary file to store the records. This file is
 * deleted upon calling {@link #discard()}.
 *
 * @author Arjohn Kampman
 */
final class SequentialRecordCache extends AbstractRecordCache {

	private final static AtomicLong TEMP_FILE_COUNTER = new AtomicLong();
	private final static String TEMP_FILE_PREFIX = "txncache" + UUID.randomUUID().toString().replace("-", "");
	private final static String TEMP_FILE_SUFFIX = ".dat";

	private static final EnumSet<StandardOpenOption> FILE_OPEN_OPTIONS = EnumSet.of(
			StandardOpenOption.READ,
			StandardOpenOption.WRITE,
			StandardOpenOption.CREATE_NEW,
			StandardOpenOption.DELETE_ON_CLOSE
	);

	/**
	 * Magic number "Sequential Record Cache" to detect whether the file is actually a sequential record cache file. The
	 * first three bytes of the file should be equal to this magic number.
	 */
	private static final byte[] MAGIC_NUMBER = { 's', 'r', 'c' };

	/**
	 * The file format version number, stored as the fourth byte in sequential record cache files.
	 */
	private static final byte FILE_FORMAT_VERSION = 1;

	// 3 bytes for magic number and 1 byte for file format version
	private static final int HEADER_LENGTH = MAGIC_NUMBER.length + 1;

	/*------------*
	 * Attributes *
	 *------------*/

	private final int recordSize;
	private long currentSize;
	private long extendedSize;

	private final NioFile nioFile;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public SequentialRecordCache(File cacheDir, int recordSize) throws IOException {
		this(cacheDir, recordSize, Long.MAX_VALUE);
	}

	public SequentialRecordCache(File cacheDir, int recordSize, long maxRecords) throws IOException {
		super(maxRecords);
		this.recordSize = recordSize;

		Path path = Paths.get(cacheDir.getCanonicalPath(),
				TEMP_FILE_PREFIX + TEMP_FILE_COUNTER.incrementAndGet() + TEMP_FILE_SUFFIX);

		this.nioFile = new NioFile(path, FILE_OPEN_OPTIONS);

		// Write file header
		append(MAGIC_NUMBER);
		append(new byte[] { FILE_FORMAT_VERSION });
	}

	private void append(byte[] data) throws IOException {
		if (currentSize >= extendedSize - recordSize) {
			long newExtendedSize = (((currentSize + recordSize) / BLOCK_SIZE) + 1) * BLOCK_SIZE;
			assert newExtendedSize > extendedSize;
			int bytesToWrite = (int) (newExtendedSize - extendedSize);
			nioFile.writeBytes(new byte[bytesToWrite], currentSize);
			extendedSize += bytesToWrite;
		}

		nioFile.writeBytes(data, currentSize);
		currentSize += data.length;
	}

	@Override
	public void discard() throws IOException {
		nioFile.delete();
	}

	@Override
	protected void clearInternal() throws IOException {
		nioFile.truncate(HEADER_LENGTH);
		currentSize = nioFile.size();
		extendedSize = currentSize;
	}

	@Override
	protected void storeRecordInternal(byte[] data) throws IOException {
		append(data);
	}

	@Override
	protected RecordIterator getRecordsInternal() throws IOException {
		return new RecordCacheIterator();
	}

	/*---------------------------------*
	 * Inner class RecordCacheIterator *
	 *---------------------------------*/

	protected class RecordCacheIterator implements RecordIterator {

		private long position = HEADER_LENGTH;
		private long readAheadBufferPosition = HEADER_LENGTH;

		private final ByteBuffer readAheadBuffer;

		{
			readAheadBuffer = ByteBuffer.allocate(BLOCK_SIZE + recordSize);
			readAheadBuffer.position(readAheadBuffer.limit());
		}

		private void fillReadAheadBuffer() throws IOException {
			while (readAheadBuffer.limit() - readAheadBuffer.position() < recordSize
					&& currentSize > readAheadBufferPosition) {
				readAheadBuffer.compact();

				// align with 4K boundary
				if (readAheadBufferPosition == HEADER_LENGTH) {
					readAheadBuffer.limit(readAheadBuffer.limit() - HEADER_LENGTH);
				} else if (readAheadBufferPosition % BLOCK_SIZE != 0) {
					readAheadBuffer.limit(
							BLOCK_SIZE - (int) (readAheadBufferPosition % BLOCK_SIZE) + readAheadBuffer.position());
				} else {
					readAheadBuffer.limit(BLOCK_SIZE + readAheadBuffer.position());
				}

				int read = nioFile.read(readAheadBuffer, readAheadBufferPosition);
				readAheadBufferPosition += read;
				readAheadBuffer.flip();

				// override the limit if we have read outside the actual size (e.g. into the extended area).
				if (readAheadBufferPosition > currentSize) {
					readAheadBuffer.limit((int) (readAheadBuffer.limit() - (readAheadBufferPosition - currentSize)));
					readAheadBufferPosition = currentSize;
				}

				if (read <= 0) {
					break;
				}

			}

		}

		@Override
		public byte[] next() throws IOException {
			fillReadAheadBuffer();
			if (!readAheadBuffer.hasRemaining()) {
				return null;
			}

			byte[] bytes = new byte[recordSize];

			readAheadBuffer.get(bytes);

			position += recordSize;
			return bytes;
		}

		@Override
		public void set(byte[] value) throws IOException {
			if (position >= HEADER_LENGTH + recordSize && position <= currentSize) {
				nioFile.writeBytes(value, position - recordSize);
			}

		}

		@Override
		public void close() throws IOException {
		}
	}
}
