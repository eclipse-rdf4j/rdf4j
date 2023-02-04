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
package org.eclipse.rdf4j.sail.nativerdf;

import java.io.File;

import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.testsuite.sail.SailConcurrencyTest;
import org.eclipse.rdf4j.testsuite.sail.SailInterruptTest;
import org.junit.jupiter.api.io.TempDir;

/**
 * An extension of {@link SailConcurrencyTest} for testing the class {@link NativeStore}.
 */
public class NativeStoreInterruptTest extends SailInterruptTest {

	/*-----------*
	 * Variables *
	 *-----------*/

	@TempDir
	File dataDir;

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	protected NotifyingSail createSail() throws SailException {
		return new NativeStore(dataDir, "spoc,posc");
	}
}
