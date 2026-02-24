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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;

/**
 * An {@link OutputFile} implementation that writes Parquet data to an in-memory byte array. This avoids any dependency
 * on Hadoop's file system abstraction.
 *
 * <p>
 * After writing is complete, call {@link #toByteArray()} to retrieve the serialized Parquet bytes.
 */
public class ByteArrayOutputFile implements OutputFile {

	private ByteArrayOutputStream baos;

	@Override
	public PositionOutputStream create(long blockSizeHint) throws IOException {
		baos = new ByteArrayOutputStream();
		return new ByteArrayPositionOutputStream(baos);
	}

	@Override
	public PositionOutputStream createOrOverwrite(long blockSizeHint) throws IOException {
		return create(blockSizeHint);
	}

	@Override
	public boolean supportsBlockSize() {
		return false;
	}

	@Override
	public long defaultBlockSize() {
		return 0;
	}

	/**
	 * Returns the bytes written to this output file.
	 *
	 * @return the Parquet file content as a byte array
	 * @throws IllegalStateException if no data has been written yet
	 */
	public byte[] toByteArray() {
		if (baos == null) {
			throw new IllegalStateException("No data has been written");
		}
		return baos.toByteArray();
	}

	/**
	 * A {@link PositionOutputStream} backed by a {@link ByteArrayOutputStream}.
	 */
	private static class ByteArrayPositionOutputStream extends PositionOutputStream {

		private final ByteArrayOutputStream baos;
		private long pos;

		ByteArrayPositionOutputStream(ByteArrayOutputStream baos) {
			this.baos = baos;
			this.pos = 0;
		}

		@Override
		public long getPos() {
			return pos;
		}

		@Override
		public void write(int b) throws IOException {
			baos.write(b);
			pos++;
		}

		@Override
		public void write(byte[] b) throws IOException {
			baos.write(b);
			pos += b.length;
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			baos.write(b, off, len);
			pos += len;
		}

		@Override
		public void flush() throws IOException {
			baos.flush();
		}

		@Override
		public void close() throws IOException {
			baos.close();
		}
	}
}
