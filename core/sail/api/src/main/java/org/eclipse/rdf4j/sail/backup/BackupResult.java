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
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public final class BackupResult {

	private final String backupId;
	private final BackupType type;
	private final Instant createdAt;
	private final long startTransactionId;
	private final long endTransactionId;
	private final OptionalLong baseTransactionId;
	private final Path artifactPath;
	private final String sha256;
	private final boolean verified;

	public BackupResult(String backupId, BackupType type, Instant createdAt, long startTransactionId,
			long endTransactionId,
			OptionalLong baseTransactionId, Path artifactPath, String sha256, boolean verified) {
		this.backupId = Objects.requireNonNull(backupId, "backupId");
		this.type = Objects.requireNonNull(type, "type");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
		this.startTransactionId = startTransactionId;
		this.endTransactionId = endTransactionId;
		this.baseTransactionId = Objects.requireNonNull(baseTransactionId, "baseTransactionId");
		this.artifactPath = Objects.requireNonNull(artifactPath, "artifactPath");
		this.sha256 = Objects.requireNonNull(sha256, "sha256");
		this.verified = verified;
	}

	public String getBackupId() {
		return backupId;
	}

	public BackupType getType() {
		return type;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public long getStartTransactionId() {
		return startTransactionId;
	}

	public long getEndTransactionId() {
		return endTransactionId;
	}

	public OptionalLong getBaseTransactionId() {
		return baseTransactionId;
	}

	public Optional<Long> getBaseTransactionIdValue() {
		return baseTransactionId.isPresent() ? Optional.of(baseTransactionId.getAsLong()) : Optional.empty();
	}

	public Path getArtifactPath() {
		return artifactPath;
	}

	public String getSha256() {
		return sha256;
	}

	public boolean isVerified() {
		return verified;
	}
}
