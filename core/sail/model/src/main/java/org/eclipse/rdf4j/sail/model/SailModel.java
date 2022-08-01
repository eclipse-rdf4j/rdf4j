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
package org.eclipse.rdf4j.sail.model;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.ExceptionConvertingIteration;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.common.iterator.CloseableIterationIterator;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.AbstractModel;
import org.eclipse.rdf4j.model.impl.FilteredModel;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Model implementation for a {@link org.eclipse.rdf4j.sail.SailConnection}. All
 * {@link org.eclipse.rdf4j.sail.SailException}s are wrapped in a {@link org.eclipse.rdf4j.model.util.ModelException}.
 * Not thread-safe.
 *
 * @author Mark
 *
 * @apiNote this feature is for internal use only: its existence, signature or behavior may change without warning from
 *          one release to the next.
 */
@InternalUseOnly
public class SailModel extends AbstractModel {

	private static final long serialVersionUID = -2104886971549374410L;

	private transient SailConnection conn;

	private UUID connKey;

	private final boolean includeInferred;

	public SailModel(SailConnection conn, boolean includeInferred) {
		this.conn = conn;
		this.includeInferred = includeInferred;
	}

	public void setConnection(SailConnection conn) {
		this.conn = conn;
	}

	@Override
	public Set<Namespace> getNamespaces() {
		Set<Namespace> namespaces;
		try {
			try (CloseableIteration<? extends Namespace, SailException> iter = conn.getNamespaces()) {
				namespaces = Iterations.asSet(conn.getNamespaces());
			}
		} catch (SailException e) {
			throw new ModelException(e);
		}
		return namespaces;
	}

	@Override
	public Optional<Namespace> getNamespace(String prefix) {
		try {
			String name = conn.getNamespace(prefix);
			return (name != null) ? Optional.of(new SimpleNamespace(prefix, name)) : Optional.ofNullable(null);
		} catch (SailException e) {
			throw new ModelException(e);
		}
	}

	@Override
	public Namespace setNamespace(String prefix, String name) {
		try {
			conn.setNamespace(prefix, name);
		} catch (SailException e) {
			throw new ModelException(e);
		}
		return new SimpleNamespace(prefix, name);
	}

	@Override
	public void setNamespace(Namespace namespace) {
		try {
			conn.setNamespace(namespace.getPrefix(), namespace.getName());
		} catch (SailException e) {
			throw new ModelException(e);
		}
	}

	@Override
	public Optional<Namespace> removeNamespace(String prefix) {
		Namespace namespace = getNamespace(prefix).orElse(null);
		if (namespace != null) {
			try {
				conn.removeNamespace(prefix);
			} catch (SailException e) {
				throw new ModelException(e);
			}
		}
		return Optional.ofNullable(namespace);
	}

	@Override
	public boolean contains(Resource subj, IRI pred, Value obj, Resource... contexts) {
		try {
			return conn.hasStatement(subj, pred, obj, includeInferred, contexts);
		} catch (SailException e) {
			throw new ModelException(e);
		}
	}

	@Override
	public boolean add(Resource subj, IRI pred, Value obj, Resource... contexts) {
		if (subj == null || pred == null || obj == null) {
			throw new UnsupportedOperationException("Incomplete statement");
		}
		boolean exists = contains(subj, pred, obj, contexts);
		if (!exists) {
			try {
				conn.addStatement(subj, pred, obj, contexts);
			} catch (SailException e) {
				throw new ModelException(e);
			}
		}
		return !exists;
	}

	@Override
	public boolean remove(Resource subj, IRI pred, Value obj, Resource... contexts) {
		boolean exists = contains(subj, pred, obj, contexts);
		if (exists) {
			try {
				conn.removeStatements(subj, pred, obj, contexts);
			} catch (SailException e) {
				throw new ModelException(e);
			}
		}
		return exists;
	}

	@Override
	public boolean clear(Resource... contexts) {
		boolean exists = contains(null, null, null, contexts);
		if (exists) {
			try {
				conn.clear(contexts);
			} catch (SailException e) {
				throw new ModelException(e);
			}
		}
		return exists;
	}

	@Override
	public Model filter(Resource subj, IRI pred, Value obj, Resource... contexts) {
		return new FilteredModel(this, subj, pred, obj, contexts) {

			private static final long serialVersionUID = -3834026632361358191L;

			@Override
			public Iterator<Statement> iterator() {
				return SailModel.this.iterator(subj, pred, obj, contexts);
			}

			@Override
			protected void removeFilteredTermIteration(Iterator<Statement> iter, Resource subj, IRI pred, Value obj,
					Resource... contexts) {
				SailModel.this.removeTermIteration(iter, subj, pred, obj, contexts);
			}
		};
	}

	@Override
	public void removeTermIteration(Iterator<Statement> iter, Resource subj, IRI pred, Value obj,
			Resource... contexts) {
		try {
			conn.removeStatements(subj, pred, obj, contexts);
		} catch (SailException e) {
			throw new ModelException(e);
		}
	}

	/**
	 * The returned Iterator implements Closeable. If it is not exhausted then it should be explicitly closed.
	 */
	@Override
	public Iterator<Statement> iterator() {
		return iterator(null, null, null);
	}

	private Iterator<Statement> iterator(Resource subj, IRI pred, Value obj, Resource... contexts) {
		try {
			CloseableIteration<? extends Statement, ?> iter = conn.getStatements(subj, pred, obj, includeInferred,
					contexts);
			return new CloseableIterationIterator<>(
					new ExceptionConvertingIteration<Statement, ModelException>(iter) {

						private Statement last;

						@Override
						public Statement next() {
							last = super.next();
							return last;
						}

						@Override
						public void remove() {
							if (last == null) {
								throw new IllegalStateException("next() not yet called");
							}
							SailModel.this.remove(last);
							last = null;
						}

						@Override
						protected ModelException convert(Exception e) {
							throw new ModelException(e);
						}
					});
		} catch (SailException e) {
			throw new ModelException(e);
		}
	}

	@Override
	protected void closeIterator(Iterator<?> iter) {
		if (iter instanceof Closeable) {
			try {
				((Closeable) iter).close();
			} catch (IOException ioe) {
				throw new ModelException(ioe);
			}
		} else {
			super.closeIterator(iter);
		}
	}

	@Override
	public int size() {
		long lsize;
		if (!includeInferred) {
			try {
				lsize = conn.size();
			} catch (SailException e) {
				throw new ModelException(e);
			}
		} else {
			lsize = 0L;
			Iterator<Statement> iter = iterator();
			try {
				while (iter.hasNext()) {
					lsize++;
					iter.next();
				}
			} finally {
				closeIterator(iter);
			}
		}
		return (lsize < Integer.MAX_VALUE) ? (int) lsize : Integer.MAX_VALUE;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		this.connKey = NonSerializables.register(this.conn);
		out.defaultWriteObject();
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		this.conn = SailConnection.class.cast(NonSerializables.get(this.connKey));
	}
}
