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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailSink;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatementHelper;

/**
 * @author Håvard Mikkelsen Ottestad
 */
@Experimental
class ExtensibleSailSource implements SailSource {

	private final DataStructureInterface dataStructure;
	private final NamespaceStoreInterface namespaceStore;
	private final boolean inferred;
	private final ExtensibleStatementHelper extensibleStatementHelper;

	public ExtensibleSailSource(DataStructureInterface dataStructure, NamespaceStoreInterface namespaceStore,
			boolean inferred, ExtensibleStatementHelper extensibleStatementHelper) {
		this.dataStructure = dataStructure;
		this.namespaceStore = namespaceStore;
		this.inferred = inferred;
		this.extensibleStatementHelper = extensibleStatementHelper;

	}

	@Override
	public void close() {
	}

	@Override
	public SailSource fork() {
		return new ExtensibleSailSource(new ReadCommittedWrapper(this.dataStructure), namespaceStore, inferred,
				extensibleStatementHelper);
	}

	@Override
	public SailSink sink(IsolationLevel level) {
		return new SailSink() {
			@Override
			public void prepare() {

			}

			@Override
			public void flush() {
				dataStructure.flushForReading();
			}

			@Override
			public synchronized void setNamespace(String prefix, String name) {
				namespaceStore.setNamespace(prefix, name);
			}

			@Override
			public synchronized void removeNamespace(String prefix) {
				namespaceStore.removeNamespace(prefix);
			}

			@Override
			public synchronized void clearNamespaces() {
				namespaceStore.clear();
			}

			@Override
			public void clear(Resource... contexts) {
				dataStructure.clear(inferred, contexts);
			}

			@Override
			public void observe(Resource subj, IRI pred, Value obj, Resource... contexts) {
				throw new RuntimeException("Unsupported operation");
			}

			@Override
			public void approve(Resource subj, IRI pred, Value obj, Resource ctx) {
				Statement statement = SimpleValueFactory.getInstance().createStatement(subj, pred, obj, ctx);

				dataStructure.addStatement(extensibleStatementHelper.fromStatement(statement, inferred));
			}

			@Override
			public void approve(Statement statement) {

				dataStructure.addStatement(extensibleStatementHelper.fromStatement(statement, inferred));
			}

			@Override
			public void deprecate(Resource subj, IRI pred, Value obj, Resource ctx) {
				throw new IllegalStateException("Unsupported operation. Use deprecate(Statement statement) instead!");
			}

			@Override
			public void deprecate(Statement statement) {
				dataStructure.removeStatement(extensibleStatementHelper.fromStatement(statement, inferred));
			}

			@Override
			public boolean deprecateByQuery(Resource subj, IRI pred, Value obj, Resource[] contexts) {
				return dataStructure.removeStatementsByQuery(subj, pred, obj, inferred, contexts);
			}

			@Override
			public void close() {

			}
		};
	}

	@Override
	public SailDataset dataset(IsolationLevel level) {
		return new SailDataset() {
			@Override
			public void close() {

			}

			@Override
			public String getNamespace(String prefix) {
				return namespaceStore.getNamespace(prefix);
//				return null;
			}

			@Override
			public CloseableIteration<? extends Namespace> getNamespaces() {
				return new CloseableIteratorIteration<>(namespaceStore.iterator());
//				return new EmptyIteration<>();
			}

			@Override
			public CloseableIteration<? extends Resource> getContextIDs() {
				return new CloseableIteration<>() {
					final CloseableIteration<? extends Statement> statements = getStatements(null, null,
							null);

					final Set<Resource> contexts = new HashSet<>();

					Resource next = internalNext();

					private Resource internalNext() {

						while (statements.hasNext()) {
							Statement next = statements.next();
							if (!contexts.contains(next.getContext())) {
								contexts.add(next.getContext());
								return next.getContext();
							}
						}

						return null;

					}

					@Override
					public boolean hasNext() {
						if (next == null) {
							next = internalNext();
						}
						return next != null;
					}

					@Override
					public Resource next() {

						if (next == null) {
							next = internalNext();
						}

						Resource temp = next;
						next = null;

						return temp;
					}

					@Override
					public void remove() {

					}

					@Override
					public void close() {
						statements.close();
					}
				};
			}

			@Override
			public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred,
					Value obj, Resource... contexts) {
				return dataStructure.getStatements(subj, pred, obj, inferred, contexts);
			}

		};
	}

	@Override
	public void prepare() {
	}

	@Override
	public void flush() {
		dataStructure.flushForCommit();
	}

	public void init() {
		dataStructure.init();
	}

}
