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
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.eclipse.rdf4j.sail.base.config.BaseSailConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

/**
 * @author Arjohn Kampman
 */
public class NativeStoreConfig extends BaseSailConfig {

	private static final boolean USE_CONFIG = "true"
			.equalsIgnoreCase(System.getProperty("org.eclipse.rdf4j.model.vocabulary.experimental.enableConfig"));

	private String tripleIndexes;
	private boolean forceSync = false;
	private int valueCacheSize = -1;
	private int valueIDCacheSize = -1;
	private int namespaceCacheSize = -1;
	private int namespaceIDCacheSize = -1;

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
		m.setNamespace(CONFIG.NS);

		if (tripleIndexes != null) {
			if (USE_CONFIG) {
				m.add(implNode, CONFIG.Native.tripleIndexes, literal(tripleIndexes));
			} else {
				m.add(implNode, TRIPLE_INDEXES, literal(tripleIndexes));
			}
		}
		if (forceSync) {
			if (USE_CONFIG) {
				m.add(implNode, CONFIG.Native.forceSync, literal(forceSync));
			} else {
				m.add(implNode, FORCE_SYNC, literal(forceSync));
			}
		}
		if (valueCacheSize >= 0) {
			if (USE_CONFIG) {
				m.add(implNode, CONFIG.Native.valueCacheSize, literal(valueCacheSize));
			} else {
				m.add(implNode, VALUE_CACHE_SIZE, literal(valueCacheSize));
			}
		}
		if (valueIDCacheSize >= 0) {
			if (USE_CONFIG) {
				m.add(implNode, CONFIG.Native.valueIDCacheSize, literal(valueIDCacheSize));
			} else {
				m.add(implNode, VALUE_ID_CACHE_SIZE, literal(valueIDCacheSize));
			}
		}
		if (namespaceCacheSize >= 0) {
			if (USE_CONFIG) {
				m.add(implNode, CONFIG.Native.namespaceCacheSize, literal(namespaceCacheSize));
			} else {
				m.add(implNode, NAMESPACE_CACHE_SIZE, literal(namespaceCacheSize));
			}
		}
		if (namespaceIDCacheSize >= 0) {
			if (USE_CONFIG) {
				m.add(implNode, CONFIG.Native.namespaceIDCacheSize, literal(namespaceIDCacheSize));
			} else {
				m.add(implNode, NAMESPACE_ID_CACHE_SIZE, literal(namespaceIDCacheSize));
			}
		}

		return implNode;
	}

	@Override
	public void parse(Model m, Resource implNode) throws SailConfigException {
		super.parse(m, implNode);

		try {

			Configurations.getLiteralValue(m, implNode, CONFIG.Native.tripleIndexes, TRIPLE_INDEXES)
					.ifPresent(lit -> setTripleIndexes(lit.getLabel()));

			Configurations.getLiteralValue(m, implNode, CONFIG.Native.forceSync, FORCE_SYNC)
					.ifPresent(lit -> {
						try {
							setForceSync(lit.booleanValue());
						} catch (IllegalArgumentException e) {
							throw new SailConfigException(
									"Boolean value required for " + CONFIG.Native.forceSync + " property, found "
											+ lit);
						}
					});

			Configurations.getLiteralValue(m, implNode, CONFIG.Native.valueCacheSize, VALUE_CACHE_SIZE)
					.ifPresent(lit -> {
						try {
							setValueCacheSize(lit.intValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Integer value required for " + CONFIG.Native.valueCacheSize + " property, found "
											+ lit);
						}
					});

			Configurations.getLiteralValue(m, implNode, CONFIG.Native.valueIDCacheSize, VALUE_ID_CACHE_SIZE)
					.ifPresent(lit -> {
						try {
							setValueIDCacheSize(lit.intValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Integer value required for " + CONFIG.Native.valueIDCacheSize + " property, found "
											+ lit);
						}
					});

			Configurations.getLiteralValue(m, implNode, CONFIG.Native.namespaceCacheSize, NAMESPACE_CACHE_SIZE)
					.ifPresent(lit -> {
						try {
							setNamespaceCacheSize(lit.intValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Integer value required for " + CONFIG.Native.namespaceCacheSize
											+ " property, found " + lit);
						}
					});

			Configurations.getLiteralValue(m, implNode, CONFIG.Native.namespaceIDCacheSize, NAMESPACE_ID_CACHE_SIZE)
					.ifPresent(lit -> {
						try {
							setNamespaceIDCacheSize(lit.intValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Integer value required for " + CONFIG.Native.namespaceIDCacheSize
											+ " property, found " + lit);
						}
					});
		} catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
