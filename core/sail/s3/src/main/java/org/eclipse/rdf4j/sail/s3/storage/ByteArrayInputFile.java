/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3.storage;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;

/**
 * An {@link InputFile} implementation that reads Parquet data from an in-memory byte array. This avoids any dependency
 * on Hadoop's file system abstraction.
 */
public class ByteArrayInputFile implements InputFile {

	private final byte[] data;

	/**
	 * Creates a new input file backed by the given byte array.
	 *
	 * @param data the Parquet file content
	 */
	public ByteArrayInputFile(byte[] data) {
		this.data = data;
	}

	@Override
	public long getLength() {
		return data.length;
	}

	@Override
	public SeekableInputStream newStream() {
		return new ByteArraySeekableInputStream(data);
	}

	/**
	 * A {@link SeekableInputStream} backed by a byte array.
	 */
	private static class ByteArraySeekableInputStream extends SeekableInputStream {

		private final byte[] data;
		private int pos;

		ByteArraySeekableInputStream(byte[] data) {
			this.data = data;
			this.pos = 0;
		}

		@Override
		public int read() throws IOException {
			if (pos >= data.length) {
				return -1;
			}
			return data[pos++] & 0xFF;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (pos >= data.length) {
				return -1;
			}
			int available = data.length - pos;
			int toRead = Math.min(len, available);
			System.arraycopy(data, pos, b, off, toRead);
			pos += toRead;
			return toRead;
		}

		@Override
		public long getPos() throws IOException {
			return pos;
		}

		@Override
		public void seek(long newPos) throws IOException {
			if (newPos < 0 || newPos > data.length) {
				throw new IOException("Seek position " + newPos + " is out of range [0, " + data.length + "]");
			}
			this.pos = (int) newPos;
		}

		@Override
		public void readFully(byte[] bytes) throws IOException {
			readFully(bytes, 0, bytes.length);
		}

		@Override
		public void readFully(byte[] bytes, int start, int len) throws IOException {
			int available = data.length - pos;
			if (available < len) {
				throw new EOFException(
						"Reached end of stream: needed " + len + " bytes but only " + available + " available");
			}
			System.arraycopy(data, pos, bytes, start, len);
			pos += len;
		}

		@Override
		public int read(ByteBuffer buf) throws IOException {
			int len = buf.remaining();
			if (len == 0) {
				return 0;
			}
			int available = data.length - pos;
			if (available <= 0) {
				return -1;
			}
			int toRead = Math.min(len, available);
			buf.put(data, pos, toRead);
			pos += toRead;
			return toRead;
		}

		@Override
		public void readFully(ByteBuffer buf) throws IOException {
			int len = buf.remaining();
			int available = data.length - pos;
			if (available < len) {
				throw new EOFException(
						"Reached end of stream: needed " + len + " bytes but only " + available + " available");
			}
			buf.put(data, pos, len);
			pos += len;
		}

		@Override
		public int available() {
			return data.length - pos;
		}
	}
}
