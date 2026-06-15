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

public final class PointInTimeRestoreRequest {
	private final Path backupDirectory;
	private final Path restoreDirectory;
	private final long targetTransactionId;
	private final boolean verifyBeforeRestore;

	public PointInTimeRestoreRequest(Path backupDirectory, Path restoreDirectory, long targetTransactionId,
			boolean verifyBeforeRestore) {
		this.backupDirectory = Objects.requireNonNull(backupDirectory, "backupDirectory");
		this.restoreDirectory = Objects.requireNonNull(restoreDirectory, "restoreDirectory");
		this.targetTransactionId = targetTransactionId;
		this.verifyBeforeRestore = verifyBeforeRestore;
	}

	public Path getBackupDirectory() {
		return backupDirectory;
	}

	public Path getRestoreDirectory() {
		return restoreDirectory;
	}

	public long getTargetTransactionId() {
		return targetTransactionId;
	}

	public boolean isVerifyBeforeRestore() {
		return verifyBeforeRestore;
	}
}

