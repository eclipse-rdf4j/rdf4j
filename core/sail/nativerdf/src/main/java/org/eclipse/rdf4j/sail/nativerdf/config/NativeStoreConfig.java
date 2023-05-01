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

import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreSchema.FORCE_SYNC;
import static org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreSchema.NAMESPACE_CACHE_SIZE;
import static org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreSchema.NAMESPACE_ID_CACHE_SIZE;
import static org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreSchema.TRIPLE_INDEXES;
import static org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreSchema.VALUE_CACHE_SIZE;
import static org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreSchema.VALUE_ID_CACHE_SIZE;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.Configurations;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.vocabulary.Config;
import org.eclipse.rdf4j.sail.base.config.BaseSailConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

/**
 * @author Arjohn Kampman
 */
public class NativeStoreConfig extends BaseSailConfig {

	/*-----------*
	 * Variables *
	 *-----------*/

	private String tripleIndexes;

	private boolean forceSync = false;

	private int valueCacheSize = -1;

	private int valueIDCacheSize = -1;

	private int namespaceCacheSize = -1;

	private int namespaceIDCacheSize = -1;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public NativeStoreConfig() {
		super(NativeStoreFactory.SAIL_TYPE);
	}

	public NativeStoreConfig(String tripleIndexes) {
		this();
		setTripleIndexes(tripleIndexes);
	}

	public NativeStoreConfig(String tripleIndexes, boolean forceSync) {
		this(tripleIndexes);
		setForceSync(forceSync);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public String getTripleIndexes() {
		return tripleIndexes;
	}

	public void setTripleIndexes(String tripleIndexes) {
		this.tripleIndexes = tripleIndexes;
	}

	public boolean getForceSync() {
		return forceSync;
	}

	public void setForceSync(boolean forceSync) {
		this.forceSync = forceSync;
	}

	public int getValueCacheSize() {
		return valueCacheSize;
	}

	public void setValueCacheSize(int valueCacheSize) {
		this.valueCacheSize = valueCacheSize;
	}

	public int getValueIDCacheSize() {
		return valueIDCacheSize;
	}

	public void setValueIDCacheSize(int valueIDCacheSize) {
		this.valueIDCacheSize = valueIDCacheSize;
	}

	public int getNamespaceCacheSize() {
		return namespaceCacheSize;
	}

	public void setNamespaceCacheSize(int namespaceCacheSize) {
		this.namespaceCacheSize = namespaceCacheSize;
	}

	public int getNamespaceIDCacheSize() {
		return namespaceIDCacheSize;
	}

	public void setNamespaceIDCacheSize(int namespaceIDCacheSize) {
		this.namespaceIDCacheSize = namespaceIDCacheSize;
	}

	@Override
	public Resource export(Model m) {
		Resource implNode = super.export(m);
		m.setNamespace(Config.NS);

		if (tripleIndexes != null) {
			m.add(implNode, Config.NativeStore.tripleIndexes, literal(tripleIndexes));
		}
		if (forceSync) {
			m.add(implNode, Config.NativeStore.forceSync, literal(forceSync));
		}
		if (valueCacheSize >= 0) {
			m.add(implNode, Config.NativeStore.valueCacheSize, literal(valueCacheSize));
		}
		if (valueIDCacheSize >= 0) {
			m.add(implNode, Config.NativeStore.valueIDCacheSize, literal(valueIDCacheSize));
		}
		if (namespaceCacheSize >= 0) {
			m.add(implNode, Config.NativeStore.namespaceCacheSize, literal(namespaceCacheSize));
		}
		if (namespaceIDCacheSize >= 0) {
			m.add(implNode, Config.NativeStore.namespaceIDCacheSize, literal(namespaceIDCacheSize));
		}

		return implNode;
	}

	@Override
	public void parse(Model m, Resource implNode) throws SailConfigException {
		super.parse(m, implNode);

		try {

			Configurations.getLiteralValue(m, implNode, Config.NativeStore.tripleIndexes, TRIPLE_INDEXES)
					.ifPresent(lit -> setTripleIndexes(lit.getLabel()));

			Configurations.getLiteralValue(m, implNode, Config.NativeStore.forceSync, FORCE_SYNC)
					.ifPresent(lit -> {
						try {
							setForceSync(lit.booleanValue());
						} catch (IllegalArgumentException e) {
							throw new SailConfigException(
									"Boolean value required for " + Config.NativeStore.forceSync + " property, found "
											+ lit);
						}
					});

			Configurations.getLiteralValue(m, implNode, Config.NativeStore.valueCacheSize, VALUE_CACHE_SIZE)
					.ifPresent(lit -> {
						try {
							setValueCacheSize(lit.intValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Integer value required for " + Config.NativeStore.valueCacheSize
											+ " property, found "
											+ lit);
						}
					});

			Configurations.getLiteralValue(m, implNode, Config.NativeStore.valueIDCacheSize, VALUE_ID_CACHE_SIZE)
					.ifPresent(lit -> {
						try {
							setValueIDCacheSize(lit.intValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Integer value required for " + Config.NativeStore.valueIDCacheSize
											+ " property, found "
											+ lit);
						}
					});

			Configurations.getLiteralValue(m, implNode, Config.NativeStore.namespaceCacheSize, NAMESPACE_CACHE_SIZE)
					.ifPresent(lit -> {
						try {
							setNamespaceCacheSize(lit.intValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Integer value required for " + Config.NativeStore.namespaceCacheSize
											+ " property, found " + lit);
						}
					});

			Configurations
					.getLiteralValue(m, implNode, Config.NativeStore.namespaceIDCacheSize, NAMESPACE_ID_CACHE_SIZE)
					.ifPresent(lit -> {
						try {
							setNamespaceIDCacheSize(lit.intValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Integer value required for " + Config.NativeStore.namespaceIDCacheSize
											+ " property, found " + lit);
						}
					});
		} catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
