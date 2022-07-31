/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.extensiblestore;

import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;

public class ExtensibleStoreImplForTests
		extends ExtensibleStore<NaiveHashSetDataStructure, SimpleMemoryNamespaceStore> {

	public ExtensibleStoreImplForTests() {
	}

	@Override
	protected synchronized void initializeInternal() throws SailException {
		namespaceStore = new SimpleMemoryNamespaceStore();
		dataStructure = new NaiveHashSetDataStructure();
		super.initializeInternal();
	}

	@Override
	protected NotifyingSailConnection getConnectionInternal() throws SailException {
		return new ExtensibleStoreConnectionImplForTests(this);
	}

	@Override
	public boolean isWritable() throws SailException {
		return true;
	}

	public EvaluationStatistics getEvalStats() {
		return sailStore.getEvaluationStatistics();
	}
}
