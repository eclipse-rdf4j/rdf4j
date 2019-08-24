/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.io;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

/**
 * File wrapper that protects against concurrent file closing events due to e.g. {@link Thread#interrupt() thread
 * interrupts}. In case the file channel that is used by this class is closed due to such an event, it will try to
 * reopen the channel. The thread that causes the {@link ClosedByInterruptException} is not protected, assuming the
 * interrupt is intended to end the thread's operation.
 * 
 * @author Arjohn Kampman
 */
public final class NioFile implements Closeable {

	private final File file;

	private final String mode;

	private volatile RandomAccessFile raf;

	private volatile FileChannel fc;

	private volatile boolean explictlyClosed;

	/**
	 * Constructor Opens a file in read/write mode, creating a new one if the file doesn't exist.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public NioFile(File file) throws IOException {
		this(file, "rw");
	}

	/**
	 * Constructor Opens a file in a specific mode, creating a new one if the file doesn't exist.
	 * 
	 * @param file file
	 * @param mode file mode
	 * @throws IOException
	 */
	public NioFile(File file, String mode) throws IOException {
		this.file = file;
		this.mode = mode;

		if (!file.exists()) {
			boolean created = file.createNewFile();
			if (!created) {
				throw new IOException("Failed to create file: " + file);
			}
		}

		explictlyClosed = false;
		open();
	}

	/**
	 * Open a file channel for random access.
	 * 
	 * @throws IOException
	 */
	private void open() throws IOException {
		raf = new RandomAccessFile(file, mode);
		fc = raf.getChannel();
	}

	/**
	 * Reopen a channel closed by an exception, unless it was closed explicitly.
	 * 
	 * @param e exception that closed the channel
	 * @throws IOException
	 */
	private synchronized void reopen(ClosedChannelException e) throws IOException {
		if (explictlyClosed) {
			throw e;
		}
		if (fc.isOpen()) {
			// file channel has been already reopened by another thread
			return;
		}
		open();
	}

	@Override
	public synchronized void close() throws IOException {
		explictlyClosed = true;
		raf.close();
	}

	/**
	 * Check if a file was closed explicitly.
	 * 
	 * @return true if it was closed explicitly
	 */
	public boolean isClosed() {
		return explictlyClosed;
	}

	public File getFile() {
		return file;
	}

	/**
	 * Close any open channels and then deletes the file.
	 * 
	 * @return <tt>true</tt> if the file has been deleted successfully, <tt>false</tt> otherwise.
	 * @throws IOException If there was a problem closing the open file channel.
	 */
	public boolean delete() throws IOException {
		// make sure to close file handles prior to deletion
		close();
		return file.delete();
	}

	/**
	 * Performs a protected {@link FileChannel#force(boolean)} call.
	 * 
	 * @param metaData
	 * @throws IOException
	 */
	public void force(boolean metaData) throws IOException {
		while (true) {
			try {
				fc.force(metaData);
				return;
			} catch (ClosedByInterruptException e) {
				throw e;
			} catch (ClosedChannelException e) {
				reopen(e);
			}
		}
	}

	/**
	 * Performs a protected {@link FileChannel#truncate(long)} call.
	 * 
	 * @param size
	 * @throws IOException
	 */
	public void truncate(long size) throws IOException {
		while (true) {
			try {
				fc.truncate(size);
				return;
			} catch (ClosedByInterruptException e) {
				throw e;
			} catch (ClosedChannelException e) {
				reopen(e);
			}
		}
	}

	/**
	 * Performs a protected {@link FileChannel#size()} call.
	 * 
	 * @return size of the file
	 * @throws IOException
	 */
	public long size() throws IOException {
		while (true) {
			try {
				return fc.size();
			} catch (ClosedByInterruptException e) {
				throw e;
			} catch (ClosedChannelException e) {
				reopen(e);
			}
		}
	}

	/**
	 * Performs a protected {@link FileChannel#transferTo(long, long, WritableByteChannel)} call.
	 * 
	 * @param position position within the file
	 * @param count    number of bytes to transfer
	 * @param target   target channel
	 * @return number of bytes transferred
	 * @throws IOException
	 */
	public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
		while (true) {
			try {
				return fc.transferTo(position, count, target);
			} catch (ClosedByInterruptException e) {
				throw e;
			} catch (ClosedChannelException e) {
				reopen(e);
			}
		}
	}

	/**
	 * Performs a protected {@link FileChannel#write(ByteBuffer, long)} call.
	 * 
	 * @param buf    buffer
	 * @param offset non-negative offset
	 * @return number of bytes written
	 * @throws IOException
	 */
	public int write(ByteBuffer buf, long offset) throws IOException {
		while (true) {
			try {
				return fc.write(buf, offset);
			} catch (ClosedByInterruptException e) {
				throw e;
			} catch (ClosedChannelException e) {
				reopen(e);
			}
		}
	}

	/**
	 * Performs a protected {@link FileChannel#read(ByteBuffer, long)} call.
	 * 
	 * @param buf    buffer to read
	 * @param offset non-negative offset
	 * @return number of bytes read
	 * @throws IOException
	 */
	public int read(ByteBuffer buf, long offset) throws IOException {
		while (true) {
			try {
				return fc.read(buf, offset);
			} catch (ClosedByInterruptException e) {
				throw e;
			} catch (ClosedChannelException e) {
				reopen(e);
			}
		}
	}

	/**
	 * Write byte array to channel starting at offset.
	 * 
	 * @param value  byte array to write
	 * @param offset non-negative offset
	 * @throws IOException
	 */
	public void writeBytes(byte[] value, long offset) throws IOException {
		write(ByteBuffer.wrap(value), offset);
	}

	/**
	 * Read a byte array of a specifi length from channel starting at offset.
	 * 
	 * @param offset
	 * @param length
	 * @return
	 * @throws IOException
	 */
	public byte[] readBytes(long offset, int length) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(length);
		read(buf, offset);
		return buf.array();
	}

	/**
	 * Write single byte to channel starting at offset.
	 * 
	 * @param value  value to write
	 * @param offset non-negative offset
	 * @throws IOException
	 */
	public void writeByte(byte value, long offset) throws IOException {
		writeBytes(new byte[] { value }, offset);
	}

	/**
	 * Read single byte from channel starting at offset.
	 * 
	 * @param offset non-negative offset
	 * @return byte
	 * @throws IOException
	 */
	public byte readByte(long offset) throws IOException {
		return readBytes(offset, 1)[0];
	}

	/**
	 * Write long value to channel starting at offset.
	 * 
	 * @param value  value to write
	 * @param offset non-negative offset
	 * @throws IOException
	 */
	public void writeLong(long value, long offset) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(8);
		buf.putLong(0, value);
		write(buf, offset);
	}

	/**
	 * Read long value from channel starting at offset.
	 * 
	 * @param offset non-negative offset
	 * @return long
	 * @throws IOException
	 */
	public long readLong(long offset) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(8);
		read(buf, offset);
		return buf.getLong(0);
	}

	/**
	 * Write integer value to channel starting at offset.
	 * 
	 * @param value  value to write
	 * @param offset non-negative offset
	 * @throws IOException
	 */
	public void writeInt(int value, long offset) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.putInt(0, value);
		write(buf, offset);
	}

	/**
	 * Read integer value from channel starting at offset.
	 * 
	 * @param offset non-negative offset
	 * @return integer
	 * @throws IOException
	 */
	public int readInt(long offset) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(4);
		read(buf, offset);
		return buf.getInt(0);
	}
}
