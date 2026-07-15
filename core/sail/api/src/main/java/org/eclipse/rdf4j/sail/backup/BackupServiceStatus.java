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

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

public final class BackupServiceStatus {

	private final Optional<BackupResult> lastSuccessfulBackup;
	private final Optional<Instant> lastSuccessfulBackupAt;
	private final Optional<Instant> lastFailureAt;
	private final Optional<String> lastFailureStage;
	private final OptionalLong lastFailureTransactionId;
	private final Optional<String> lastFailureMessage;
	private final List<BackupScheduleStatus> scheduledBackups;

	public BackupServiceStatus(Optional<BackupResult> lastSuccessfulBackup, Optional<Instant> lastSuccessfulBackupAt,
			Optional<Instant> lastFailureAt, Optional<String> lastFailureStage,
			OptionalLong lastFailureTransactionId, Optional<String> lastFailureMessage,
			List<BackupScheduleStatus> scheduledBackups) {
		this.lastSuccessfulBackup = Objects.requireNonNull(lastSuccessfulBackup, "lastSuccessfulBackup");
		this.lastSuccessfulBackupAt = Objects.requireNonNull(lastSuccessfulBackupAt, "lastSuccessfulBackupAt");
		this.lastFailureAt = Objects.requireNonNull(lastFailureAt, "lastFailureAt");
		this.lastFailureStage = Objects.requireNonNull(lastFailureStage, "lastFailureStage");
		this.lastFailureTransactionId = Objects.requireNonNull(lastFailureTransactionId, "lastFailureTransactionId");
		this.lastFailureMessage = Objects.requireNonNull(lastFailureMessage, "lastFailureMessage");
		this.scheduledBackups = List.copyOf(Objects.requireNonNull(scheduledBackups, "scheduledBackups"));
	}

	public Optional<BackupResult> getLastSuccessfulBackup() {
		return lastSuccessfulBackup;
	}

	public Optional<Instant> getLastSuccessfulBackupAt() {
		return lastSuccessfulBackupAt;
	}

	public Optional<Instant> getLastFailureAt() {
		return lastFailureAt;
	}

	public Optional<String> getLastFailureStage() {
		return lastFailureStage;
	}

	public OptionalLong getLastFailureTransactionId() {
		return lastFailureTransactionId;
	}

	public Optional<String> getLastFailureMessage() {
		return lastFailureMessage;
	}

	public List<BackupScheduleStatus> getScheduledBackups() {
		return scheduledBackups;
	}
}
