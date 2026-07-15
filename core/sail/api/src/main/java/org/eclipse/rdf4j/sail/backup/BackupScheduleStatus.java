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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.rdf4j.common.annotation.Experimental;

@Experimental
public final class BackupScheduleStatus {

	private final UUID scheduleId;
	private final BackupSchedule schedule;
	private final Instant createdAt;
	private final boolean active;
	private final Optional<Instant> lastAttemptAt;
	private final Optional<Instant> lastSuccessAt;
	private final Optional<BackupResult> lastSuccess;
	private final Optional<Instant> lastFailureAt;
	private final Optional<String> lastFailureMessage;

	public BackupScheduleStatus(UUID scheduleId, BackupSchedule schedule, Instant createdAt, boolean active,
			Optional<Instant> lastAttemptAt, Optional<Instant> lastSuccessAt, Optional<BackupResult> lastSuccess,
			Optional<Instant> lastFailureAt, Optional<String> lastFailureMessage) {
		this.scheduleId = Objects.requireNonNull(scheduleId, "scheduleId");
		this.schedule = Objects.requireNonNull(schedule, "schedule");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
		this.active = active;
		this.lastAttemptAt = Objects.requireNonNull(lastAttemptAt, "lastAttemptAt");
		this.lastSuccessAt = Objects.requireNonNull(lastSuccessAt, "lastSuccessAt");
		this.lastSuccess = Objects.requireNonNull(lastSuccess, "lastSuccess");
		this.lastFailureAt = Objects.requireNonNull(lastFailureAt, "lastFailureAt");
		this.lastFailureMessage = Objects.requireNonNull(lastFailureMessage, "lastFailureMessage");
	}

	public UUID getScheduleId() {
		return scheduleId;
	}

	public BackupSchedule getSchedule() {
		return schedule;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public boolean isActive() {
		return active;
	}

	public Optional<Instant> getLastAttemptAt() {
		return lastAttemptAt;
	}

	public Optional<Instant> getLastSuccessAt() {
		return lastSuccessAt;
	}

	public Optional<BackupResult> getLastSuccess() {
		return lastSuccess;
	}

	public Optional<Instant> getLastFailureAt() {
		return lastFailureAt;
	}

	public Optional<String> getLastFailureMessage() {
		return lastFailureMessage;
	}
}
