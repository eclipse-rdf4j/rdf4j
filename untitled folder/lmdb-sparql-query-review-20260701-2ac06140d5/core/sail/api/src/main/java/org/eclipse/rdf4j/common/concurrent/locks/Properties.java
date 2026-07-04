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

package org.eclipse.rdf4j.common.concurrent.locks;

/**
 * Class controlling various logging properties such as the amount of lock tracking that is done for debugging (at the
 * cost of performance).
 *
 * @author Arjohn Kampman
 */
public class Properties {

	/**
	 * The system property "info.aduna.concurrent.locks.trackLocks" that can be used to enable lock tracking by giving
	 * it a (non-null) value.
	 */
	public static final String TRACK_LOCKS = "info.aduna.concurrent.locks.trackLocks";

	/**
	 * Sets of clears the {@link #TRACK_LOCKS} system property.
	 */
	public static void setLockTrackingEnabled(boolean trackLocks) {
		if (trackLocks) {
			System.setProperty(TRACK_LOCKS, "");
		} else {
			System.clearProperty(TRACK_LOCKS);
		}
	}

	public static boolean lockTrackingEnabled() {
		try {
			return System.getProperty(TRACK_LOCKS) != null;
		} catch (SecurityException e) {
			// Thrown when not allowed to read system properties, for example when
			// running in applets
			return false;
		}
	}
}
