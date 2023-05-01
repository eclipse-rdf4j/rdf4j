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

import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.eclipse.rdf4j.sail.memory.config.MemoryStoreSchema.PERSIST;
import static org.eclipse.rdf4j.sail.memory.config.MemoryStoreSchema.SYNC_DELAY;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.util.Configurations;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.vocabulary.Config;
import org.eclipse.rdf4j.sail.base.config.BaseSailConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

/**
 * @author Arjohn Kampman
 */
public class MemoryStoreConfig extends BaseSailConfig {

	private boolean persist = false;

	private long syncDelay = 0L;

	public MemoryStoreConfig() {
		super(MemoryStoreFactory.SAIL_TYPE);
	}

	public MemoryStoreConfig(boolean persist) {
		this();
		setPersist(persist);
	}

	public MemoryStoreConfig(boolean persist, long syncDelay) {
		this(persist);
		setSyncDelay(syncDelay);
	}

	public boolean getPersist() {
		return persist;
	}

	public void setPersist(boolean persist) {
		this.persist = persist;
	}

	public long getSyncDelay() {
		return syncDelay;
	}

	public void setSyncDelay(long syncDelay) {
		this.syncDelay = syncDelay;
	}

	@Override
	public Resource export(Model graph) {
		Resource implNode = super.export(graph);
		graph.setNamespace(Config.NS);

		if (persist) {
			graph.add(implNode, Config.MemoryStore.persist, BooleanLiteral.TRUE);
		}

		if (syncDelay != 0) {
			graph.add(implNode, Config.MemoryStore.syncDelay, literal(syncDelay));
		}

		return implNode;
	}

	@Override
	public void parse(Model graph, Resource implNode) throws SailConfigException {
		super.parse(graph, implNode);

		try {

			Configurations.getLiteralValue(graph, implNode, Config.MemoryStore.persist, PERSIST)
					.ifPresent(persistValue -> {
						try {
							setPersist((persistValue).booleanValue());
						} catch (IllegalArgumentException e) {
							throw new SailConfigException(
									"Boolean value required for " + Config.MemoryStore.persist + " property, found "
											+ persistValue);
						}
					});

			Configurations.getLiteralValue(graph, implNode, Config.MemoryStore.syncDelay, SYNC_DELAY)
					.ifPresent(syncDelayValue -> {
						try {
							setSyncDelay((syncDelayValue).longValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Long integer value required for " + Config.MemoryStore.syncDelay
											+ " property, found "
											+ syncDelayValue);
						}
					});
		} catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
