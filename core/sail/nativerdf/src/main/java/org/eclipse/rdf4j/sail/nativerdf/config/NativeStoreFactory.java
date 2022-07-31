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
package org.eclipse.rdf4j.sail.nativerdf.config;

import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

/**
 * A {@link SailFactory} that creates {@link NativeStore}s based on RDF configuration data.
 *
 * @author Arjohn Kampman
 */
public class NativeStoreFactory implements SailFactory {

	/**
	 * The type of repositories that are created by this factory.
	 *
	 * @see SailFactory#getSailType()
	 */
	public static final String SAIL_TYPE = "openrdf:NativeStore";

	/**
	 * Returns the Sail's type: <var>openrdf:NativeStore</var>.
	 */
	@Override
	public String getSailType() {
		return SAIL_TYPE;
	}

	@Override
	public SailImplConfig getConfig() {
		return new NativeStoreConfig();
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}

		NativeStore nativeStore = new NativeStore();

		if (config instanceof NativeStoreConfig) {
			NativeStoreConfig nativeConfig = (NativeStoreConfig) config;

			nativeStore.setTripleIndexes(nativeConfig.getTripleIndexes());
			nativeStore.setForceSync(nativeConfig.getForceSync());

			if (nativeConfig.getValueCacheSize() >= 0) {
				nativeStore.setValueCacheSize(nativeConfig.getValueCacheSize());
			}
			if (nativeConfig.getValueIDCacheSize() >= 0) {
				nativeStore.setValueIDCacheSize(nativeConfig.getValueIDCacheSize());
			}
			if (nativeConfig.getNamespaceCacheSize() >= 0) {
				nativeStore.setNamespaceCacheSize(nativeConfig.getNamespaceCacheSize());
			}
			if (nativeConfig.getNamespaceIDCacheSize() >= 0) {
				nativeStore.setNamespaceIDCacheSize(nativeConfig.getNamespaceIDCacheSize());
			}
			if (nativeConfig.getIterationCacheSyncThreshold() > 0) {
				nativeStore.setIterationCacheSyncThreshold(nativeConfig.getIterationCacheSyncThreshold());
			}

			EvaluationStrategyFactory evalStratFactory = nativeConfig.getEvaluationStrategyFactory();
			if (evalStratFactory != null) {
				nativeStore.setEvaluationStrategyFactory(evalStratFactory);
			}
		}

		return nativeStore;
	}
}
