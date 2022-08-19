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
package org.eclipse.rdf4j.common.concurrent.locks;

import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockDiagnostics;

class WritePrefReadWriteLockManagerTest extends AbstractReadWriteLockManagerTest {

	@Override
	void setUpLockManagers() {
		Properties.setLockTrackingEnabled(false);
		lockManager = new WritePrefReadWriteLockManager("", 1);
		lockManagerReleaseAbandoned = new WritePrefReadWriteLockManager("", 1, LockDiagnostics.releaseAbandoned);
		lockManagerTracking = new WritePrefReadWriteLockManager(true, 1);
		lockManagerReleaseAbandonedStackTrace = new WritePrefReadWriteLockManager("", 1,
				LockDiagnostics.releaseAbandoned, LockDiagnostics.stackTrace);

	}

}
