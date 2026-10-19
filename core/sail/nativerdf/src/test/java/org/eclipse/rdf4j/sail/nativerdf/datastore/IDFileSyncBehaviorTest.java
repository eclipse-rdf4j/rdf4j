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
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.eclipse.rdf4j.common.io.NioFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that IDFile.sync(boolean) passes the caller's boolean flag through to FileChannel#force(boolean), ensuring
 * metadata can be flushed when requested.
 */
public class IDFileSyncBehaviorTest {

	@TempDir
	File tempDir;

	@Test
	public void syncTrueHonorsMetadataFlag() throws Exception {
		File file = new File(tempDir, "values.id");
		try (IDFile id = new IDFile(file, /* forceSync= */ false)) {
			TrackingFileChannel tracker = injectTrackingChannel(id);

			id.sync(true);

			assertThat(tracker.forceTrueCount)
					.as("IDFile.sync(true) should call force(true) on the underlying channel")
					.isGreaterThan(0);
		}
	}

	private static TrackingFileChannel injectTrackingChannel(IDFile id) throws Exception {
		Field nioFileField = IDFile.class.getDeclaredField("nioFile");
		nioFileField.setAccessible(true);
		NioFile nio = (NioFile) nioFileField.get(id);

		Field fcField = NioFile.class.getDeclaredField("fc");
		fcField.setAccessible(true);
		FileChannel delegate = (FileChannel) fcField.get(nio);

		TrackingFileChannel tracking = new TrackingFileChannel(delegate);
		fcField.set(nio, tracking);
		return tracking;
	}

	static class TrackingFileChannel extends FileChannel {
		final FileChannel delegate;
		volatile int forceTrueCount = 0;
		volatile int forceFalseCount = 0;

		TrackingFileChannel(FileChannel delegate) {
			this.delegate = delegate;
		}

		@Override
		public void force(boolean metaData) throws java.io.IOException {
			if (metaData) {
				forceTrueCount++;
			} else {
				forceFalseCount++;
			}
			delegate.force(metaData);
		}

		@Override
		public int read(ByteBuffer dst) throws java.io.IOException {
			return delegate.read(dst);
		}

		@Override
		public long read(ByteBuffer[] dsts, int offset, int length) throws java.io.IOException {
			return delegate.read(dsts, offset, length);
		}

		@Override
		public int write(ByteBuffer src) throws java.io.IOException {
			return delegate.write(src);
		}

		@Override
		public long write(ByteBuffer[] srcs, int offset, int length) throws java.io.IOException {
			return delegate.write(srcs, offset, length);
		}

		@Override
		public long position() throws java.io.IOException {
			return delegate.position();
		}

		@Override
		public FileChannel position(long newPosition) throws java.io.IOException {
			delegate.position(newPosition);
			return this;
		}

		@Override
		public long size() throws java.io.IOException {
			return delegate.size();
		}

		@Override
		public FileChannel truncate(long size) throws java.io.IOException {
			delegate.truncate(size);
			return this;
		}

		@Override
		protected void implCloseChannel() throws java.io.IOException {
			delegate.close();
		}

		@Override
		public int read(ByteBuffer dst, long position) throws java.io.IOException {
			return delegate.read(dst, position);
		}

		@Override
		public int write(ByteBuffer src, long position) throws java.io.IOException {
			return delegate.write(src, position);
		}

		@Override
		public long transferTo(long position, long count, java.nio.channels.WritableByteChannel target)
				throws java.io.IOException {
			return delegate.transferTo(position, count, target);
		}

		@Override
		public long transferFrom(java.nio.channels.ReadableByteChannel src, long position, long count)
				throws java.io.IOException {
			return delegate.transferFrom(src, position, count);
		}

		@Override
		public MappedByteBuffer map(MapMode mode, long position, long size) throws java.io.IOException {
			return delegate.map(mode, position, size);
		}

		@Override
		public FileLock lock(long position, long size, boolean shared) throws java.io.IOException {
			return delegate.lock(position, size, shared);
		}

		@Override
		public FileLock tryLock(long position, long size, boolean shared) throws java.io.IOException {
			return delegate.tryLock(position, size, shared);
		}
	}
}
