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

import static org.eclipse.rdf4j.sail.memory.config.MemoryStoreSchema.NAMESPACE;
import static org.eclipse.rdf4j.sail.memory.config.MemoryStoreSchema.PERSIST;
import static org.eclipse.rdf4j.sail.memory.config.MemoryStoreSchema.SYNC_DELAY;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.BooleanLiteral;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
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

		graph.setNamespace("ms", NAMESPACE);
		if (persist) {
			graph.add(implNode, PERSIST, BooleanLiteral.TRUE);
		}

		if (syncDelay != 0) {
			graph.add(implNode, SYNC_DELAY, SimpleValueFactory.getInstance().createLiteral(syncDelay));
		}

		return implNode;
	}

	@Override
	public void parse(Model graph, Resource implNode) throws SailConfigException {
		super.parse(graph, implNode);

		try {

			Models.objectLiteral(graph.getStatements(implNode, PERSIST, null)).ifPresent(persistValue -> {
				try {
					setPersist((persistValue).booleanValue());
				} catch (IllegalArgumentException e) {
					throw new SailConfigException(
							"Boolean value required for " + PERSIST + " property, found " + persistValue);
				}
			});

			Models.objectLiteral(graph.getStatements(implNode, SYNC_DELAY, null)).ifPresent(syncDelayValue -> {
				try {
					setSyncDelay((syncDelayValue).longValue());
				} catch (NumberFormatException e) {
					throw new SailConfigException(
							"Long integer value required for " + SYNC_DELAY + " property, found " + syncDelayValue);
				}
			});
		} catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
