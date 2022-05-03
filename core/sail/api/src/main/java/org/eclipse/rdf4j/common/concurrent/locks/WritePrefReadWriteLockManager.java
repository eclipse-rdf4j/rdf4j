/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks;

import java.lang.invoke.VarHandle;

import org.eclipse.rdf4j.common.concurrent.locks.diagnostics.LockDiagnostics;

/**
 * A read/write lock manager with writer preference.
 *
 * @author Håvard M. Ottestad
 */
public class WritePrefReadWriteLockManager extends AbstractReadWriteLockManager {

	public WritePrefReadWriteLockManager() {
		super();
	}

	public WritePrefReadWriteLockManager(boolean trackLocks) {
		super(trackLocks);
	}

	public WritePrefReadWriteLockManager(boolean trackLocks, int waitToCollect) {
		super(trackLocks, waitToCollect);
	}

	public WritePrefReadWriteLockManager(String alias, LockDiagnostics... lockDiagnostics) {
		super(alias, lockDiagnostics);
	}

	public WritePrefReadWriteLockManager(String alias, int waitToCollect, LockDiagnostics... lockDiagnostics) {
		super(alias, waitToCollect, lockDiagnostics);
	}

	@Override
	int getWriterPreference() {
		return 1000;
	}

	@Override
	ReadLock createReadLockInner() throws InterruptedException {
		while (stampedLock.isWriteLocked()) {
			spinWaitAtReadLock();
		}

		while (true) {
			readersLocked.increment();
			if (!stampedLock.isWriteLocked()) {
				// Everything is good! We have acquired a read-lock and there are no active writers.
				break;
			} else {
				// Release our read lock so we don't block any writers.
				readersUnlocked.increment();
				spinWaitAtReadLock();
			}
		}

		return new ReadLock(readersUnlocked);
	}
}
