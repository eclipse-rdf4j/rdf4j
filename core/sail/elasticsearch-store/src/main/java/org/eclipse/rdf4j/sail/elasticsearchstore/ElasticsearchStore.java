/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractNotifyingSail;

import java.util.Collections;
import java.util.List;

public class ElasticsearchStore extends AbstractNotifyingSail implements FederatedServiceResolverClient {

	private ElasticsearchSailStore sailStore;

	public ElasticsearchStore() {
		sailStore = new ElasticsearchSailStore();
	}

	@Override
	public List<IsolationLevel> getSupportedIsolationLevels() {
		return Collections.singletonList(IsolationLevels.NONE);
	}

	@Override
	public IsolationLevel getDefaultIsolationLevel() {
		return IsolationLevels.NONE;
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {

	}

	@Override
	protected void shutDownInternal() throws SailException {

	}

	@Override
	protected NotifyingSailConnection getConnectionInternal() throws SailException {
		return new ElasticsearchStoreConnection(this, sailStore, getEvaluationStrategyFactory());
	}

	@Override
	public boolean isWritable() throws SailException {
		return false;
	}

	@Override
	public ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	private EvaluationStrategyFactory evalStratFactory;

	public synchronized EvaluationStrategyFactory getEvaluationStrategyFactory() {
		if (evalStratFactory == null) {
			evalStratFactory = new StrictEvaluationStrategyFactory(getFederatedServiceResolver());
		}
		evalStratFactory.setQuerySolutionCacheThreshold(0);
		return evalStratFactory;
	}

	/**
	 * independent life cycle
	 */
	private FederatedServiceResolver serviceResolver;

	/**
	 * dependent life cycle
	 */
	private SPARQLServiceResolver dependentServiceResolver;

	public synchronized FederatedServiceResolver getFederatedServiceResolver() {
		if (serviceResolver == null) {
			if (dependentServiceResolver == null) {
				dependentServiceResolver = new SPARQLServiceResolver();
			}
			setFederatedServiceResolver(dependentServiceResolver);
		}
		return serviceResolver;
	}

	public void setEvaluationStrategyFactory(EvaluationStrategyFactory evalStratFactory) {
		this.evalStratFactory = evalStratFactory;

	}
}
