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
package org.eclipse.rdf4j.common.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests that NioFile loops to complete full writes/reads even when the underlying FileChannel performs partial IO.
 */
public class NioFilePartialIOTest {

	@TempDir
	File tmp;

	@Test
	public void writeBytesLoopsOnPartialWrite() throws Exception {
		Path p = tmp.toPath().resolve("wbytes.dat");
		NioFile nf = new NioFile(p.toFile());
		injectChoppyChannel(nf, 1); // every write call becomes partial

		byte[] data = new byte[64 * 1024 + 123];
		new Random(42).nextBytes(data);
		nf.writeBytes(data, 0);
		nf.close();

		byte[] readBack = Files.readAllBytes(p);
		assertArrayEquals(data, readBack);
	}

	@Test
	public void writePrimitivesLoopOnPartialWrite() throws Exception {
		Path p = tmp.toPath().resolve("wprim.dat");
		NioFile nf = new NioFile(p.toFile());
		injectChoppyChannel(nf, 1);

		nf.writeByte((byte) 0x7F, 0);
		nf.writeInt(0xCAFEBABE, 1);
		nf.writeLong(0x0123456789ABCDEFL, 1 + 4);
		nf.close();

		try (FileChannel fc = FileChannel.open(p, StandardOpenOption.READ)) {
			ByteBuffer buf = ByteBuffer.allocate(1 + 4 + 8);
			fc.read(buf);
			buf.rewind();
			assertEquals((byte) 0x7F, buf.get());
			assertEquals(0xCAFEBABE, buf.getInt());
			assertEquals(0x0123456789ABCDEFL, buf.getLong());
		}
	}

	@Test
	public void readBytesLoopsOnPartialRead() throws Exception {
		Path p = tmp.toPath().resolve("rbytes.dat");
		byte[] data = new byte[32 * 1024 + 17];
		new Random(24).nextBytes(data);
		Files.write(p, data);

		NioFile nf = new NioFile(p.toFile());
		injectChoppyChannel(nf, 1, true, false); // partial reads only

		byte[] got = nf.readBytes(0, data.length);
		nf.close();
		assertArrayEquals(data, got);
	}

