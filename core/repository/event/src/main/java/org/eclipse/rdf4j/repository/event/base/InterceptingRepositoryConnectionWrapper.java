/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.event.base;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
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
import org.eclipse.rdf4j.repository.event.InterceptingRepositoryConnection;
import org.eclipse.rdf4j.repository.event.RepositoryConnectionInterceptor;

/**
 * Wrapper that notifies interceptors of events on RepositoryConnections before they happen. Any interceptor can block
 * the operation by returning true from the relevant notification method. To do so will also cause the notification
 * process to stop, i.e. no other interceptors will be notified. The order in which interceptors are notified is
 * unspecified.
 *
 * @author Herko ter Horst
 * @see InterceptingRepositoryWrapper
 */
public class InterceptingRepositoryConnectionWrapper extends RepositoryConnectionWrapper
		implements InterceptingRepositoryConnection {

	/*-----------*
	 * Variables *
	 *-----------*/

	private boolean activated;

	private final Set<RepositoryConnectionInterceptor> interceptors = new CopyOnWriteArraySet<>();

	/*--------------*
	 * Construcotrs *
	 *--------------*/

	public InterceptingRepositoryConnectionWrapper(Repository repository, RepositoryConnection connection) {
		super(repository, connection);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Registers a <var>RepositoryConnectionInterceptor</var> that will receive notifications of operations that are
	 * performed on this connection.
	 */
	@Override
	public void addRepositoryConnectionInterceptor(RepositoryConnectionInterceptor interceptor) {
		interceptors.add(interceptor);
		activated = true;
	}

	/**
	 * Removes a registered <var>RepositoryConnectionInterceptor</var> from this connection.
	 */
	@Override
	public void removeRepositoryConnectionInterceptor(RepositoryConnectionInterceptor interceptor) {
		interceptors.remove(interceptor);
		activated = !interceptors.isEmpty();
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
		boolean denied = false;
		if (activated) {
			for (RepositoryConnectionInterceptor interceptor : interceptors) {
				denied = interceptor.add(getDelegate(), subject, predicate, object, contexts);
				if (denied) {
					break;
				}
			}
		}
		if (!denied) {
			getDelegate().add(subject, predicate, object, contexts);
		}
	}

	@Override
	public void clear(Resource... contexts) throws RepositoryException {
		boolean denied = false;
		if (activated) {
			for (RepositoryConnectionInterceptor interceptor : interceptors) {
				denied = interceptor.clear(getDelegate(), contexts);
				if (denied) {
					break;
				}
			}
		}
		if (!denied) {
			getDelegate().clear(contexts);
		}
	}

	@Override
	public void begin() throws RepositoryException {
		boolean denied = false;
		if (activated) {
			for (RepositoryConnectionInterceptor interceptor : interceptors) {
				denied = interceptor.begin(getDelegate());
				if (denied) {
					break;
				}
			}
		}
		if (!denied) {
			super.begin();
		}
	}

	@Override
	public void close() throws RepositoryException {
		boolean denied = false;
		if (activated) {
			for (RepositoryConnectionInterceptor interceptor : interceptors) {
				denied = interceptor.close(getDelegate());
				if (denied) {
					break;
				}
			}
		}
		if (!denied) {
			super.close();
		}
	}

	@Override
	public void commit() throws RepositoryException {
		boolean denied = false;
		if (activated) {
			for (RepositoryConnectionInterceptor interceptor : interceptors) {
				denied = interceptor.commit(getDelegate());
				if (denied) {
					break;
				}
			}
		}
		if (!denied) {
			getDelegate().commit();
		}
	}

	@Override
	public void removeWithoutCommit(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		boolean denied = false;
		if (activated) {
			for (RepositoryConnectionInterceptor interceptor : interceptors) {
				denied = interceptor.remove(getDelegate(), subject, predicate, object, contexts);
				if (denied) {
					break;
				}
			}
		}
		if (!denied) {
			getDelegate().remove(subject, predicate, object, contexts);

		}
	}

	@Override
	public void removeNamespace(String prefix) throws RepositoryException {
		boolean denied = false;
		if (activated) {
			for (RepositoryConnectionInterceptor interceptor : interceptors) {
				denied = interceptor.removeNamespace(getDelegate(), prefix);
				if (denied) {
					break;
				}
			}
		}
		if (!denied) {
			getDelegate().removeNamespace(prefix);
		}
	}

	@Override
	public void clearNamespaces() throws RepositoryException {
		boolean denied = false;
		if (activated) {
			for (RepositoryConnectionInterceptor interceptor : interceptors) {
				denied = interceptor.clearNamespaces(getDelegate());
				if (denied) {
					break;
				}
			}
		}
		if (!denied) {
			getDelegate().clearNamespaces();
		}
	}

	@Override
	public void rollback() throws RepositoryException {
		boolean denied = false;
		if (activated) {
			for (RepositoryConnectionInterceptor interceptor : interceptors) {
				denied = interceptor.rollback(getDelegate());
				if (denied) {
					break;
				}
			}
		}
		if (!denied) {
			getDelegate().rollback();
		}
	}

	@Override
	@Deprecated
	public void setAutoCommit(boolean autoCommit) throws RepositoryException {
		boolean denied = false;
		boolean wasAutoCommit = !isActive();
		if (activated && wasAutoCommit != autoCommit) {
			for (RepositoryConnectionInterceptor interceptor : interceptors) {
				denied = interceptor.setAutoCommit(getDelegate(), autoCommit);
				if (denied) {
					break;
				}
			}
		}
		if (!denied) {
			getDelegate().setAutoCommit(autoCommit);

		}
	}

	@Override
	public void setNamespace(String prefix, String name) throws RepositoryException {
		boolean denied = false;
		if (activated) {
			for (RepositoryConnectionInterceptor interceptor : interceptors) {
				denied = interceptor.setNamespace(getDelegate(), prefix, name);
				if (denied) {
					break;
				}
			}
		}
		if (!denied) {
			getDelegate().setNamespace(prefix, name);
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
					boolean denied = false;
					if (activated) {
						for (RepositoryConnectionInterceptor interceptor : interceptors) {
							denied = interceptor.execute(conn, ql, update, baseURI, delegate);
							if (denied) {
								break;
							}
						}
					}
					if (!denied) {
						delegate.execute();
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
