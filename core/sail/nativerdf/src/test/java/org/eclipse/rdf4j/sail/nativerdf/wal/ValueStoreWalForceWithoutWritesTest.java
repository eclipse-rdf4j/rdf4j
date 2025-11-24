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
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.nativerdf.ValueStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ValueStoreWalForceWithoutWritesTest {

	private static final ValueFactory VF = SimpleValueFactory.getInstance();

	@TempDir
	Path tempDir;

	@AfterEach
	void resetChannelFactory() {
		ValueStoreWAL.resetChannelOpenerForTesting();
	}

	@Test
	void doesNotForceFreshChannels() throws Exception {
		List<Path> violations = Collections.synchronizedList(new ArrayList<>());
		ValueStoreWAL.setChannelOpenerForTesting((path, options) -> new TrackingFileChannel(
				FileChannel.open(path, options), path, violations));

		Path walDir = tempDir.resolve("wal");
		Files.createDirectories(walDir);
		ValueStoreWalConfig config = ValueStoreWalConfig.builder()
				.walDirectory(walDir)
				.storeUuid(UUID.randomUUID().toString())
				.maxSegmentBytes(2 * 1024)
				.batchBufferBytes(8 * 1024)
				.syncPolicy(ValueStoreWalConfig.SyncPolicy.INTERVAL)
				.build();

		Path valuesDir = tempDir.resolve("values");
		Files.createDirectories(valuesDir);

		try (ValueStoreWAL wal = ValueStoreWAL.open(config);
				ValueStore store = new ValueStore(valuesDir.toFile(), false, ValueStore.VALUE_CACHE_SIZE,
						ValueStore.VALUE_ID_CACHE_SIZE, ValueStore.NAMESPACE_CACHE_SIZE,
						ValueStore.NAMESPACE_ID_CACHE_SIZE, wal)) {
			Literal literal = VF.createLiteral("value-" + "x".repeat(8192));
			store.storeValue(literal);
			OptionalLong pending = store.drainPendingWalHighWaterMark();
			assertThat(pending).isPresent();
			store.awaitWalDurable(pending.getAsLong());
		}

		assertThat(violations)
				.as("force() must only occur on channels that performed writes")
				.isEmpty();
	}

	private static final class TrackingFileChannel extends FileChannel {
		private final FileChannel delegate;
		private final Path path;
		private final List<Path> violations;
		private long bytesWritten;

		private TrackingFileChannel(FileChannel delegate, Path path, List<Path> violations) {
			this.delegate = delegate;
			this.path = path;
			this.violations = violations;
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
			int written = delegate.write(src);
			bytesWritten += Math.max(0, written);
			return written;
		}

		@Override
		public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
			long written = delegate.write(srcs, offset, length);
			bytesWritten += Math.max(0, written);
			return written;
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
			if (bytesWritten == 0) {
				violations.add(path);
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
			int written = delegate.write(src, position);
			bytesWritten += Math.max(0, written);
			return written;
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
}
