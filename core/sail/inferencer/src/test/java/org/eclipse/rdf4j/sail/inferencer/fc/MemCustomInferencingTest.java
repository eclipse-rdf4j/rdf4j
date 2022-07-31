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

import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public class MemCustomInferencingTest extends CustomGraphQueryInferencerTest {

	public MemCustomInferencingTest(String resourceFolder, Expectation testData, QueryLanguage language) {
		super(resourceFolder, testData, language);
	}

	@Override
	protected NotifyingSail newSail() {
		NotifyingSail store = new MemoryStore();
		return store;
	}
}
