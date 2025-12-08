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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A *very small* in‑memory replacement for SailStore sufficient for unit tests of SketchBasedJoinEstimator.
 */
class StubSailStore implements SailStore {

	private final List<Statement> data = new CopyOnWriteArrayList<>();

	public void add(Statement st) {
		data.add(st);
	}

	public void addAll(Collection<Statement> sts) {
		data.addAll(sts);
	}

	/* -- SailStore interface -------------------------------------- */

	@Override
	public ValueFactory getValueFactory() {
		return null;
	}

	@Override
	public EvaluationStatistics getEvaluationStatistics() {
		return null;
	}

	@Override
	public SailSource getExplicitSailSource() {
		return new StubSailSource();
	}

	@Override
	public SailSource getInferredSailSource() {
		return null;
	}

	@Override
	public void close() throws SailException {

	}

	/* … all other SailStore methods can remain unimplemented … */

	/* ------------------------------------------------------------- */
	private class StubSailSource implements SailSource {
		@Override
		public void close() {
		}

		@Override
		public SailSource fork() {
			return null;
		}

		@Override
		public SailSink sink(IsolationLevel level) throws SailException {
			return null;
		}

		@Override
		public SailDataset dataset(IsolationLevel level) throws SailException {
			return new SailDataset() {

				@Override
				public void close() {
				}

				@Override
				public CloseableIteration<? extends Namespace> getNamespaces() throws SailException {
					return null;
				}

				@Override
				public String getNamespace(String prefix) throws SailException {
					return "";
				}

				@Override
				public CloseableIteration<? extends Resource> getContextIDs() throws SailException {
					return null;
				}

				@Override
				public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
						Resource... contexts) throws SailException {
					return new CloseableIteratorIteration<>(data.iterator());
				}
			};
		}

		@Override
		public void prepare() throws SailException {

		}

		@Override
		public void flush() throws SailException {

		}
	}
}
