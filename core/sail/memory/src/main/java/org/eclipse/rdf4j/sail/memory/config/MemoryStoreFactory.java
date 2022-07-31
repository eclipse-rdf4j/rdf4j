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
package org.eclipse.rdf4j.sail.memory.config;

import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.config.SailConfigException;
import org.eclipse.rdf4j.sail.config.SailFactory;
import org.eclipse.rdf4j.sail.config.SailImplConfig;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

/**
 * A {@link SailFactory} that creates {@link MemoryStore}s based on RDF configuration data.
 *
 * @author Arjohn Kampman
 */
public class MemoryStoreFactory implements SailFactory {

	/**
	 * The type of repositories that are created by this factory.
	 *
	 * @see SailFactory#getSailType()
	 */
	public static final String SAIL_TYPE = "openrdf:MemoryStore";

	/**
	 * Returns the Sail's type: <var>openrdf:MemoryStore</var>.
	 */
	@Override
	public String getSailType() {
		return SAIL_TYPE;
	}

	@Override
	public SailImplConfig getConfig() {
		return new MemoryStoreConfig();
	}

	@Override
	public Sail getSail(SailImplConfig config) throws SailConfigException {
		if (!SAIL_TYPE.equals(config.getType())) {
			throw new SailConfigException("Invalid Sail type: " + config.getType());
		}

		MemoryStore memoryStore = new MemoryStore();

		if (config instanceof MemoryStoreConfig) {
			MemoryStoreConfig memConfig = (MemoryStoreConfig) config;

			memoryStore.setPersist(memConfig.getPersist());
			memoryStore.setSyncDelay(memConfig.getSyncDelay());

			if (memConfig.getIterationCacheSyncThreshold() > 0) {
				memoryStore.setIterationCacheSyncThreshold(memConfig.getIterationCacheSyncThreshold());
			}

			EvaluationStrategyFactory evalStratFactory = memConfig.getEvaluationStrategyFactory();
			if (evalStratFactory != null) {
				memoryStore.setEvaluationStrategyFactory(evalStratFactory);
			}
		}

		return memoryStore;
	}
}
