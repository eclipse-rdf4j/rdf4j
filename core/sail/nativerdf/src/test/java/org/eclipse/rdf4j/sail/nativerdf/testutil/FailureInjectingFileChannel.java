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
package org.eclipse.rdf4j.sail.nativerdf.testutil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Delegating FileChannel that can simulate failures for testing.
 */
public class FailureInjectingFileChannel extends FileChannel {

	private final FileChannel delegate;

	// simple toggles for simulation
	private volatile boolean failNextWrite;
	private volatile boolean failNextForce;

	public FailureInjectingFileChannel(FileChannel delegate) {
		this.delegate = delegate;
	}

	public void setFailNextWrite(boolean fail) {
		this.failNextWrite = fail;
	}

	public void setFailNextForce(boolean fail) {
		this.failNextForce = fail;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		return delegate.read(dst);
	}

	@Override
	public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
		return delegate.read(dsts, offset, length);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		if (failNextWrite) {
			failNextWrite = false;
			throw new IOException("Simulated write failure");
		}
		return delegate.write(src);
	}

	@Override
	public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
		if (failNextWrite) {
			failNextWrite = false;
			throw new IOException("Simulated write failure");
		}
		return delegate.write(srcs, offset, length);
	}

	@Override
	public long position() throws IOException {
		return delegate.position();
	}

	@Override
	public FileChannel position(long newPosition) throws IOException {
		delegate.position(newPosition);
		return this;
	}

	@Override
	public long size() throws IOException {
		return delegate.size();
	}

	@Override
	public FileChannel truncate(long size) throws IOException {
		delegate.truncate(size);
		return this;
	}

	@Override
	public void force(boolean metaData) throws IOException {
		if (failNextForce) {
			failNextForce = false;
			throw new IOException("Simulated force failure");
		}
		delegate.force(metaData);
	}

	@Override
	public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
		return delegate.transferTo(position, count, target);
	}

	@Override
	public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
		return delegate.transferFrom(src, position, count);
	}

	@Override
	public int read(ByteBuffer dst, long position) throws IOException {
		return delegate.read(dst, position);
	}

	@Override
	public int write(ByteBuffer src, long position) throws IOException {
		if (failNextWrite) {
			failNextWrite = false;
			throw new IOException("Simulated write failure");
		}
		return delegate.write(src, position);
	}

	@Override
	protected void implCloseChannel() throws IOException {
		delegate.close();
	}

	@Override
	public FileLock lock(long position, long size, boolean shared) throws IOException {
		return delegate.lock(position, size, shared);
	}

	@Override
	public FileLock tryLock(long position, long size, boolean shared) throws IOException {
		return delegate.tryLock(position, size, shared);
	}

	@Override
	public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
		return delegate.map(mode, position, size);
	}
}
