/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.config;

import java.time.Duration;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.sail.base.config.BaseSailConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

/**
 *
 */
public class LmdbStoreConfig extends BaseSailConfig {
	/**
	 * The default size of the triple database.
	 */
	public static final long TRIPLE_DB_SIZE = 10_485_760; // 10 MiB

	/**
	 * The default size of the value database.
	 */
	public static final long VALUE_DB_SIZE = 10_485_760; // 10 MiB

	/**
	 * The default value cache size.
	 */
	public static final int VALUE_CACHE_SIZE = 512;

	/**
	 * The default value id cache size.
	 */
	public static final int VALUE_ID_CACHE_SIZE = 128;

	/**
	 * The default namespace cache size.
	 */
	public static final int NAMESPACE_CACHE_SIZE = 64;

	/**
	 * The default namespace id cache size.
	 */
	public static final int NAMESPACE_ID_CACHE_SIZE = 32;

	private String tripleIndexes;

	private long tripleDBSize = -1;

	private long valueDBSize = -1;

	private boolean forceSync = false;

	private int valueCacheSize = -1;

	private int valueIDCacheSize = -1;

	private int namespaceCacheSize = -1;

	private int namespaceIDCacheSize = -1;

	private boolean autoGrow = true;

	private boolean pageCardinalityEstimator = true;

	private long valueEvictionInterval = Duration.ofSeconds(60).toMillis();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public LmdbStoreConfig() {
		super(LmdbStoreFactory.SAIL_TYPE);
	}

	public LmdbStoreConfig(String tripleIndexes) {
		this();
		setTripleIndexes(tripleIndexes);
	}

	public LmdbStoreConfig(String tripleIndexes, boolean forceSync) {
		this(tripleIndexes);
		setForceSync(forceSync);
	}

	/*---------*
	 * Methods *
	 *---------*/
	public String getTripleIndexes() {
		return tripleIndexes;
	}

	public LmdbStoreConfig setTripleIndexes(String tripleIndexes) {
		this.tripleIndexes = tripleIndexes;
		return this;
	}

	public LmdbStoreConfig setTripleDBSize(long tripleDBSize) {
		this.tripleDBSize = tripleDBSize;
		return this;
	}

	public long getTripleDBSize() {
		return tripleDBSize >= 0 ? tripleDBSize : TRIPLE_DB_SIZE;
	}

	public LmdbStoreConfig setValueDBSize(long valueDBSize) {
		this.valueDBSize = valueDBSize;
		return this;
	}

	public long getValueDBSize() {
		return valueDBSize >= 0 ? valueDBSize : VALUE_DB_SIZE;
	}

	public boolean getForceSync() {
		return forceSync;
	}

	/**
	 * Flag indicating whether updates should be synced to disk forcefully. This may have a severe impact on write
	 * performance. By default, this feature is disabled.
	 */
	public LmdbStoreConfig setForceSync(boolean forceSync) {
		this.forceSync = forceSync;
		return this;
	}

	public int getValueCacheSize() {
		return valueCacheSize >= 0 ? valueCacheSize : VALUE_CACHE_SIZE;
	}

	public LmdbStoreConfig setValueCacheSize(int valueCacheSize) {
		this.valueCacheSize = valueCacheSize;
		return this;
	}

	public int getValueIDCacheSize() {
		return valueIDCacheSize >= 0 ? valueIDCacheSize : VALUE_ID_CACHE_SIZE;
	}

	public LmdbStoreConfig setValueIDCacheSize(int valueIDCacheSize) {
		this.valueIDCacheSize = valueIDCacheSize;
		return this;
	}

	public int getNamespaceCacheSize() {
		return namespaceCacheSize >= 0 ? namespaceCacheSize : NAMESPACE_CACHE_SIZE;
	}

	public LmdbStoreConfig setNamespaceCacheSize(int namespaceCacheSize) {
		this.namespaceCacheSize = namespaceCacheSize;
		return this;
	}

	public int getNamespaceIDCacheSize() {
		return namespaceIDCacheSize >= 0 ? namespaceIDCacheSize : NAMESPACE_ID_CACHE_SIZE;
	}

	public LmdbStoreConfig setNamespaceIDCacheSize(int namespaceIDCacheSize) {
		this.namespaceIDCacheSize = namespaceIDCacheSize;
		return this;
	}

	public boolean getAutoGrow() {
		return autoGrow;
	}

	public LmdbStoreConfig setAutoGrow(boolean autoGrow) {
		this.autoGrow = autoGrow;
		return this;
	}

	public long getValueEvictionInterval() {
		return valueEvictionInterval;
	}

	public LmdbStoreConfig setValueEvictionInterval(long valueEvictionInterval) {
		this.valueEvictionInterval = valueEvictionInterval;
		return this;
	}

	public boolean getPageCardinalityEstimator() {
		return pageCardinalityEstimator;
	}

	public LmdbStoreConfig setPageCardinalityEstimator(boolean pageCardinalityEstimator) {
		this.pageCardinalityEstimator = pageCardinalityEstimator;
		return this;
	}

