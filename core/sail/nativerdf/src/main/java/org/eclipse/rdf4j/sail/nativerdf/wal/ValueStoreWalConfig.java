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

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for the ValueStore WAL implementation.
 */
public final class ValueStoreWalConfig {

	public static final String DEFAULT_DIRECTORY_NAME = "value-store-wal";

	public enum SyncPolicy {
		ALWAYS,
		INTERVAL,
		COMMIT
	}

	private final Path walDirectory;
	private final Path snapshotsDirectory;
	private final String storeUuid;
	private final long maxSegmentBytes;
	private final int queueCapacity;
	private final int batchBufferBytes;
	private final SyncPolicy syncPolicy;
	private final Duration syncInterval;
	private final Duration idlePollInterval;
	private final boolean syncBootstrapOnOpen;
	private final boolean recoverValueStoreOnOpen;

	private ValueStoreWalConfig(Builder builder) {
		this.walDirectory = builder.walDirectory;
		this.snapshotsDirectory = builder.snapshotsDirectory;
		this.storeUuid = builder.storeUuid;
		this.maxSegmentBytes = builder.maxSegmentBytes;
		this.queueCapacity = builder.queueCapacity;
		this.batchBufferBytes = builder.batchBufferBytes;
		this.syncPolicy = builder.syncPolicy;
		this.syncInterval = builder.syncInterval;
		this.idlePollInterval = builder.idlePollInterval;
		this.syncBootstrapOnOpen = builder.syncBootstrapOnOpen;
		this.recoverValueStoreOnOpen = builder.recoverValueStoreOnOpen;
	}

	public Path walDirectory() {
		return walDirectory;
	}

	public Path snapshotsDirectory() {
		return snapshotsDirectory;
	}

	public String storeUuid() {
		return storeUuid;
	}

	public long maxSegmentBytes() {
		return maxSegmentBytes;
	}

	public int queueCapacity() {
		return queueCapacity;
	}

	public int batchBufferBytes() {
		return batchBufferBytes;
	}

	public SyncPolicy syncPolicy() {
		return syncPolicy;
	}

	public Duration syncInterval() {
		return syncInterval;
	}

	public Duration idlePollInterval() {
		return idlePollInterval;
	}

	/**
	 * When true, the ValueStore will synchronously rebuild the WAL from existing values during open before allowing any
	 * new values to be added. When false (default), bootstrap runs asynchronously in the background.
	 */
	public boolean syncBootstrapOnOpen() {
		return syncBootstrapOnOpen;
	}

	/**
	 * When true, the ValueStore will attempt to reconstruct missing or empty ValueStore files from the WAL during open
	 * before allowing any operations.
	 */
	public boolean recoverValueStoreOnOpen() {
		return recoverValueStoreOnOpen;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private Path walDirectory;
		private Path snapshotsDirectory;
		private String storeUuid;
		private long maxSegmentBytes = 1L << 30; // 1 GB
		private int queueCapacity = 262_144;
		private int batchBufferBytes = 1 << 20; // 1 MB
		private SyncPolicy syncPolicy = SyncPolicy.COMMIT;
		private Duration syncInterval = Duration.ofMillis(2);
		private Duration idlePollInterval = Duration.ofMillis(1);
		private boolean syncBootstrapOnOpen = false;
		private boolean recoverValueStoreOnOpen = false;

		private Builder() {
		}

		public Builder walDirectory(Path walDirectory) {
			this.walDirectory = Objects.requireNonNull(walDirectory, "walDirectory");
			if (this.snapshotsDirectory == null) {
				this.snapshotsDirectory = walDirectory.resolve("snapshots");
			}
			return this;
		}

		public Builder snapshotsDirectory(Path snapshotsDirectory) {
			this.snapshotsDirectory = Objects.requireNonNull(snapshotsDirectory, "snapshotsDirectory");
			return this;
		}

		public Builder storeUuid(String storeUuid) {
			this.storeUuid = Objects.requireNonNull(storeUuid, "storeUuid");
			return this;
		}

		public Builder maxSegmentBytes(long maxSegmentBytes) {
			this.maxSegmentBytes = maxSegmentBytes;
			return this;
		}

		public Builder queueCapacity(int queueCapacity) {
			this.queueCapacity = queueCapacity;
			return this;
		}

		public Builder batchBufferBytes(int batchBufferBytes) {
			this.batchBufferBytes = batchBufferBytes;
			return this;
		}

		public Builder syncPolicy(SyncPolicy syncPolicy) {
			this.syncPolicy = Objects.requireNonNull(syncPolicy, "syncPolicy");
			return this;
		}

		public Builder syncInterval(Duration syncInterval) {
			this.syncInterval = Objects.requireNonNull(syncInterval, "syncInterval");
			return this;
		}

		public Builder idlePollInterval(Duration idlePollInterval) {
			this.idlePollInterval = Objects.requireNonNull(idlePollInterval, "idlePollInterval");
			return this;
		}

		/**
		 * Control whether WAL bootstrap happens synchronously during open. Default is false.
		 */
		public Builder syncBootstrapOnOpen(boolean syncBootstrapOnOpen) {
			this.syncBootstrapOnOpen = syncBootstrapOnOpen;
			return this;
		}

		/** Enable automatic ValueStore recovery from WAL during open. */
		public Builder recoverValueStoreOnOpen(boolean recoverValueStoreOnOpen) {
			this.recoverValueStoreOnOpen = recoverValueStoreOnOpen;
			return this;
		}

		public ValueStoreWalConfig build() {
			if (walDirectory == null) {
				throw new IllegalStateException("walDirectory must be set");
			}
			if (snapshotsDirectory == null) {
				snapshotsDirectory = walDirectory.resolve("snapshots");
			}
			if (storeUuid == null || storeUuid.isEmpty()) {
				throw new IllegalStateException("storeUuid must be set");
			}
			if (maxSegmentBytes <= 0) {
				throw new IllegalStateException("maxSegmentBytes must be positive");
			}
			if (queueCapacity <= 0) {
				throw new IllegalStateException("queueCapacity must be positive");
			}
			if (batchBufferBytes <= 4096) {
				throw new IllegalStateException("batchBufferBytes must be > 4KB");
			}
			return new ValueStoreWalConfig(this);
		}
	}
}
