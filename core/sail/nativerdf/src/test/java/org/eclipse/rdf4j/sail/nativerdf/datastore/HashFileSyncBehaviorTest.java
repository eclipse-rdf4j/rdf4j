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
package org.eclipse.rdf4j.sail.nativerdf.datastore;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.eclipse.rdf4j.common.io.NioFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that HashFile.sync(boolean) honors the caller's request to force metadata to disk by passing {@code true} to
 * the underlying FileChannel#force(boolean) call. This must also hold when HashFile is constructed with
 * {@code forceSync=true}.
 */
public class HashFileSyncBehaviorTest {

	@TempDir
	File tempDir;

	private File hashFilePath;

	@BeforeEach
	public void setup() {
		hashFilePath = new File(tempDir, "values.hash");
	}

	@AfterEach
	public void tearDown() {
		// nothing to close because individual tests close files explicitly when needed
	}

	@Test
	public void syncFalseForcesFileContent_whenForceSyncDisabled() throws Exception {
		try (HashFile hf = new HashFile(hashFilePath, /* forceSync= */ false, /* initialSize= */ 16)) {
			TrackingFileChannel tracker = injectTrackingChannel(hf);

			// Act
			hf.sync(false);

			// Assert: even without metadata, content must be flushed
			assertThat(tracker.forceFalseCount)
					.as("HashFile.sync(false) should call force(false) on the underlying channel when forceSync=false")
					.isGreaterThan(0);
		}
	}

	@Test
	public void syncTrueHonorsMetadataFlag_whenForceSyncDisabled() throws Exception {
		try (HashFile hf = new HashFile(hashFilePath, /* forceSync= */ false, /* initialSize= */ 16)) {
			TrackingFileChannel tracker = injectTrackingChannel(hf);

			// Act
			hf.sync(true);

			// Assert: must have at least one force(true) call
			assertThat(tracker.forceTrueCount)
					.as("HashFile.sync(true) should call force(true) on the underlying channel when forceSync=false")
					.isGreaterThan(0);
		}
	}

	@Test
	public void syncTrueHonorsMetadataFlag_whenForceSyncEnabled() throws Exception {
		try (HashFile hf = new HashFile(hashFilePath, /* forceSync= */ true, /* initialSize= */ 16)) {
			TrackingFileChannel tracker = injectTrackingChannel(hf);

			// Act
			hf.sync(true);

			// Assert: must have at least one force(true) call
			assertThat(tracker.forceTrueCount)
					.as("HashFile.sync(true) should call force(true) on the underlying channel when forceSync=true")
					.isGreaterThan(0);
		}
	}

	private static TrackingFileChannel injectTrackingChannel(HashFile hf) throws Exception {
		// Access private final NioFile field on HashFile
		Field nioFileField = HashFile.class.getDeclaredField("nioFile");
		nioFileField.setAccessible(true);
		NioFile nio = (NioFile) nioFileField.get(hf);

		// Access private volatile FileChannel field on NioFile
		Field fcField = NioFile.class.getDeclaredField("fc");
		fcField.setAccessible(true);
		FileChannel delegate = (FileChannel) fcField.get(nio);

		TrackingFileChannel tracking = new TrackingFileChannel(delegate);
		fcField.set(nio, tracking);
		return tracking;
	}

	/**
	 * Delegating channel that tracks calls to force(boolean).
	 */
	static class TrackingFileChannel extends FileChannel {
		final FileChannel delegate;
		volatile int forceTrueCount = 0;
		volatile int forceFalseCount = 0;

		TrackingFileChannel(FileChannel delegate) {
			this.delegate = delegate;
		}

		@Override
		public void force(boolean metaData) throws IOException {
			if (metaData) {
				forceTrueCount++;
			} else {
				forceFalseCount++;
			}
			delegate.force(metaData);
		}

		// Delegations for abstract methods / other operations
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
		public void implCloseChannel() throws IOException {
			delegate.close();
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
	}
}
