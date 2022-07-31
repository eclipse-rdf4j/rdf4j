/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.security.AccessControlException;

import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.sail.LockManager;
import org.eclipse.rdf4j.sail.SailLockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to create a lock in a directory.
 *
 * @author James Leigh
 * @author Arjohn Kampman
 */
public class DirectoryLockManager implements LockManager {

	private static final String LOCK_DIR_NAME = "lock";

	private static final String LOCK_FILE_NAME = "locked";

	private static final String INFO_FILE_NAME = "process";

	private final Logger logger = LoggerFactory.getLogger(DirectoryLockManager.class);

	private final File dir;

	public DirectoryLockManager(File dir) {
		this.dir = dir;
	}

	@Override
	public String getLocation() {
		return dir.toString();
	}

	private File getLockDir() {
		return new File(dir, LOCK_DIR_NAME);
	}

	/**
	 * Determines if the directory is locked.
	 *
	 * @return <code>true</code> if the directory is already locked.
	 */
	@Override
	public boolean isLocked() {
		return getLockDir().exists();
	}

	/**
	 * Creates a lock in a directory if it does not yet exist.
	 *
	 * @return a newly acquired lock or null if the directory is already locked.
	 */
	@Override
	public Lock tryLock() {
		File lockDir = getLockDir();

		if (lockDir.exists()) {
			removeInvalidLock(lockDir);
		}

		if (!lockDir.mkdir()) {
			return null;
		}

		Lock lock = null;

		try {
			File infoFile = new File(lockDir, INFO_FILE_NAME);
			File lockedFile = new File(lockDir, LOCK_FILE_NAME);

			RandomAccessFile raf = new RandomAccessFile(lockedFile, "rw");
			try {
				FileLock fileLock = raf.getChannel().lock();
				lock = createLock(raf, fileLock);
				sign(infoFile);
			} catch (IOException e) {
				if (lock != null) {
					// Also closes raf
					lock.release();
				} else {
					raf.close();
				}
				throw e;
			}
		} catch (IOException e) {
			logger.error(e.toString(), e);
		}

		return lock;
	}

	/**
	 * Creates a lock in a directory if it does not yet exist.
	 *
	 * @return a newly acquired lock.
	 * @throws SailLockedException if the directory is already locked.
	 */
	@Override
	public Lock lockOrFail() throws SailLockedException {
		Lock lock = tryLock();

		if (lock != null) {
			return lock;
		}

		String requestedBy = getProcessName();
		String lockedBy = getLockedBy();

		if (lockedBy != null) {
			throw new SailLockedException(lockedBy, requestedBy, this);
		}

		lock = tryLock();
		if (lock != null) {
			return lock;
		}

		throw new SailLockedException(requestedBy);
	}

	/**
	 * Revokes a lock owned by another process.
	 *
	 * @return <code>true</code> if a lock was successfully revoked.
	 */
	@Override
	public boolean revokeLock() {
		File lockDir = getLockDir();
		File lockedFile = new File(lockDir, LOCK_FILE_NAME);
		File infoFile = new File(lockDir, INFO_FILE_NAME);
		lockedFile.delete();
		infoFile.delete();
		return lockDir.delete();
	}

	private void removeInvalidLock(File lockDir) {
		try {
			boolean revokeLock = false;

			File lockedFile = new File(lockDir, LOCK_FILE_NAME);
			try (RandomAccessFile raf = new RandomAccessFile(lockedFile, "rw")) {
				FileLock fileLock = raf.getChannel().tryLock();

				if (fileLock != null) {
					logger.warn("Removing invalid lock {}", getLockedBy());
					fileLock.release();
					revokeLock = true;
				}
			} catch (OverlappingFileLockException exc) {
				// lock is still valid
			}

			if (revokeLock) {
				revokeLock();
			}
		} catch (IOException e) {
			logger.warn(e.toString(), e);
		}
	}

	private String getLockedBy() {
		try {
			File lockDir = getLockDir();
			File infoFile = new File(lockDir, INFO_FILE_NAME);
			try (BufferedReader reader = new BufferedReader(new FileReader(infoFile))) {
				return reader.readLine();
			}
		} catch (IOException e) {
			logger.warn(e.toString(), e);
			return null;
		}
	}

	private Lock createLock(final RandomAccessFile raf, final FileLock fileLock) {
		return new Lock() {

			private Thread hook;

			{
				try {
					Thread hook = new Thread(this::delete);
					Runtime.getRuntime().addShutdownHook(hook);
					this.hook = hook;
				} catch (AccessControlException e) {
					// okay, just remember to close it yourself
				}
			}

			@Override
			public boolean isActive() {
				return fileLock.isValid() || hook != null;
			}

			@Override
			public void release() {
				try {
					if (hook != null) {
						Runtime.getRuntime().removeShutdownHook(hook);
						hook = null;
					}
				} catch (IllegalStateException e) {
					// already shutting down
				} catch (AccessControlException e) {
					logger.warn(e.toString(), e);
				}
				delete();
			}

			synchronized void delete() {
				try {
					if (raf.getChannel().isOpen()) {
						fileLock.release();
						raf.close();
					}
				} catch (IOException e) {
					logger.warn(e.toString(), e);
				}

				revokeLock();
			}
		};
	}

	private void sign(File infoFile) throws IOException {
		try (FileWriter out = new FileWriter(infoFile)) {
			out.write(getProcessName());
			out.flush();
		}
	}

	private String getProcessName() {
		return ManagementFactory.getRuntimeMXBean().getName();
	}
}
