/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.base;

import java.util.function.Function;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractNotifyingSail;
import org.junit.Test;

/**
 * Minimal tests for the functionality of {@link SnapshotSailStore}
 */
public class SnapshotSailStoreTest {

	/**
	 * Base no-op sink as base for testing
	 */
	static class TestSailSink implements SailSink {

		@Override
		public void close() throws SailException {

		}

		@Override
		public void prepare() throws SailException {

		}

		@Override
		public void flush() throws SailException {

		}

		@Override
		public void setNamespace(String prefix, String name) throws SailException {

		}

		@Override
		public void removeNamespace(String prefix) throws SailException {

		}

		@Override
		public void clearNamespaces() throws SailException {

		}

		@Override
		public void clear(Resource... contexts) throws SailException {

		}

		@Override
		public void observe(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {

		}

		@Override
		public void approve(Resource subj, IRI pred, Value obj, Resource ctx) throws SailException {

		}

		@Override
		public void deprecate(Statement statement) throws SailException {

		}
	}

	@Test
	public void testRollbackExceptionDuringCommit() {
		SnapshotSailStore sailStore = createSnapshotSailStore(level -> new TestSailSink() {
			@Override
			public void approve(Resource subj, IRI pred, Value obj, Resource ctx) throws SailException {
				throw new SailException("error during approve");
			}
		});

		Sail sail = createSail(sailStore);
		SailConnection c = sail.getConnection();
		c.begin(IsolationLevels.SNAPSHOT);
		c.addStatement(RDF.TYPE, RDFS.LABEL, sail.getValueFactory().createLiteral("type"));
		try {
			// an exception is triggered during commit by sink implementation above
			c.commit();
		} catch (Exception e) {
			c.rollback();
		} finally {
			c.close();
			// shutting down the SAIL should not result in an exception
			sail.shutDown();
		}
	}

	private Sail createSail(SailStore sailStore) {
		return new AbstractNotifyingSail() {
			@Override
			protected void shutDownInternal() throws SailException {
				// closing the SailStore tries to flush existing changes again
				sailStore.close();
			}

			@Override
			protected NotifyingSailConnection getConnectionInternal() throws SailException {
				return new SailSourceConnection(this, sailStore, (FederatedServiceResolver) null) {
					@Override
					protected void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts)
							throws SailException {
					}

					@Override
					protected void removeStatementsInternal(Resource subj, IRI pred, Value obj, Resource... contexts)
							throws SailException {
					}
				};
			}

			@Override
			public boolean isWritable() throws SailException {
				return true;
			}

			@Override
			public ValueFactory getValueFactory() {
				return SimpleValueFactory.getInstance();
			}
		};
	}

	private SnapshotSailStore createSnapshotSailStore(Function<IsolationLevel, SailSink> sinkFactory) {
		BackingSailSource dummySource = new BackingSailSource() {
			@Override
			public SailSink sink(IsolationLevel level) throws SailException {
				return sinkFactory.apply(level);
			}

			@Override
			public SailDataset dataset(IsolationLevel level) throws SailException {
				return new SailDataset() {
					@Override
					public void close() throws SailException {
					}

					@Override
					public CloseableIteration<? extends Namespace, SailException> getNamespaces() throws SailException {
						return new EmptyIteration<>();
					}

					@Override
					public String getNamespace(String prefix) throws SailException {
						return null;
					}

					@Override
					public CloseableIteration<? extends Resource, SailException> getContextIDs() throws SailException {
						return new EmptyIteration<>();
					}

					@Override
					public CloseableIteration<? extends Statement, SailException> getStatements(Resource subj, IRI pred,
							Value obj,
							Resource... contexts) throws SailException {
						return new EmptyIteration<>();
					}
				};
			}
		};
		return new SnapshotSailStore(new SailStore() {
			@Override
			public ValueFactory getValueFactory() {
				return SimpleValueFactory.getInstance();
			}

			@Override
			public EvaluationStatistics getEvaluationStatistics() {
				return new EvaluationStatistics();
			}

			@Override
			public SailSource getExplicitSailSource() {
				return dummySource;
			}

			@Override
			public SailSource getInferredSailSource() {
				return dummySource;
			}

			@Override
			public void close() throws SailException {
			}
		}, LinkedHashModel::new);
	}
}
