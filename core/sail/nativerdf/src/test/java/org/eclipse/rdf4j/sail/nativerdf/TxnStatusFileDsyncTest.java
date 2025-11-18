/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TxnStatusFileDsyncTest {

	@TempDir
	File dataDir;

	@Test
	public void defaultUsesDsync() throws Exception {
		System.clearProperty("org.eclipse.rdf4j.sail.nativerdf.disableTxnStatusDsync");

		// Trigger class initialization
		new TxnStatusFile(dataDir).close();

		boolean alwaysSync = getAlwaysSyncFlag();
		assertTrue(alwaysSync, "TxnStatusFile should use DSYNC by default");
	}

	@Test
	public void propertyDisablesDsync() throws Exception {
		System.setProperty("org.eclipse.rdf4j.sail.nativerdf.disableTxnStatusDsync", "true");

		// Trigger class initialization with property set
		new TxnStatusFile(dataDir).close();

		boolean alwaysSync = getAlwaysSyncFlag();
		assertFalse(alwaysSync, "System property should disable DSYNC");
	}

	private boolean getAlwaysSyncFlag() throws Exception {
		Field field = TxnStatusFile.class.getDeclaredField("ALWAYS_SYNC_TXN_STATUS");
		field.setAccessible(true);
		return (boolean) field.get(null);
	}
}
