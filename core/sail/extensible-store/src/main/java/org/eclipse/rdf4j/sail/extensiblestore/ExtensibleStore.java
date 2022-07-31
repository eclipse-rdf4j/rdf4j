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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics.EvaluationStatisticsEnum;
import org.eclipse.rdf4j.sail.extensiblestore.valuefactory.ExtensibleStatementHelper;
import org.eclipse.rdf4j.sail.helpers.AbstractNotifyingSail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A store where the backing storage can be implemented by the user. Supports up to ReadCommitted.
 * </p>
 * <p>
 * Extend this class and extend ExtensibleStoreConnection. Implement getConnection().
 * </p>
 * <p>
 * Implement the DataStructureInterface and the NamespaceStoreInterface. In your ExtensibleStore-extending class
 * implement a constructor and set the following variables: namespaceStore, dataStructure, dataStructureInferred.
 * </p>
 * <p>
 * Note that the entire ExtensibleStore and all code in this package is experimental. Method signatures, class names,
 * interfaces and the like are likely to change in future releases.
 * </p>
 *
 *
 * @author Håvard Mikkelsen Ottestad
 */
@Experimental
public abstract class ExtensibleStore<T extends DataStructureInterface, N extends NamespaceStoreInterface>
		extends AbstractNotifyingSail implements FederatedServiceResolverClient {

	private static final Logger logger = LoggerFactory.getLogger(ExtensibleStore.class);

	protected ExtensibleSailStore sailStore;

	protected N namespaceStore;

	protected T dataStructure;

	final boolean cacheEnabled;

	private EvaluationStrategyFactory evalStratFactory;
	private SPARQLServiceResolver dependentServiceResolver;
	private FederatedServiceResolver serviceResolver;

	public ExtensibleStore() {
		this(true);
	}

	public ExtensibleStore(boolean cacheEnabled) {
		this.cacheEnabled = cacheEnabled;
	}

	ExtensibleSailStore getSailStore() {
		return sailStore;
	}

	@Override
	synchronized protected void initializeInternal() throws SailException {
		if (sailStore != null) {
			sailStore.close();
		}

		DataStructureInterface dataStructure = Objects.requireNonNull(this.dataStructure);

		if (cacheEnabled) {
			dataStructure = new ReadCache(dataStructure);
		}

		sailStore = new ExtensibleSailStore(dataStructure,
				Objects.requireNonNull(namespaceStore), getEvaluationStatisticsType(), getExtensibleStatementHelper());

		sailStore.init();
		namespaceStore.init();
	}

	@Override
	public List<IsolationLevel> getSupportedIsolationLevels() {
		return Arrays.asList(IsolationLevels.NONE, IsolationLevels.READ_UNCOMMITTED, IsolationLevels.READ_COMMITTED);
	}

	@Override
	public IsolationLevel getDefaultIsolationLevel() {
		return IsolationLevels.READ_COMMITTED;
	}

	@Override
	public void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		this.serviceResolver = resolver;
	}

	@Override
	public ValueFactory getValueFactory() {
		return SimpleValueFactory.getInstance();
	}

	public synchronized EvaluationStrategyFactory getEvaluationStrategyFactory() {
		if (evalStratFactory == null) {
			evalStratFactory = new StrictEvaluationStrategyFactory(getFederatedServiceResolver());
		}
		evalStratFactory.setQuerySolutionCacheThreshold(getIterationCacheSyncThreshold());
		evalStratFactory.setTrackResultSize(isTrackResultSize());
		return evalStratFactory;
	}

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

	@Override
	synchronized protected void shutDownInternal() throws SailException {
		sailStore.close();
		sailStore = null;
		dataStructure = null;
		namespaceStore = null;
	}

	// override this method to change which evaluation statistics to use
	public EvaluationStatisticsEnum getEvaluationStatisticsType() {
		return EvaluationStatisticsEnum.dynamic;
	}

	public ExtensibleStatementHelper getExtensibleStatementHelper() {
		return ExtensibleStatementHelper.getDefaultImpl();
	}
}
