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
package org.eclipse.rdf4j.sail.nativerdf.datastore;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.io.NioFile;

/**
 * Class supplying access to a data file. A data file stores data sequentially. Each entry starts with the entry's
 * length (4 bytes), followed by the data itself. File offsets are used to identify entries.
 *
 * @author Arjohn Kampman
 */
public class DataFile implements Closeable {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * Magic number "Native Data File" to detect whether the file is actually a data file. The first three bytes of the
	 * file should be equal to this magic number.
	 */
	private static final byte[] MAGIC_NUMBER = new byte[] { 'n', 'd', 'f' };

	/**
	 * File format version, stored as the fourth byte in data files.
	 */
	private static final byte FILE_FORMAT_VERSION = 1;

	private static final long HEADER_LENGTH = MAGIC_NUMBER.length + 1;

	/*-----------*
	 * Variables *
	 *-----------*/

	private final NioFile nioFile;

	private final boolean forceSync;

	// cached file size, also reflects buffer usage
	private volatile long nioFileSize;

	// 4KB write buffer that is flushed on sync, close and any read operations
	private final ByteBuffer buffer = ByteBuffer.allocate(4 * 1024);

	/*--------------*
	 * Constructors *
	 *--------------*/

	public DataFile(File file) throws IOException {
		this(file, false);
	}

	public DataFile(File file, boolean forceSync) throws IOException {
		this.nioFile = new NioFile(file);
		this.forceSync = forceSync;

		try {
			// Open a read/write channel to the file

			if (nioFile.size() == 0) {
				// Empty file, write header
				nioFile.writeBytes(MAGIC_NUMBER, 0);
				nioFile.writeByte(FILE_FORMAT_VERSION, MAGIC_NUMBER.length);

				sync();
			} else if (nioFile.size() < HEADER_LENGTH) {
				throw new IOException("File too small to be a compatible data file");
			} else {
				// Verify file header
				if (!Arrays.equals(MAGIC_NUMBER, nioFile.readBytes(0, MAGIC_NUMBER.length))) {
					throw new IOException("File doesn't contain compatible data records");
				}

				byte version = nioFile.readByte(MAGIC_NUMBER.length);
				if (version > FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read data file; it uses a newer file format");
				} else if (version != FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read data file; invalid file format version: " + version);
				}
			}
		} catch (IOException e) {
			this.nioFile.close();
			throw e;
		}

		this.nioFileSize = nioFile.size();

	}

	/*---------*
	 * Methods *
	 *---------*/

	public File getFile() {
		return nioFile.getFile();
	}

	/**
	 * Stores the specified data and returns the byte-offset at which it has been stored.
	 *
	 * @param data The data to store, must not be <var>null</var>.
	 * @return The byte-offset in the file at which the data was stored.
	 */
	public long storeData(byte[] data) throws IOException {
		assert data != null : "data must not be null";

		long offset = nioFileSize;

		if (data.length + 4 > buffer.capacity()) {
			// direct write because we are writing more data than the buffer can hold

			flush();

			// TODO: two writes could be more efficient since it prevent array copies
			ByteBuffer buf = ByteBuffer.allocate(data.length + 4);
			buf.putInt(data.length);
			buf.put(data);
			buf.rewind();

			nioFile.write(buf, offset);

			nioFileSize += buf.array().length;

		} else {
			if (data.length + 4 > remainingBufferCapacity()) {
				flush();
			}

			buffer.putInt(data.length);
			buffer.put(data);
			nioFileSize += data.length + 4;
		}

		return offset;

	}

	synchronized private void flush() throws IOException {
		int position = buffer.position();
		if (position == 0) {
			return;
		}
		buffer.position(0);

		byte[] byteToWrite = new byte[position];
		buffer.get(byteToWrite, 0, position);

		nioFile.write(ByteBuffer.wrap(byteToWrite), nioFileSize - byteToWrite.length);

		buffer.position(0);

	}

	private int remainingBufferCapacity() {
		return buffer.capacity() - buffer.position();
	}

	// This variable is used for predicting the number of bytes to read in getData(long offset). This helps us to only
	// need to execute a single IO read instead of first one read to find the length and then one read to read the data.
	int dataLengthApproximateAverage = 25;

	/**
	 * Gets the data that is stored at the specified offset.
	 *
	 * @param offset An offset in the data file, must be larger than 0.
	 * @return The data that was found on the specified offset.
	 * @exception IOException If an I/O error occurred.
	 */
	public byte[] getData(long offset) throws IOException {
		assert offset > 0 : "offset must be larger than 0, is: " + offset;
		flush();

		// Read in twice the average length because multiple small read operations take more time than one single larger
		// operation even if that larger operation is unnecessarily large (within sensible limits).
		byte[] data = new byte[(dataLengthApproximateAverage * 2) + 4];
		ByteBuffer buf = ByteBuffer.wrap(data);
		nioFile.read(buf, offset);

		int dataLength = (data[0] << 24) & 0xff000000 |
				(data[1] << 16) & 0x00ff0000 |
				(data[2] << 8) & 0x0000ff00 |
				(data[3]) & 0x000000ff;

		// We have either managed to read enough data and can return the required subset of the data, or we have read
		// too little so we need to execute another read to get the correct data.
		if (dataLength <= data.length - 4) {

			// adjust the approximate average with 1 part actual length and 99 parts previous average up to a sensible
			// max of 200
			dataLengthApproximateAverage = (int) (Math.min(200,
					((dataLengthApproximateAverage / 100.0) * 99) + (dataLength / 100.0)));

			return Arrays.copyOfRange(data, 4, dataLength + 4);

		} else {

			// adjust the approximate average, but favour the actual dataLength since dataLength predictions misses are
			// costly
			dataLengthApproximateAverage = Math.min(200, (dataLengthApproximateAverage + dataLength) / 2);

			// we didn't read enough data so we need to execute a new read
			data = new byte[dataLength];
			buf = ByteBuffer.wrap(data);
			nioFile.read(buf, offset + 4L);

			return data;
		}

	}

	/**
	 * Discards all stored data.
	 *
	 * @throws IOException If an I/O error occurred.
	 */
	public void clear() throws IOException {
		nioFile.truncate(HEADER_LENGTH);
		nioFileSize = HEADER_LENGTH;
		buffer.clear();
	}

	/**
	 * Syncs any unstored data to the hash file.
	 */
	public void sync() throws IOException {
		flush();

		if (forceSync) {
			nioFile.force(false);
		}
	}

	public void sync(boolean force) throws IOException {
		flush();

		nioFile.force(force);
	}

	/**
	 * Closes the data file, releasing any file locks that it might have.
	 *
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		flush();

		nioFile.close();
	}

	/**
	 * Gets an iterator that can be used to iterate over all stored data.
	 *
	 * @return a DataIterator.
	 */
	public DataIterator iterator() {
		try {
			flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return new DataIterator();
	}

	/**
	 * An iterator that iterates over the data that is stored in a data file.
	 */
	public class DataIterator {

		private long position = HEADER_LENGTH;

		public boolean hasNext() throws IOException {
			return position < nioFileSize;
		}

		public byte[] next() throws IOException {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			byte[] data = getData(position);
			position += (4 + data.length);
			return data;
		}
	}
}
