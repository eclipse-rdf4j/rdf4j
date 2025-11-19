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

package org.eclipse.rdf4j.sail.nativerdf.wal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Proves that rotating a WAL segment must force the previous segment to disk before closing it. This test wraps the
 * writer's FileChannel with a tracking wrapper and invokes the private rotate method via reflection.
 */
class ValueStoreWALForceOnRotateTest {

	@TempDir
	Path tempDir;

	@Test
	void rotationForcesPreviousSegment() throws Exception {
		Path walDir = tempDir.resolve(ValueStoreWalConfig.DEFAULT_DIRECTORY_NAME);
		Files.createDirectories(walDir);

		ValueStoreWalConfig cfg = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.syncPolicy(ValueStoreWalConfig.SyncPolicy.COMMIT)
				.storeUuid(UUID.randomUUID().toString())
				.maxSegmentBytes(1 << 20) // 1MB; size irrelevant since we call rotate directly
				.build();

		try (ValueStoreWAL wal = ValueStoreWAL.open(cfg)) {
			// Mint a single record to ensure lastAppendedLsn > lastForcedLsn so a force would be required.
			long lsn = wal.logMint(1, ValueStoreWalValueKind.LITERAL, "x", "http://dt", "", 123);

			// Wait until the writer thread has actually appended the record (no force requested!)
			waitUntilLastAppendedAtLeast(wal, lsn);

			Object logWriter = getField(wal, "logWriter");

			// Wrap the current segment channel with a tracker and swap it in.
			FileChannel original = (FileChannel) getField(logWriter, "segmentChannel");
			TrackingFileChannel tracking = new TrackingFileChannel(original);
			setField(logWriter, "segmentChannel", tracking);

			// Call the private rotate method directly so we only exercise rotation (no additional forces).
			Method rotate = logWriter.getClass().getDeclaredMethod("rotateSegment");
			rotate.setAccessible(true);
			rotate.invoke(logWriter);

			// Expectation: rotation must force() the old segment before closing it
			assertThat(tracking.wasForced()).as("previous segment must be fsynced before rotation").isTrue();
		}
	}

	private static void waitUntilLastAppendedAtLeast(ValueStoreWAL wal, long targetLsn) throws Exception {
		Field f = ValueStoreWAL.class.getDeclaredField("lastAppendedLsn");
		f.setAccessible(true);
		AtomicLong lastAppended = (AtomicLong) f.get(wal);
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (System.nanoTime() < deadline) {
			if (lastAppended.get() >= targetLsn) {
				return;
			}
			Thread.sleep(1);
		}
		throw new AssertionError("writer thread did not append record in time");
	}

	/**
	 * Minimal FileChannel wrapper that tracks whether force() was called, delegating all operations to the wrapped
	 * channel.
	 */
	private static final class TrackingFileChannel extends FileChannel {
		private final FileChannel delegate;
		private volatile boolean forced;

		TrackingFileChannel(FileChannel delegate) {
			this.delegate = delegate;
		}

		boolean wasForced() {
			return forced;
		}

		@Override
		public void force(boolean metaData) throws IOException {
			forced = true;
			delegate.force(metaData);
		}

		// --- Delegate all abstract methods ---

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
			return delegate.write(src);
		}

		@Override
		public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
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
		public long transferTo(long position, long count, WritableByteChannel target)
				throws IOException {
			return delegate.transferTo(position, count, target);
		}

		@Override
		public long transferFrom(ReadableByteChannel src, long position, long count)
				throws IOException {
			return delegate.transferFrom(src, position, count);
		}

		@Override
		public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
			return delegate.map(mode, position, size);
		}

		@Override
		public int read(ByteBuffer dst, long position) throws IOException {
			return delegate.read(dst, position);
		}

		@Override
		public int write(ByteBuffer src, long position) throws IOException {
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
	}

	private static Object getField(Object target, String name) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(target);
	}

	private static void setField(Object target, String name, Object value) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, value);
	}
}
