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

/**
 * A read/write lock manager with reader preference.
 *
 * @author Håvard M. Ottestad
 */
public class ReadPrefReadWriteLockManager extends AbstractReadWriteLockManager {

	public ReadPrefReadWriteLockManager() {
		super();
	}

	public ReadPrefReadWriteLockManager(boolean trackLocks) {
		super(trackLocks);
	}

	public ReadPrefReadWriteLockManager(boolean trackLocks, int waitToCollect) {
		super(trackLocks, waitToCollect);
	}

	public ReadPrefReadWriteLockManager(String alias, int waitToCollect, LockDiagnostics... lockDiagnostics) {
		super(alias, waitToCollect, lockDiagnostics);
	}

	@Override
	int getWriterPreference() {
		return 1;
	}

	public ReadPrefReadWriteLockManager(String alias, LockDiagnostics... lockDiagnostics) {
		super(alias, lockDiagnostics);
	}
}
