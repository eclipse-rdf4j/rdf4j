/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.backup;

import java.nio.file.Path;
import java.util.Objects;
import java.util.OptionalLong;

import org.eclipse.rdf4j.common.annotation.Experimental;

@Experimental
public final class BackupRequest {

	private final Path backupDirectory;
	private final BackupType type;
	private final BackupCompression compression;
	private final boolean verifyAfterWrite;
	private final OptionalLong sinceTransactionId;
	private final int retentionCount;

	private BackupRequest(Builder builder) {
		this.backupDirectory = Objects.requireNonNull(builder.backupDirectory, "backupDirectory");
		this.type = Objects.requireNonNull(builder.type, "type");
		this.compression = Objects.requireNonNull(builder.compression, "compression");
		this.verifyAfterWrite = builder.verifyAfterWrite;
		this.sinceTransactionId = builder.sinceTransactionId;
		this.retentionCount = builder.retentionCount;
	}

	public Path getBackupDirectory() {
		return backupDirectory;
	}

	public BackupType getType() {
		return type;
	}

	public BackupCompression getCompression() {
		return compression;
	}

	public boolean isVerifyAfterWrite() {
		return verifyAfterWrite;
	}

	public OptionalLong getSinceTransactionId() {
		return sinceTransactionId;
	}

	public int getRetentionCount() {
		return retentionCount;
	}

	public static Builder builder(Path backupDirectory, BackupType type) {
		return new Builder(backupDirectory, type);
	}

	public static final class Builder {
		private final Path backupDirectory;
		private final BackupType type;
		private BackupCompression compression = BackupCompression.NONE;
		private boolean verifyAfterWrite = true;
		private OptionalLong sinceTransactionId = OptionalLong.empty();
		private int retentionCount = 0;

		private Builder(Path backupDirectory, BackupType type) {
			this.backupDirectory = backupDirectory;
			this.type = type;
		}

		public Builder compression(BackupCompression compression) {
			this.compression = compression;
			return this;
		}

		public Builder verifyAfterWrite(boolean verifyAfterWrite) {
			this.verifyAfterWrite = verifyAfterWrite;
			return this;
		}

		public Builder sinceTransactionId(long sinceTransactionId) {
			this.sinceTransactionId = OptionalLong.of(sinceTransactionId);
			return this;
		}

		public Builder retentionCount(int retentionCount) {
			this.retentionCount = retentionCount;
			return this;
		}

		public BackupRequest build() {
			if (retentionCount < 0) {
				throw new IllegalArgumentException("retentionCount must be >= 0");
			}
			return new BackupRequest(this);
		}
	}
}
