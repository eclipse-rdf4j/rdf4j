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

	private String tripleIndexes;
	private boolean forceSync = false;
	private int valueCacheSize = -1;
	private int valueIDCacheSize = -1;
	private int namespaceCacheSize = -1;
	private int namespaceIDCacheSize = -1;

	// WAL: expose max segment bytes via config (optional)
	private long walMaxSegmentBytes = -1L;

	// Additional WAL configuration options
	private int walQueueCapacity = -1;
	private int walBatchBufferBytes = -1;
	private String walSyncPolicy; // expects one of ValueStoreWalConfig.SyncPolicy
	private long walSyncIntervalMillis = -1L;
	private long walIdlePollIntervalMillis = -1L;
	private String walDirectoryName; // relative to dataDir

	// When true, WAL bootstrap runs synchronously during open before accepting new values
	private boolean walSyncBootstrapOnOpen = false;

	// When true, reconstruct ValueStore from WAL during open if empty/missing
	private boolean walAutoRecoverOnOpen = false;

	// When false, completely disable the ValueStore WAL
	private boolean walEnabled = true;

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

	public long getWalMaxSegmentBytes() {
		return walMaxSegmentBytes;
	}

	public void setWalMaxSegmentBytes(long walMaxSegmentBytes) {
		this.walMaxSegmentBytes = walMaxSegmentBytes;
	}

	public int getWalQueueCapacity() {
		return walQueueCapacity;
	}

	public void setWalQueueCapacity(int walQueueCapacity) {
		this.walQueueCapacity = walQueueCapacity;
	}

	public int getWalBatchBufferBytes() {
		return walBatchBufferBytes;
	}

	public void setWalBatchBufferBytes(int walBatchBufferBytes) {
		this.walBatchBufferBytes = walBatchBufferBytes;
	}

	public String getWalSyncPolicy() {
		return walSyncPolicy;
	}

	public void setWalSyncPolicy(String walSyncPolicy) {
		this.walSyncPolicy = walSyncPolicy;
	}

	public long getWalSyncIntervalMillis() {
		return walSyncIntervalMillis;
	}

	public void setWalSyncIntervalMillis(long walSyncIntervalMillis) {
		this.walSyncIntervalMillis = walSyncIntervalMillis;
	}

	public long getWalIdlePollIntervalMillis() {
		return walIdlePollIntervalMillis;
	}

	public void setWalIdlePollIntervalMillis(long walIdlePollIntervalMillis) {
		this.walIdlePollIntervalMillis = walIdlePollIntervalMillis;
	}

	public String getWalDirectoryName() {
		return walDirectoryName;
	}

	public void setWalDirectoryName(String walDirectoryName) {
		this.walDirectoryName = walDirectoryName;
	}

	public boolean getWalSyncBootstrapOnOpen() {
		return walSyncBootstrapOnOpen;
	}

	public void setWalSyncBootstrapOnOpen(boolean walSyncBootstrapOnOpen) {
		this.walSyncBootstrapOnOpen = walSyncBootstrapOnOpen;
	}

	public boolean getWalAutoRecoverOnOpen() {
		return walAutoRecoverOnOpen;
	}

	public void setWalAutoRecoverOnOpen(boolean walAutoRecoverOnOpen) {
		this.walAutoRecoverOnOpen = walAutoRecoverOnOpen;
	}

	public boolean getWalEnabled() {
		return walEnabled;
	}

	public void setWalEnabled(boolean walEnabled) {
		this.walEnabled = walEnabled;
	}

	@Override
	public Resource export(Model m) {
		if (Configurations.useLegacyConfig()) {
			return exportLegacy(m);
		}

		Resource implNode = super.export(m);
		m.setNamespace(CONFIG.NS);

		if (tripleIndexes != null) {
			m.add(implNode, CONFIG.Native.tripleIndexes, literal(tripleIndexes));
		}
		if (forceSync) {
			m.add(implNode, CONFIG.Native.forceSync, literal(forceSync));
		}
		if (valueCacheSize >= 0) {
			m.add(implNode, CONFIG.Native.valueCacheSize, literal(valueCacheSize));
		}
		if (valueIDCacheSize >= 0) {
			m.add(implNode, CONFIG.Native.valueIDCacheSize, literal(valueIDCacheSize));
		}
		if (namespaceCacheSize >= 0) {
			m.add(implNode, CONFIG.Native.namespaceCacheSize, literal(namespaceCacheSize));
		}
		if (namespaceIDCacheSize >= 0) {
			m.add(implNode, CONFIG.Native.namespaceIDCacheSize, literal(namespaceIDCacheSize));
		}
		// WAL configuration properties
		if (walMaxSegmentBytes >= 0) {
			m.add(implNode, CONFIG.Native.walMaxSegmentBytes, literal(walMaxSegmentBytes));
		}
		if (walQueueCapacity > 0) {
			m.add(implNode, CONFIG.Native.walQueueCapacity, literal(walQueueCapacity));
		}
		if (walBatchBufferBytes > 0) {
			m.add(implNode, CONFIG.Native.walBatchBufferBytes, literal(walBatchBufferBytes));
		}
		if (walSyncPolicy != null) {
			m.add(implNode, CONFIG.Native.walSyncPolicy, literal(walSyncPolicy));
		}
		if (walSyncIntervalMillis >= 0) {
			m.add(implNode, CONFIG.Native.walSyncIntervalMillis, literal(walSyncIntervalMillis));
		}
		if (walIdlePollIntervalMillis >= 0) {
			m.add(implNode, CONFIG.Native.walIdlePollIntervalMillis, literal(walIdlePollIntervalMillis));
		}
		if (walDirectoryName != null) {
			m.add(implNode, CONFIG.Native.walDirectoryName, literal(walDirectoryName));
		}
		// Only export when true to avoid noise
		if (walSyncBootstrapOnOpen) {
			m.add(implNode, CONFIG.Native.walSyncBootstrapOnOpen, literal(true));
		}
		if (walAutoRecoverOnOpen) {
			m.add(implNode, CONFIG.Native.walAutoRecoverOnOpen, literal(true));
		}
		if (!walEnabled) {
			m.add(implNode, CONFIG.Native.walEnabled, literal(false));
		}

		return implNode;
	}

	private Resource exportLegacy(Model m) {
		Resource implNode = super.export(m);
		m.setNamespace("ns", NativeStoreSchema.NAMESPACE);

		if (tripleIndexes != null) {
			m.add(implNode, TRIPLE_INDEXES, literal(tripleIndexes));
		}
		if (forceSync) {
			m.add(implNode, FORCE_SYNC, literal(forceSync));
		}
		if (valueCacheSize >= 0) {
			m.add(implNode, VALUE_CACHE_SIZE, literal(valueCacheSize));
		}
		if (valueIDCacheSize >= 0) {
			m.add(implNode, VALUE_ID_CACHE_SIZE, literal(valueIDCacheSize));
		}
		if (namespaceCacheSize >= 0) {
			m.add(implNode, NAMESPACE_CACHE_SIZE, literal(namespaceCacheSize));
		}
		if (namespaceIDCacheSize >= 0) {
			m.add(implNode, NAMESPACE_ID_CACHE_SIZE, literal(namespaceIDCacheSize));
		}
		// legacy export does not define a schema term; omit for legacy

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

			// WAL configuration properties
			Configurations.getLiteralValue(m, implNode, CONFIG.Native.walMaxSegmentBytes)
					.ifPresent(lit -> {
						try {
							setWalMaxSegmentBytes(lit.longValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException("Long value required for "
									+ CONFIG.Native.walMaxSegmentBytes + " property, found " + lit);
						}
					});

			Configurations.getLiteralValue(m, implNode, CONFIG.Native.walQueueCapacity)
					.ifPresent(lit -> {
						try {
							setWalQueueCapacity(lit.intValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException("Integer value required for "
									+ CONFIG.Native.walQueueCapacity + " property, found " + lit);
						}
					});

			Configurations.getLiteralValue(m, implNode, CONFIG.Native.walBatchBufferBytes)
					.ifPresent(lit -> {
						try {
							setWalBatchBufferBytes(lit.intValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException("Integer value required for "
									+ CONFIG.Native.walBatchBufferBytes + " property, found " + lit);
						}
					});

			Configurations.getLiteralValue(m, implNode, CONFIG.Native.walSyncPolicy)
					.ifPresent(lit -> setWalSyncPolicy(lit.getLabel()));

			Configurations.getLiteralValue(m, implNode, CONFIG.Native.walSyncIntervalMillis)
					.ifPresent(lit -> {
						try {
							setWalSyncIntervalMillis(lit.longValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException("Long value required for "
									+ CONFIG.Native.walSyncIntervalMillis + " property, found " + lit);
						}
					});

			Configurations.getLiteralValue(m, implNode, CONFIG.Native.walIdlePollIntervalMillis)
					.ifPresent(lit -> {
						try {
							setWalIdlePollIntervalMillis(lit.longValue());
						} catch (NumberFormatException e) {
							throw new SailConfigException("Long value required for "
									+ CONFIG.Native.walIdlePollIntervalMillis + " property, found " + lit);
						}
					});

			Configurations.getLiteralValue(m, implNode, CONFIG.Native.walDirectoryName)
					.ifPresent(lit -> setWalDirectoryName(lit.getLabel()));

			Configurations.getLiteralValue(m, implNode, CONFIG.Native.walSyncBootstrapOnOpen)
					.ifPresent(lit -> {
						try {
							setWalSyncBootstrapOnOpen(lit.booleanValue());
						} catch (IllegalArgumentException e) {
							throw new SailConfigException("Boolean value required for "
									+ CONFIG.Native.walSyncBootstrapOnOpen + " property, found " + lit);
						}
					});

			Configurations.getLiteralValue(m, implNode, CONFIG.Native.walAutoRecoverOnOpen)
					.ifPresent(lit -> {
						try {
							setWalAutoRecoverOnOpen(lit.booleanValue());
						} catch (IllegalArgumentException e) {
							throw new SailConfigException("Boolean value required for "
									+ CONFIG.Native.walAutoRecoverOnOpen + " property, found " + lit);
						}
					});

			Configurations.getLiteralValue(m, implNode, CONFIG.Native.walEnabled)
					.ifPresent(lit -> {
						try {
							setWalEnabled(lit.booleanValue());
						} catch (IllegalArgumentException e) {
							throw new SailConfigException(
									"Boolean value required for " + CONFIG.Native.walEnabled + " property, found "
											+ lit);
						}
					});
		} catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