	@Test
	public void readPrimitivesLoopOnPartialRead() throws Exception {
		Path p = tmp.toPath().resolve("rprim.dat");
		try (FileChannel fc = FileChannel.open(p, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
			ByteBuffer buf = ByteBuffer.allocate(1 + 4 + 8);
			buf.put((byte) 0x12);
			buf.putInt(0xDEADBEEF);
			buf.putLong(0x0F1E2D3C4B5A6978L);
			buf.rewind();
			fc.write(buf);
		}

		NioFile nf = new NioFile(p.toFile());
		injectChoppyChannel(nf, 1, true, false);

		assertEquals((byte) 0x12, nf.readByte(0));
		assertEquals(0xDEADBEEF, nf.readInt(1));
		assertEquals(0x0F1E2D3C4B5A6978L, nf.readLong(1 + 4));
		nf.close();
	}

	@Test
	public void writeBytesResumesAfterChannelClose() throws Exception {
		Path p = tmp.toPath().resolve("wbytes-close.dat");
		byte[] data = new byte[4096 + 37];
		new Random(11).nextBytes(data);

		try (NioFile nf = new NioFile(p.toFile())) {
			injectClosingChannel(nf, 64, false, true);
			nf.writeBytes(data, 0);
		}

		byte[] readBack = Files.readAllBytes(p);
		assertArrayEquals(data, readBack);
	}

	@Test
	public void readBytesResumesAfterChannelClose() throws Exception {
		Path p = tmp.toPath().resolve("rbytes-close.dat");
		byte[] data = new byte[2048 + 19];
		new Random(29).nextBytes(data);
		Files.write(p, data);

		byte[] got;
		try (NioFile nf = new NioFile(p.toFile())) {
			injectClosingChannel(nf, 32, true, false);
			got = nf.readBytes(0, data.length);
		}

		assertArrayEquals(data, got);
	}

	// --- helpers ---

	private static void injectChoppyChannel(NioFile nf, int frequency) {
		injectChoppyChannel(nf, frequency, false, false);
	}

	private static void injectChoppyChannel(NioFile nf, int frequency, boolean choppyRead, boolean choppyWrite) {
		try {
			Field fcField = NioFile.class.getDeclaredField("fc");
			fcField.setAccessible(true);
			FileChannel original = (FileChannel) fcField.get(nf);
			fcField.set(nf, new ChoppyFileChannel(original, frequency, choppyRead, choppyWrite));
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private static void injectClosingChannel(NioFile nf, int partialSize, boolean closeOnRead, boolean closeOnWrite) {
		try {
			Field fcField = NioFile.class.getDeclaredField("fc");
			fcField.setAccessible(true);
			FileChannel original = (FileChannel) fcField.get(nf);
			fcField.set(nf, new ClosingFileChannel(original, partialSize, closeOnRead, closeOnWrite));
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	static class ChoppyFileChannel extends FileChannel {
		private final FileChannel delegate;
		private final int frequency;
		private final boolean choppyRead;
		private final boolean choppyWrite;
		private int op;

		ChoppyFileChannel(FileChannel delegate, int frequency, boolean choppyRead, boolean choppyWrite) {
			this.delegate = delegate;
			this.frequency = Math.max(1, frequency);
			this.choppyRead = choppyRead;
			this.choppyWrite = choppyWrite;
		}

		@Override
		public int write(ByteBuffer src, long position) throws IOException {
			op++;
			if (choppyWrite && op % frequency == 0) {
				int rem = src.remaining();
				int part = Math.max(1, rem / 2);
				ByteBuffer slice = src.slice();
				slice.limit(part);
				int n = delegate.write(slice, position);
				src.position(src.position() + n);
				return n;
			}
			return delegate.write(src, position);
		}

		@Override
		public int read(ByteBuffer dst, long position) throws IOException {
			op++;
			if (choppyRead && op % frequency == 0) {
				int want = dst.remaining();
				int part = Math.max(1, want / 2);
				ByteBuffer slice = ByteBuffer.allocate(part);
				int n = delegate.read(slice, position);
				slice.rewind();
				if (n > 0) {
					int lim = Math.min(n, part);
					dst.put((ByteBuffer) slice.limit(lim));
				}
				return n;
			}
			return delegate.read(dst, position);
		}

		// Straight delegates for other abstract methods
		@Override
		public int write(ByteBuffer src) throws IOException {
			return delegate.write(src);
		}

		@Override
		public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
			return delegate.write(srcs, offset, length);
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
			delegate.force(metaData);
		}

		@Override
		public long transferTo(long position, long count, java.nio.channels.WritableByteChannel target)
				throws IOException {
			return delegate.transferTo(position, count, target);
		}

		@Override
		public long transferFrom(java.nio.channels.ReadableByteChannel src, long position, long count)
				throws IOException {
			return delegate.transferFrom(src, position, count);
		}

		@Override
		public java.nio.MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
			return delegate.map(mode, position, size);
		}

		@Override
		public java.nio.channels.FileLock lock(long position, long size, boolean shared) throws IOException {
			return delegate.lock(position, size, shared);
		}

		@Override
		public java.nio.channels.FileLock tryLock(long position, long size, boolean shared) throws IOException {
			return delegate.tryLock(position, size, shared);
		}

		@Override
		protected void implCloseChannel() throws IOException {
			delegate.close();
		}
	}

	static class ClosingFileChannel extends FileChannel {
		private final FileChannel delegate;
		private final int partialSize;
		private final boolean closeOnRead;
		private final boolean closeOnWrite;
		private final AtomicBoolean readClosed = new AtomicBoolean();
		private final AtomicBoolean writeClosed = new AtomicBoolean();

		ClosingFileChannel(FileChannel delegate, int partialSize, boolean closeOnRead, boolean closeOnWrite) {
			this.delegate = delegate;
			this.partialSize = Math.max(1, partialSize);
			this.closeOnRead = closeOnRead;
			this.closeOnWrite = closeOnWrite;
		}

		@Override
		public int write(ByteBuffer src, long position) throws IOException {
			if (closeOnWrite && writeClosed.compareAndSet(false, true)) {
				int requested = Math.min(partialSize, src.remaining());
				ByteBuffer slice = src.slice();
				slice.limit(requested);
				int written = delegate.write(slice, position);
				src.position(src.position() + written);
				if (written > 0) {
					doClose();
					throw new ClosedChannelException();
				}
				writeClosed.set(false);
			}
			ensureOpen();
			return delegate.write(src, position);
		}

		@Override
		public int read(ByteBuffer dst, long position) throws IOException {
			if (closeOnRead && readClosed.compareAndSet(false, true)) {
				int requested = Math.min(partialSize, dst.remaining());
				ByteBuffer tmp = ByteBuffer.allocate(requested);
				int n = delegate.read(tmp, position);
				if (n > 0) {
					tmp.flip();
					tmp.limit(n);
					dst.put(tmp);
					doClose();
					throw new ClosedChannelException();
				}
				readClosed.set(false);
				return n;
			}
			ensureOpen();
			return delegate.read(dst, position);
		}

		private void ensureOpen() throws ClosedChannelException {
			if (!delegate.isOpen() || !isOpen()) {
				throw new ClosedChannelException();
			}
		}

		private void doClose() throws IOException {
			// close wrapper first so AbstractInterruptibleChannel marks it as closed
			if (isOpen()) {
				close();
			} else if (delegate.isOpen()) {
				delegate.close();
			}
		}

		@Override
		public int write(ByteBuffer src) throws IOException {
			ensureOpen();
			return delegate.write(src);
		}

		@Override
		public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
			ensureOpen();
			return delegate.write(srcs, offset, length);
		}

		@Override
		public int read(ByteBuffer dst) throws IOException {
			ensureOpen();
			return delegate.read(dst);
		}

		@Override
		public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
			ensureOpen();
			return delegate.read(dsts, offset, length);
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
			delegate.force(metaData);
		}

		@Override
		public long transferTo(long position, long count, java.nio.channels.WritableByteChannel target)
				throws IOException {
			return delegate.transferTo(position, count, target);
		}

		@Override
		public long transferFrom(java.nio.channels.ReadableByteChannel src, long position, long count)
				throws IOException {
			return delegate.transferFrom(src, position, count);
		}

		@Override
		public java.nio.MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
			return delegate.map(mode, position, size);
		}

		@Override
		public java.nio.channels.FileLock lock(long position, long size, boolean shared) throws IOException {
			return delegate.lock(position, size, shared);
		}

		@Override
		public java.nio.channels.FileLock tryLock(long position, long size, boolean shared) throws IOException {
			return delegate.tryLock(position, size, shared);
		}

		@Override
		protected void implCloseChannel() throws IOException {
			delegate.close();
		}
	}
}
