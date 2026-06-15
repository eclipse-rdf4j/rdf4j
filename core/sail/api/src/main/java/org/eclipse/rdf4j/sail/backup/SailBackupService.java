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
import java.util.List;
import java.util.UUID;

import org.eclipse.rdf4j.sail.SailException;

public interface SailBackupService extends AutoCloseable {

	BackupResult createBackup(BackupRequest request) throws SailException;

	Path restore(PointInTimeRestoreRequest request) throws SailException;

	List<BackupResult> listBackups(Path backupDirectory) throws SailException;

	UUID schedule(BackupSchedule schedule) throws SailException;

	boolean cancelSchedule(UUID scheduleId);

	boolean verify(BackupResult backupResult) throws SailException;

	@Override
	void close();
}

