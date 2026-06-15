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

import java.time.Duration;
import java.util.Objects;

public final class BackupSchedule {

	private final Duration interval;
	private final BackupRequest request;

	public BackupSchedule(Duration interval, BackupRequest request) {
		this.interval = Objects.requireNonNull(interval, "interval");
		this.request = Objects.requireNonNull(request, "request");
		if (interval.isNegative() || interval.isZero()) {
			throw new IllegalArgumentException("interval must be > 0");
		}
	}

	public Duration getInterval() {
		return interval;
	}

	public BackupRequest getRequest() {
		return request;
	}
}

