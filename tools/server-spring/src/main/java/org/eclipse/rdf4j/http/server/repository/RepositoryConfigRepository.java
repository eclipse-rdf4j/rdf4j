/*******************************************************************************
 * Copyright (c) 2017 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.UnknownTransactionStateException;
import org.eclipse.rdf4j.repository.base.AbstractRepository;
import org.eclipse.rdf4j.repository.base.AbstractRepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;

/**
 * {@link Repository} implementation that saves {@link RepositoryConfig} RDF to a {@link RepositoryManager}.
 *
 * @author James Leigh
 */
public class RepositoryConfigRepository extends AbstractRepository {

	/**
	 * The repository identifier for the system repository that contains the configuration data.
	 */
	public static final String ID = "SYSTEM";

	private final RepositoryManager manager;

	public RepositoryConfigRepository(RepositoryManager manager) {
		this.manager = manager;
	}

	@Override
	public void setDataDir(File dataDir) {
		// no-op
	}

	@Override
	public File getDataDir() {
		return null;
	}

	@Override
	public boolean isWritable() throws RepositoryException {
		return true;
	}

	@Override
	public ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	@Override
	protected void initializeInternal() throws RepositoryException {
	}

	@Override
	protected void shutDownInternal() throws RepositoryException {
	}

	@Override
	public RepositoryConnection getConnection() throws RepositoryException {
		return new AbstractRepositoryConnection(this) {

			private boolean active = false;

			private Model committed = loadModel();

			private final Model added = new TreeModel();

			private final Model removed = new TreeModel();

			@Override
			public RepositoryResult<Resource> getContextIDs() throws RepositoryException {
				Set<Resource> contextIDs = new LinkedHashSet<>();
				manager.getRepositoryIDs().forEach(id -> {
					contextIDs.add(getContext(id));
				});
				CloseableIteration<Resource, RepositoryException> iter;
				iter = new CloseableIteratorIteration<>(contextIDs.iterator());
				return new RepositoryResult<>(iter);
			}

			@Override
			public RepositoryResult<Statement> getStatements(Resource subj, IRI pred, Value obj,
					boolean includeInferred, Resource... contexts) throws RepositoryException {
				CloseableIteration<Statement, RepositoryException> iter = new CloseableIteratorIteration<>(
						committed.getStatements(subj, pred, obj, contexts).iterator());
				return new RepositoryResult<>(iter);
			}

			@Override
			public void exportStatements(Resource subj, IRI pred, Value obj, boolean includeInferred,
					RDFHandler handler, Resource... contexts) throws RepositoryException, RDFHandlerException {
				Model model = committed.filter(subj, pred, obj, contexts);
				handler.startRDF();
				model.getNamespaces().forEach(ns -> {
					handler.handleNamespace(ns.getPrefix(), ns.getName());
				});
				model.forEach(st -> {
					handler.handleStatement(st);
				});
				handler.endRDF();
			}

			@Override
			public long size(Resource... contexts) throws RepositoryException {
				return committed.filter(null, null, null, contexts).size();
			}

			@Override
			public boolean isActive() throws UnknownTransactionStateException, RepositoryException {
				return active;
			}

			@Override
			public void begin() throws RepositoryException {
				active = true;
			}

			@Override
			public void prepare() throws RepositoryException {
				// no-op
			}

			@Override
			public void commit() throws RepositoryException {
				Set<String> ids = new LinkedHashSet<>();
				ids.addAll(manager.getRepositoryIDs());
				ids.addAll(RepositoryConfigUtil.getRepositoryIDs(added));
				ids.forEach(id -> {
					Resource ctx = getContext(id);
					Model less = removed.filter(null, null, null, ctx);
					Model more = added.filter(null, null, null, ctx);
					Model alt = RepositoryConfigUtil.getRepositoryConfigModel(added, id);
					if (!less.isEmpty() || !more.isEmpty() || alt != null) {
						Model model = new TreeModel(committed.filter(null, null, null, getContext(id)));
						model.removeAll(less);
						removed.getNamespaces().forEach(ns -> {
							model.removeNamespace(ns.getPrefix());
						});
						added.getNamespaces().forEach(ns -> {
							model.setNamespace(ns);
						});
						model.addAll(more);
						if (alt != null) {
							model.addAll(alt);
						}
						if (model.isEmpty()) {
							manager.removeRepository(id);
						} else {
							manager.addRepositoryConfig(RepositoryConfigUtil.getRepositoryConfig(model, id));
						}
					}
				});
				committed = loadModel();
				rollback();
			}

			@Override
			public void rollback() throws RepositoryException {
				added.clear();
				added.getNamespaces().clear();
				removed.clear();
				removed.getNamespaces().clear();
				active = false;
			}

			@Override
			public RepositoryResult<Namespace> getNamespaces() throws RepositoryException {
				CloseableIteration<Namespace, RepositoryException> iter;
				iter = new CloseableIteratorIteration<>(committed.getNamespaces().iterator());
				return new RepositoryResult<>(iter);
			}

			@Override
			public String getNamespace(String prefix) throws RepositoryException {
				Optional<Namespace> ns = committed.getNamespace(prefix);
				if (ns.isPresent()) {
					return ns.get().getName();
				} else {
					return null;
				}
			}

			@Override
			public void setNamespace(String prefix, String name) throws RepositoryException {
				removed.removeNamespace(prefix);
				added.setNamespace(prefix, name);
			}

			@Override
			public void removeNamespace(String prefix) throws RepositoryException {
				added.removeNamespace(prefix);
				Optional<Namespace> ns = committed.getNamespace(prefix);
				if (ns.isPresent()) {
					removed.setNamespace(ns.get());
				}
			}

			@Override
			public void clearNamespaces() throws RepositoryException {
				added.getNamespaces().clear();
				committed.getNamespaces().forEach(ns -> {
					removed.setNamespace(ns);
				});
			}

			@Override
			public Query prepareQuery(QueryLanguage ql, String query, String baseURI)
					throws RepositoryException, MalformedQueryException {
				throw unsupported();
			}

			@Override
			public TupleQuery prepareTupleQuery(QueryLanguage ql, String query, String baseURI)
					throws RepositoryException, MalformedQueryException {
				throw unsupported();
			}

			@Override
			public GraphQuery prepareGraphQuery(QueryLanguage ql, String query, String baseURI)
					throws RepositoryException, MalformedQueryException {
				throw unsupported();
			}

			@Override
			public BooleanQuery prepareBooleanQuery(QueryLanguage ql, String query, String baseURI)
					throws RepositoryException, MalformedQueryException {
				throw unsupported();
			}

			@Override
			public Update prepareUpdate(QueryLanguage ql, String update, String baseURI)
					throws RepositoryException, MalformedQueryException {
				throw unsupported();
			}

			@Override
			protected void addWithoutCommit(Resource subj, IRI pred, Value obj, Resource... contexts)
					throws RepositoryException {
				added.add(subj, pred, obj, contexts);
			}

			@Override
			protected void removeWithoutCommit(Resource subj, IRI pred, Value obj, Resource... contexts)
					throws RepositoryException {
				Model model = committed.filter(subj, pred, obj, contexts);
				removed.addAll(model);
			}

			private Model loadModel() {
				Model model = new TreeModel();
				manager.getRepositoryIDs().forEach(id -> {
					Resource ctx = getContext(id);
					RepositoryConfig config = manager.getRepositoryConfig(id);
					Model cfg = new TreeModel();
					config.export(cfg, ctx);
					cfg.getNamespaces().forEach(ns -> {
						model.setNamespace(ns);
					});
					cfg.forEach(st -> {
						model.add(st.getSubject(), st.getPredicate(), st.getObject(), ctx);
					});
				});
				return model;
			}

			private Resource getContext(String repositoryID) {
				String location;
				try {
					location = manager.getLocation().toURI().toString();
				} catch (MalformedURLException | URISyntaxException e) {
					assert false;
					location = "urn:" + repositoryID;
				}
				String url = Protocol.getRepositoryLocation(location, repositoryID);
				return getValueFactory().createIRI(url + "#" + repositoryID);
			}

			private UnsupportedOperationException unsupported() {
				return new UnsupportedOperationException("Query operations are not supported on the SYSTEM repository");
			}

		};
	}

}
