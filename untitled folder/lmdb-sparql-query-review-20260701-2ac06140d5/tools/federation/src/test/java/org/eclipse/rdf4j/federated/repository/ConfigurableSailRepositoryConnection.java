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
package org.eclipse.rdf4j.federated.repository;

import org.eclipse.rdf4j.federated.repository.ConfigurableSailRepositoryFactory.FailingRepositoryException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailBooleanQuery;
import org.eclipse.rdf4j.repository.sail.SailGraphQuery;
import org.eclipse.rdf4j.repository.sail.SailQuery;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailTupleQuery;
import org.eclipse.rdf4j.sail.SailConnection;

/**
 * Specialized {@link SailRepositoryConnection} that can be used with {@link ConfigurableSailRepository}
 *
 * @author Andreas Schwarte
 *
 */
public class ConfigurableSailRepositoryConnection extends SailRepositoryConnection {

	private final ConfigurableSailRepository rep;

	protected ConfigurableSailRepositoryConnection(ConfigurableSailRepository repository,
			SailConnection sailConnection) {
		super(repository, sailConnection);
		this.rep = repository;
	}

	@Override
	public void add(Statement st, Resource... contexts)
			throws RepositoryException {
		checkOperations(true);
		super.add(st, contexts);
	}

	@Override
	public void add(Iterable<? extends Statement> arg0,
			Resource... arg1) throws RepositoryException {
		checkOperations(true);
		super.add(arg0, arg1);
	}

	@Override
	public boolean hasStatement(Resource subj, IRI pred, Value obj, boolean includeInferred, Resource... contexts)
			throws RepositoryException {
		checkOperations(false);
		return super.hasStatement(subj, pred, obj, includeInferred, contexts);
	}

	@Override
	public RepositoryResult<Statement> getStatements(Resource subj, IRI pred, Value obj, boolean includeInferred,
			Resource... contexts) throws RepositoryException {
		checkOperations(false);
		return super.getStatements(subj, pred, obj, includeInferred, contexts);
	}

	@Override
	public void add(Resource subject, IRI predicate, Value object, Resource... contexts)
			throws RepositoryException {
		checkOperations(true);
		super.add(subject, predicate, object, contexts);
	}

	@Override
	public SailBooleanQuery prepareBooleanQuery(QueryLanguage ql,
			String queryString, String baseURI)
			throws MalformedQueryException {
		checkOperations(false);
		return super.prepareBooleanQuery(ql, queryString, baseURI);
	}

	@Override
	public SailGraphQuery prepareGraphQuery(QueryLanguage ql,
			String queryString, String baseURI)
			throws MalformedQueryException {
		checkOperations(false);
		return super.prepareGraphQuery(ql, queryString, baseURI);
	}

	@Override
	public SailQuery prepareQuery(QueryLanguage ql, String queryString,
			String baseURI) throws MalformedQueryException {
		checkOperations(false);
		return super.prepareQuery(ql, queryString, baseURI);
	}

	@Override
	public SailTupleQuery prepareTupleQuery(QueryLanguage ql,
			String queryString, String baseURI)
			throws MalformedQueryException {
		checkOperations(false);
		return super.prepareTupleQuery(ql, queryString, baseURI);
	}

	@Override
	public Update prepareUpdate(QueryLanguage ql, String update,
			String baseURI) throws RepositoryException,
			MalformedQueryException {
		checkOperations(true);
		return super.prepareUpdate(ql, update, baseURI);
	}

	private void checkOperations(boolean isWrite) throws FailingRepositoryException {
		checkFail(isWrite);
		checkLatency();
	}

	private void checkLatency() {
		if (rep.latencySimulator != null) {
			rep.latencySimulator.run();
		}
	}

	private void checkFail(boolean isWrite) throws FailingRepositoryException {
		int _operationsCount = 0;
		if (rep.failAfter >= 0) {
			_operationsCount = rep.operationsCount.incrementAndGet();
		} else {
			rep.operationsCount.set(0);
		}

		if (isWrite && !rep.writable) {
			throw new FailingRepositoryException("Operation failed, not writable");
		}
		if (rep.failAfter != -1 && _operationsCount > rep.failAfter) {
			throw new FailingRepositoryException("Operation failed");
		}
	}
}
