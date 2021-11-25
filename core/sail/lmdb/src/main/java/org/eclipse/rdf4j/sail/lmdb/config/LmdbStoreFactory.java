/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.config;

import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;

/**
 * A {@link SailFactory} that creates {@link LmdbStore}s based on RDF configuration data.
 *
 */
public class LmdbStoreFactory implements SailFactory {

	/**
	 * The type of repositories that are created by this factory.
	 *
	 * @see SailFactory#getSailType()
	 */
	public static final String SAIL_TYPE = "openrdf:LmdbStore";

	/**
	 * Returns the Sail's type: <tt>openrdf:LmdbStore</tt>.
	 */
	@Override
	public String getSailType() {
		return SAIL_TYPE;
	}

	@Override
	public SailImplConfig getConfig() {
		return new LmdbStoreConfig();
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}

		LmdbStore lmdbStore = new LmdbStore();

		if (config instanceof LmdbStoreConfig) {
			LmdbStoreConfig lmdbConfig = (LmdbStoreConfig) config;

			lmdbStore.setTripleIndexes(lmdbConfig.getTripleIndexes());
			lmdbStore.setForceSync(lmdbConfig.getForceSync());

			if (lmdbConfig.getValueCacheSize() >= 0) {
				lmdbStore.setValueCacheSize(lmdbConfig.getValueCacheSize());
			}
			if (lmdbConfig.getValueIDCacheSize() >= 0) {
				lmdbStore.setValueIDCacheSize(lmdbConfig.getValueIDCacheSize());
			}
			if (lmdbConfig.getNamespaceCacheSize() >= 0) {
				lmdbStore.setNamespaceCacheSize(lmdbConfig.getNamespaceCacheSize());
			}
			if (lmdbConfig.getNamespaceIDCacheSize() >= 0) {
				lmdbStore.setNamespaceIDCacheSize(lmdbConfig.getNamespaceIDCacheSize());
			}
			if (lmdbConfig.getIterationCacheSyncThreshold() > 0) {
				lmdbStore.setIterationCacheSyncThreshold(lmdbConfig.getIterationCacheSyncThreshold());
			}

			EvaluationStrategyFactory evalStratFactory = lmdbConfig.getEvaluationStrategyFactory();
			if (evalStratFactory != null) {
				lmdbStore.setEvaluationStrategyFactory(evalStratFactory);
			}
		}

		return lmdbStore;
	}
}
