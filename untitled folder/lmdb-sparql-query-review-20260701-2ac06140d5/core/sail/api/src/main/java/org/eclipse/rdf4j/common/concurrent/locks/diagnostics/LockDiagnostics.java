/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.common.concurrent.locks.diagnostics;

import org.eclipse.rdf4j.common.concurrent.locks.Properties;

/**
 * Configuration options for diagnostic features of the ReadWriteLockManager implementations to help debug locking
 * issues.
 *
 * @author Håvard M. Ottestad
 */
public enum LockDiagnostics {

	releaseAbandoned,
	detectStalledOrDeadlock,
	stackTrace;

	private final static LockDiagnostics[] legacyTracking = { releaseAbandoned, detectStalledOrDeadlock, stackTrace };
	private final static LockDiagnostics[] noTracking = { releaseAbandoned };

	public static LockDiagnostics[] fromLegacyTracking(boolean trackLocks) {
		if (trackLocks || Properties.lockTrackingEnabled()) {
			return legacyTracking;
		}
		return noTracking;
	}
}
