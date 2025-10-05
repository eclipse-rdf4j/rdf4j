/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/

package org.eclipse.rdf4j.sail.base;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;
import org.junit.jupiter.api.Test;

/**
 * Verifies that SailDatasetImpl.size respects a pending clear() operation (statementCleared), and does not delegate to
 * the backing dataset when cleared with no contexts.
 */
public class SailDatasetImplSizeTest {

	/**
	 * Minimal backing dataset that reports a fixed size regardless of arguments.
	 */
	private static final class FixedSizeDataset implements SailDataset {
		private final long size;

		private FixedSizeDataset(long size) {
			this.size = size;
		}

		@Override
		public void close() throws SailException {
			// no-op
		}

		@Override
		public CloseableIteration<? extends Namespace> getNamespaces() throws SailException {
			return new EmptyIteration<>();
		}

		@Override
		public String getNamespace(String prefix) throws SailException {
			return null;
		}

		@Override
		public CloseableIteration<? extends Resource> getContextIDs() throws SailException {
			return new EmptyIteration<>();
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
				Resource... contexts) throws SailException {
			return new EmptyIteration<>();
		}

		@Override
		public long size(Resource subj, IRI pred, Value obj, Resource... contexts) {
			return size;
		}
	}

	@Test
	public void size_respects_statementCleared() {
		// backing dataset contains data (non-zero size)
		SailDataset backing = new FixedSizeDataset(5);

		// create a changeset and simulate clear() without contexts
		Changeset changes = new Changeset() {
			@Override
			public void flush() throws SailException {
				// not used in this test
			}

			@Override
			public org.eclipse.rdf4j.model.Model createEmptyModel() {
				return new org.eclipse.rdf4j.model.impl.LinkedHashModel();
			}
		};

		// clear() with zero contexts should mark statementCleared=true while leaving
		// hasApproved()/hasDeprecated() false
		changes.clear();

		// snapshot over backing with pending clear should report size 0
		SailDataset snapshot = new SailDatasetImpl(backing, changes);
		long snapshotSize = snapshot.size(null, null, null);

		assertEquals(0L, snapshotSize,
				"size() should respect statementCleared and return 0 when cleared without contexts");
	}
}