	@Override
	public Resource export(Model m) {
		Resource implNode = super.export(m);
		ValueFactory vf = SimpleValueFactory.getInstance();

		m.setNamespace("ns", LmdbStoreSchema.NAMESPACE);
		if (tripleIndexes != null) {
			m.add(implNode, LmdbStoreSchema.TRIPLE_INDEXES, vf.createLiteral(tripleIndexes));
		}
		if (tripleDBSize >= 0) {
			m.add(implNode, LmdbStoreSchema.TRIPLE_DB_SIZE, vf.createLiteral(tripleDBSize));
		}
		if (valueDBSize >= 0) {
			m.add(implNode, LmdbStoreSchema.VALUE_DB_SIZE, vf.createLiteral(valueDBSize));
		}
		if (forceSync) {
			m.add(implNode, LmdbStoreSchema.FORCE_SYNC, vf.createLiteral(true));
		}
		if (valueCacheSize >= 0) {
			m.add(implNode, LmdbStoreSchema.VALUE_CACHE_SIZE, vf.createLiteral(valueCacheSize));
		}
		if (valueIDCacheSize >= 0) {
			m.add(implNode, LmdbStoreSchema.VALUE_ID_CACHE_SIZE, vf.createLiteral(valueIDCacheSize));
		}
		if (namespaceCacheSize >= 0) {
			m.add(implNode, LmdbStoreSchema.NAMESPACE_CACHE_SIZE, vf.createLiteral(namespaceCacheSize));
		}
		if (namespaceIDCacheSize >= 0) {
			m.add(implNode, LmdbStoreSchema.NAMESPACE_ID_CACHE_SIZE, vf.createLiteral(namespaceIDCacheSize));
		}
		if (!autoGrow) {
			m.add(implNode, LmdbStoreSchema.AUTO_GROW, vf.createLiteral(false));
		}
		if (!pageCardinalityEstimator) {
			m.add(implNode, LmdbStoreSchema.PAGE_CARDINALITY_ESTIMATOR, vf.createLiteral(false));
		}
		if (valueEvictionInterval != Duration.ofSeconds(60).toMillis()) {
			m.add(implNode, LmdbStoreSchema.VALUE_EVICTION_INTERVAL, vf.createLiteral(valueEvictionInterval));
		}
		return implNode;
	}

	@Override
	public void parse(Model m, Resource implNode) throws SailConfigException {
		super.parse(m, implNode);

		try {
			Models.objectLiteral(m.getStatements(implNode, LmdbStoreSchema.TRIPLE_INDEXES, null))
					.ifPresent(lit -> setTripleIndexes(lit.getLabel()));

			Models.objectLiteral(m.getStatements(implNode, LmdbStoreSchema.TRIPLE_DB_SIZE, null))
					.ifPresent(lit -> {
						try {
							setTripleDBSize(lit.longValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Long value required for " + LmdbStoreSchema.TRIPLE_DB_SIZE
											+ " property, found " + lit);
						}
					});

			Models.objectLiteral(m.getStatements(implNode, LmdbStoreSchema.VALUE_DB_SIZE, null))
					.ifPresent(lit -> {
						try {
							setValueDBSize(lit.longValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Long value required for " + LmdbStoreSchema.VALUE_DB_SIZE
											+ " property, found " + lit);
						}
					});

			Models.objectLiteral(m.getStatements(implNode, LmdbStoreSchema.FORCE_SYNC, null)).ifPresent(lit -> {
				try {
					setForceSync(lit.booleanValue());
				} catch (IllegalArgumentException e) {
					throw new SailConfigException(
							"Boolean value required for " + LmdbStoreSchema.FORCE_SYNC + " property, found " + lit);
				}
			});

			Models.objectLiteral(m.getStatements(implNode, LmdbStoreSchema.VALUE_CACHE_SIZE, null)).ifPresent(lit -> {
				try {
					setValueCacheSize(lit.intValue());
				} catch (NumberFormatException e) {
					throw new SailConfigException(
							"Integer value required for " + LmdbStoreSchema.VALUE_CACHE_SIZE + " property, found "
									+ lit);
				}
			});

			Models.objectLiteral(m.getStatements(implNode, LmdbStoreSchema.VALUE_ID_CACHE_SIZE, null))
					.ifPresent(lit -> {
						try {
							setValueIDCacheSize(lit.intValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Integer value required for " + LmdbStoreSchema.VALUE_ID_CACHE_SIZE
											+ " property, found " + lit);
						}
					});

			Models.objectLiteral(m.getStatements(implNode, LmdbStoreSchema.NAMESPACE_CACHE_SIZE, null))
					.ifPresent(lit -> {
						try {
							setNamespaceCacheSize(lit.intValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Integer value required for " + LmdbStoreSchema.NAMESPACE_CACHE_SIZE
											+ " property, found " + lit);
						}
					});

			Models.objectLiteral(m.getStatements(implNode, LmdbStoreSchema.NAMESPACE_ID_CACHE_SIZE, null))
					.ifPresent(lit -> {
						try {
							setNamespaceIDCacheSize(lit.intValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Integer value required for " + LmdbStoreSchema.NAMESPACE_ID_CACHE_SIZE
											+ " property, found " + lit);
						}
					});

			Models.objectLiteral(m.getStatements(implNode, LmdbStoreSchema.AUTO_GROW, null)).ifPresent(lit -> {
				try {
					setAutoGrow(lit.booleanValue());
				} catch (IllegalArgumentException e) {
					throw new SailConfigException(
							"Boolean value required for " + LmdbStoreSchema.AUTO_GROW + " property, found " + lit);
				}
			});

			Models.objectLiteral(m.getStatements(implNode, LmdbStoreSchema.PAGE_CARDINALITY_ESTIMATOR, null))
					.ifPresent(lit -> {
						try {
							setPageCardinalityEstimator(lit.booleanValue());
						} catch (IllegalArgumentException e) {
							throw new SailConfigException(
									"Boolean value required for " + LmdbStoreSchema.PAGE_CARDINALITY_ESTIMATOR
											+ " property, found " + lit);
						}
					});

			Models.objectLiteral(m.getStatements(implNode, LmdbStoreSchema.VALUE_EVICTION_INTERVAL, null))
					.ifPresent(lit -> {
						try {
							setValueEvictionInterval(lit.longValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException(
									"Long value required for " + LmdbStoreSchema.VALUE_EVICTION_INTERVAL
											+ " property, found " + lit);
						}
					});
		} catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
