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
package org.eclipse.rdf4j.sail.lucene.impl;

import static org.junit.Assert.assertThrows;

import java.util.Properties;

import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test to verify that LuceneSail parameters are validated correctly.
 *
 * @author Piotr Sowiński
 */
public class LuceneParametersTest {

	@Test
	public void testZeroFsyncInterval() {
		var index = new LuceneIndex();
		var params = new Properties();
		params.setProperty(LuceneSail.FSYNC_INTERVAL_KEY, "0");
		params.setProperty(LuceneSail.LUCENE_RAMDIR_KEY, "true");
		var e = assertThrows(IllegalArgumentException.class, () -> index.initialize(params));
		Assertions.assertTrue(
				e.getMessage().contains(LuceneSail.FSYNC_INTERVAL_KEY),
				"Message should mention fsync interval"
		);
	}

	@Test
	public void testNegativeFsyncInterval() {
		var index = new LuceneIndex();
		var params = new Properties();
		params.setProperty(LuceneSail.FSYNC_INTERVAL_KEY, "-10");
		params.setProperty(LuceneSail.LUCENE_RAMDIR_KEY, "true");
		var e = assertThrows(IllegalArgumentException.class, () -> index.initialize(params));
		Assertions.assertTrue(
				e.getMessage().contains(LuceneSail.FSYNC_INTERVAL_KEY),
				"Message should mention fsync interval"
		);
	}
}
