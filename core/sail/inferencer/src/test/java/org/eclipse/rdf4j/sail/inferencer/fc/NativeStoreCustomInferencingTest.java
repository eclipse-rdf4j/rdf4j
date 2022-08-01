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
package org.eclipse.rdf4j.sail.inferencer.fc;

import static org.junit.Assert.fail;

import java.io.IOException;

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class NativeStoreCustomInferencingTest extends CustomGraphQueryInferencerTest {

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	public NativeStoreCustomInferencingTest(String resourceFolder, Expectation testData, QueryLanguage language) {
		super(resourceFolder, testData, language);
	}

	@Override
	protected NotifyingSail newSail() {
		try {
			return new NativeStore(tempDir.newFolder(), "spoc,posc");
		} catch (IOException e) {
			fail(e.getMessage());
			throw new AssertionError(e);
		}
	}
}
