/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.datastore;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.io.NioFile;

/**
 * Class supplying access to a data file. A data file stores data sequentially.
 * Each entry starts with the entry's length (4 bytes), followed by the data
 * itself. File offsets are used to identify entries.
 * 
 * @author Arjohn Kampman
 */
public class DataFile {

	/*-----------*
	 * Constants *
	 *-----------*/

	/**
	 * Magic number "Native Data File" to detect whether the file is actually a
	 * data file. The first three bytes of the file should be equal to this magic
	 * number.
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

	/*--------------*
	 * Constructors *
	 *--------------*/

	public DataFile(File file)
		throws IOException
	{
		this(file, false);
	}

	public DataFile(File file, boolean forceSync)
		throws IOException
	{
		this.nioFile = new NioFile(file);
		this.forceSync = forceSync;

		try {
			// Open a read/write channel to the file

			if (nioFile.size() == 0) {
				// Empty file, write header
				nioFile.writeBytes(MAGIC_NUMBER, 0);
				nioFile.writeByte(FILE_FORMAT_VERSION, MAGIC_NUMBER.length);

				sync();
			}
			else if (nioFile.size() < HEADER_LENGTH) {
				throw new IOException("File too small to be a compatible data file");
			}
			else {
				// Verify file header
				if (!Arrays.equals(MAGIC_NUMBER, nioFile.readBytes(0, MAGIC_NUMBER.length))) {
					throw new IOException("File doesn't contain compatible data records");
				}

				byte version = nioFile.readByte(MAGIC_NUMBER.length);
				if (version > FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read data file; it uses a newer file format");
				}
				else if (version != FILE_FORMAT_VERSION) {
					throw new IOException("Unable to read data file; invalid file format version: " + version);
				}
			}
		}
		catch (IOException e) {
			this.nioFile.close();
			throw e;
		}
	}

	/*---------*
	 * Methods *
	 *---------*/

	public File getFile() {
		return nioFile.getFile();
	}

	/**
	 * Stores the specified data and returns the byte-offset at which it has been
	 * stored.
	 * 
	 * @param data
	 *        The data to store, must not be <tt>null</tt>.
	 * @return The byte-offset in the file at which the data was stored.
	 */
	public long storeData(byte[] data)
		throws IOException
	{
		assert data != null : "data must not be null";

		long offset = nioFile.size();

		// TODO: two writes could be more efficient since it prevent array copies
		ByteBuffer buf = ByteBuffer.allocate(data.length + 4);
		buf.putInt(data.length);
		buf.put(data);
		buf.rewind();

		nioFile.write(buf, offset);

		return offset;
	}

	/**
	 * Gets the data that is stored at the specified offset.
	 * 
	 * @param offset
	 *        An offset in the data file, must be larger than 0.
	 * @return The data that was found on the specified offset.
	 * @exception IOException
	 *            If an I/O error occurred.
	 */
	public byte[] getData(long offset)
		throws IOException
	{
		assert offset > 0 : "offset must be larger than 0, is: " + offset;

		// TODO: maybe get more data in one go is more efficient?
		int dataLength = nioFile.readInt(offset);

		byte[] data = new byte[dataLength];
		ByteBuffer buf = ByteBuffer.wrap(data);
		nioFile.read(buf, offset + 4L);

		return data;
	}

	/**
	 * Discards all stored data.
	 * 
	 * @throws IOException
	 *         If an I/O error occurred.
	 */
	public void clear()
		throws IOException
	{
		nioFile.truncate(HEADER_LENGTH);
	}

	/**
	 * Syncs any unstored data to the hash file.
	 */
	public void sync()
		throws IOException
	{
		if (forceSync) {
			nioFile.force(false);
		}
	}

	/**
	 * Closes the data file, releasing any file locks that it might have.
	 * 
	 * @throws IOException
	 */
	public void close()
		throws IOException
	{
		nioFile.close();
	}

	/**
	 * Gets an iterator that can be used to iterate over all stored data.
	 * 
	 * @return a DataIterator.
	 */
	public DataIterator iterator() {
		return new DataIterator();
	}

	/**
	 * An iterator that iterates over the data that is stored in a data file.
	 */
	public class DataIterator {

		private long position = HEADER_LENGTH;

		public boolean hasNext()
			throws IOException
		{
			return position < nioFile.size();
		}

		public byte[] next()
			throws IOException
		{
			if (!hasNext()) {
				throw new NoSuchElementException();
			}

			byte[] data = getData(position);
			position += (4 + data.length);
			return data;
		}
	}
}
