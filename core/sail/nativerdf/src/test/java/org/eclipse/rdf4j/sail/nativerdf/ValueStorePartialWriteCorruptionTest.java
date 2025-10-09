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
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.rdf4j.sail.nativerdf.datastore.DataStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Induces ValueStore corruption by simulating partial file writes at the channel level. This test replaces the
 * FileChannel inside the internal NioFile used by ValueStore's DataFile with a wrapper that occasionally reports fewer
 * bytes written than requested. Since DataFile/NioFile do not loop to ensure full writes, this can leave truncated
 * records (e.g., zeroed payloads or invalid length prefixes), reproducing the user-observed symptoms.
 */
public class ValueStorePartialWriteCorruptionTest {

	@TempDir
	File dataDir;

	private ValueStore valueStore;

	@BeforeEach
	public void setup() throws IOException {
		valueStore = new ValueStore(dataDir);
		// Disable soft-fail to surface corruption as exceptions in checkConsistency
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;
		injectChoppyChannel(valueStore, /* partialWriteFrequency= */1);
	}

	@AfterEach
	public void tearDown() throws IOException {
		try {
			if (valueStore != null) {
				valueStore.close();
			}
		} finally {
			NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;
		}
	}

	@Test
	public void partialWritesCauseValueStoreInconsistency() throws Exception {
		int writers = 8;
		int valuesPerWriter = 20_000;

		ExecutorService pool = Executors.newFixedThreadPool(writers);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();

		for (int i = 0; i < writers; i++) {
			final int seed = 2025 + i;
			futures.add(pool.submit(() -> {
				Random rnd = new Random(seed);
				try {
					start.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
				for (int j = 0; j < valuesPerWriter; j++) {
					try {
						// Mix of small IRIs and occasionally large literals to trigger both buffered and direct writes
						String ns = "http://ex/" + rnd.nextInt(64) + "/";
						String local = "s" + rnd.nextInt(50_000);
						valueStore.storeValue(valueStore.createIRI(ns, local));
						if ((j % 200) == 0) {
							String big = buildString(12_000 + rnd.nextInt(6000));
							valueStore.storeValue(valueStore.createLiteral(big));
						} else {
							valueStore.storeValue(valueStore.createLiteral("v" + j));
						}
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
				return null;
			}));
		}

		start.countDown();
		for (Future<?> f : futures) {
			f.get();
		}
		pool.shutdownNow();

		// Close and reopen to force reload from disk only
		valueStore.close();
		valueStore = new ValueStore(dataDir);

		// After the write-loop fix in NioFile, partial low-level writes are retried until complete.
		// Therefore, checkConsistency should not throw anymore.
		valueStore.checkConsistency();
	}

	private static String buildString(int len) {
		StringBuilder sb = new StringBuilder(len);
		while (sb.length() < len) {
			sb.append('x');
		}
		return sb.toString();
	}

	/**
	 * Replace the internal FileChannel of the ValueStore's DataFile with a wrapper that sometimes performs partial
	 * writes.
	 */
	private static void injectChoppyChannel(ValueStore vs, int partialWriteFrequency) {
		try {
			Field dsField = ValueStore.class.getDeclaredField("dataStore");
			dsField.setAccessible(true);
			DataStore ds = (DataStore) dsField.get(vs);

			Field dfField = DataStore.class.getDeclaredField("dataFile");
			dfField.setAccessible(true);
			Object dataFile = dfField.get(ds);

			Field nioField = dataFile.getClass().getDeclaredField("nioFile");
			nioField.setAccessible(true);
			Object nioFile = nioField.get(dataFile);

			Field fcField = nioFile.getClass().getDeclaredField("fc");
			fcField.setAccessible(true);
			FileChannel original = (FileChannel) fcField.get(nioFile);

			fcField.set(nioFile, new ChoppyFileChannel(original, partialWriteFrequency));
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * A FileChannel wrapper that intermittently writes fewer bytes than requested.
	 */
	static class ChoppyFileChannel extends FileChannel {
		private final FileChannel delegate;
		private final int frequency;
		private int opCount = 0;

		ChoppyFileChannel(FileChannel delegate, int frequency) {
			this.delegate = delegate;
			this.frequency = Math.max(1, frequency);
		}

		@Override
		public int write(ByteBuffer src, long position) throws IOException {
			opCount++;
			if (opCount % frequency == 0) {
				int remaining = src.remaining();
				if (remaining > 4) {
					int partial = Math.max(1, remaining / 2);
					// write only a part
					ByteBuffer slice = src.slice();
					slice.limit(partial);
					int written = delegate.write(slice, position);
					// advance original buffer by 'written' bytes but leave unwritten bytes unflushed
					src.position(src.position() + written);
					return written;
				}
			}
			return delegate.write(src, position);
		}

		@Override
		public int write(ByteBuffer src) throws IOException {
			opCount++;
			if (opCount % frequency == 0) {
				int remaining = src.remaining();
				if (remaining > 4) {
					int partial = Math.max(1, remaining / 2);
					ByteBuffer slice = src.slice();
					slice.limit(partial);
					int written = delegate.write(slice);
					src.position(src.position() + written);
					return written;
				}
			}
			return delegate.write(src);
		}

		// Delegations / stubs for other abstract methods
		@Override
		public int read(ByteBuffer dst) throws IOException {
			return delegate.read(dst);
		}

		// remove duplicate signature (below there is another read(ByteBuffer,long))

		@Override
		public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
			return delegate.read(dsts, offset, length);
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
		public int read(ByteBuffer dst, long position) throws IOException {
			return delegate.read(dst, position);
		}

		@Override
		public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
			return delegate.map(mode, position, size);
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
		protected void implCloseChannel() throws IOException {
			delegate.close();
		}
	}
}
