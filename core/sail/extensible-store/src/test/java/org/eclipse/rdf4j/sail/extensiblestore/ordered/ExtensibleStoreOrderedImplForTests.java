/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.extensiblestore.ordered;

import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.ExtensibleStore;
import org.eclipse.rdf4j.sail.extensiblestore.SimpleMemoryNamespaceStore;

public class ExtensibleStoreOrderedImplForTests
		extends ExtensibleStore<OrderedDataStructure, SimpleMemoryNamespaceStore> {

	public ExtensibleStoreOrderedImplForTests() {
		super(Cache.NONE);
	}

	public ExtensibleStoreOrderedImplForTests(Cache cache) {
		super(cache);
	}

	@Override
	protected synchronized void initializeInternal() throws SailException {
		namespaceStore = new SimpleMemoryNamespaceStore();
		dataStructure = new OrderedDataStructure();
		super.initializeInternal();
	}

	@Override
	protected NotifyingSailConnection getConnectionInternal() throws SailException {
		return new ExtensibleStoreConnectionOrderedImplForTests(this);
	}

	@Override
	public boolean isWritable() throws SailException {
		return true;
	}

	public EvaluationStatistics getEvalStats() {
		return sailStore.getEvaluationStatistics();
	}
}
