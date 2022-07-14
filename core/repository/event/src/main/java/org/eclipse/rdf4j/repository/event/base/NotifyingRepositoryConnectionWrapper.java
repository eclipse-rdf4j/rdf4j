/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.event.base;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;
import org.eclipse.rdf4j.repository.event.NotifyingRepositoryConnection;
import org.eclipse.rdf4j.repository.event.RepositoryConnectionListener;

/**
 * This broadcaster is used by the RepositoryBroadcaster to wrap the delegate repository connection. Listeners are
 * notified of changes after they have occurred.
 *
 * @author James Leigh
 * @author Herko ter Horst
 */
public class NotifyingRepositoryConnectionWrapper extends RepositoryConnectionWrapper
		implements NotifyingRepositoryConnection {

	/*-----------*
	 * Variables *
	 *-----------*/

	private boolean activated = false;

	private boolean reportDeltas = false;

	private final Set<RepositoryConnectionListener> listeners = new CopyOnWriteArraySet<>();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public NotifyingRepositoryConnectionWrapper(Repository repository, RepositoryConnection connection) {
		super(repository, connection);
	}

	public NotifyingRepositoryConnectionWrapper(Repository repository, RepositoryConnection connection,
			boolean reportDeltas) {
		this(repository, connection);
		setReportDeltas(reportDeltas);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public boolean reportDeltas() {
		return reportDeltas;
	}

	public void setReportDeltas(boolean reportDeltas) {
		this.reportDeltas = reportDeltas;
	}

	/**
	 * Registers a <var>RepositoryConnectionListener</var> that will receive notifications of operations that are
	 * performed on this connection.
	 */
	@Override
	public void addRepositoryConnectionListener(RepositoryConnectionListener listener) {
		listeners.add(listener);
		activated = true;
	}

	/**
	 * Removes a registered <var>RepositoryConnectionListener</var> from this connection.
	 */
	@Override
	public void removeRepositoryConnectionListener(RepositoryConnectionListener listener) {
		listeners.remove(listener);
		activated = !listeners.isEmpty();
	}

	@Override
	protected boolean isDelegatingAdd() {
		return !activated;
	}

	@Override
	protected boolean isDelegatingRemove() {
		return !activated;
	}

	@Override
	public void addWithoutCommit(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		boolean reportEvent = activated;

		if (reportEvent && reportDeltas()) {
			// Only report if the statement is not present yet
			reportEvent = !getDelegate().hasStatement(subject, predicate, object, false, contexts);
		}

		getDelegate().add(subject, predicate, object, contexts);

		if (reportEvent) {
			for (RepositoryConnectionListener listener : listeners) {
				listener.add(getDelegate(), subject, predicate, object, contexts);
			}
		}
	}

	@Override
	public void clear(Resource... contexts) throws RepositoryException {
		if (activated && reportDeltas()) {
			removeWithoutCommit(null, null, null, contexts);
		} else if (activated) {
			getDelegate().clear(contexts);
			for (RepositoryConnectionListener listener : listeners) {
				listener.clear(getDelegate(), contexts);
			}
		} else {
			getDelegate().clear(contexts);
		}
	}

	@Override
	public void close() throws RepositoryException {
		getDelegate().close();

		if (activated) {
			for (RepositoryConnectionListener listener : listeners) {
				listener.close(getDelegate());
			}
		}
	}

	@Override
	public void commit() throws RepositoryException {
		getDelegate().commit();

		if (activated) {
			for (RepositoryConnectionListener listener : listeners) {
				listener.commit(getDelegate());
			}
		}
	}

	@Override
	public void removeWithoutCommit(Resource subj, IRI pred, Value obj, Resource... ctx) throws RepositoryException {
		if (activated && reportDeltas()) {

			List<Statement> list;
			try (Stream<Statement> stream = getDelegate().getStatements(subj, pred, obj, false, ctx).stream()) {
				list = stream.collect(Collectors.toList());
			}

			getDelegate().remove(subj, pred, obj, ctx);
			for (RepositoryConnectionListener listener : listeners) {
				for (Statement stmt : list) {
					Resource s = stmt.getSubject();
					IRI p = stmt.getPredicate();
					Value o = stmt.getObject();
					Resource c = stmt.getContext();
					listener.remove(getDelegate(), s, p, o, c);
				}
			}
		} else if (activated) {
			getDelegate().remove(subj, pred, obj, ctx);
			for (RepositoryConnectionListener listener : listeners) {
				listener.remove(getDelegate(), subj, pred, obj, ctx);
			}
		} else {
			getDelegate().remove(subj, pred, obj, ctx);
		}
	}

	@Override
	public void removeNamespace(String prefix) throws RepositoryException {
		getDelegate().removeNamespace(prefix);

		if (activated) {
			for (RepositoryConnectionListener listener : listeners) {
				listener.removeNamespace(getDelegate(), prefix);
			}
		}
	}

	@Override
	public void clearNamespaces() throws RepositoryException {
		if (activated && reportDeltas()) {

			List<String> prefix;
			try (Stream<Namespace> stream = getDelegate().getNamespaces().stream()) {
				prefix = stream.map(Namespace::getPrefix).collect(Collectors.toList());
			}

			getDelegate().clearNamespaces();
			for (String p : prefix) {
				removeNamespace(p);
			}
		} else if (activated) {
			getDelegate().clearNamespaces();
			for (RepositoryConnectionListener listener : listeners) {
				listener.clearNamespaces(getDelegate());
			}
		} else {
			getDelegate().clearNamespaces();
		}
	}

	@Override
	public void begin() throws RepositoryException {
		getDelegate().begin();

		if (activated) {
			for (RepositoryConnectionListener listener : listeners) {
				listener.begin(getDelegate());
			}
		}
	}

	@Override
	public void rollback() throws RepositoryException {
		getDelegate().rollback();

		if (activated) {
			for (RepositoryConnectionListener listener : listeners) {
				listener.rollback(getDelegate());
			}
		}
	}

	@Override
	@Deprecated
	public void setAutoCommit(boolean autoCommit) throws RepositoryException {
		boolean wasAutoCommit = !isActive();
		getDelegate().setAutoCommit(autoCommit);

		if (activated && wasAutoCommit != autoCommit) {
			for (RepositoryConnectionListener listener : listeners) {
				listener.setAutoCommit(getDelegate(), autoCommit);
			}
			if (autoCommit) {
				for (RepositoryConnectionListener listener : listeners) {
					listener.commit(getDelegate());
				}
			}
		}
	}

	@Override
	public void setNamespace(String prefix, String name) throws RepositoryException {
		getDelegate().setNamespace(prefix, name);

		if (activated) {
			for (RepositoryConnectionListener listener : listeners) {
				listener.setNamespace(getDelegate(), prefix, name);
			}
		}
	}

	@Override
	public Update prepareUpdate(final QueryLanguage ql, final String update, final String baseURI)
			throws MalformedQueryException, RepositoryException {
		if (activated) {
			return new Update() {

				private final RepositoryConnection conn = getDelegate();

				private final Update delegate = conn.prepareUpdate(ql, update, baseURI);

				@Override
				public void execute() throws UpdateExecutionException {
					delegate.execute();
					if (activated) {
						for (RepositoryConnectionListener listener : listeners) {
							listener.execute(conn, ql, update, baseURI, delegate);
						}
					}
				}

				@Override
				public void setBinding(String name, Value value) {
					delegate.setBinding(name, value);
				}

				@Override
				public void removeBinding(String name) {
					delegate.removeBinding(name);
				}

				@Override
				public void clearBindings() {
					delegate.clearBindings();
				}

				@Override
				public BindingSet getBindings() {
					return delegate.getBindings();
				}

				@Override
				public void setDataset(Dataset dataset) {
					delegate.setDataset(dataset);
				}

				@Override
				public Dataset getDataset() {
					return delegate.getDataset();
				}

				@Override
				public void setIncludeInferred(boolean includeInferred) {
					delegate.setIncludeInferred(includeInferred);
				}

				@Override
				public boolean getIncludeInferred() {
					return delegate.getIncludeInferred();
				}

				@Override
				public void setMaxExecutionTime(int maxExecutionTimeSeconds) {
					delegate.setMaxExecutionTime(maxExecutionTimeSeconds);
				}

				@Override
				public int getMaxExecutionTime() {
					return delegate.getMaxExecutionTime();
				}
			};
		} else {
			return getDelegate().prepareUpdate(ql, update, baseURI);
		}
	}
}
