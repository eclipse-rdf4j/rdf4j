/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.config;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.sail.base.config.BaseSailConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

/**
 * @author Arjohn Kampman
 */
public class LmdbStoreConfig extends BaseSailConfig {

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
		ValueFactory vf = SimpleValueFactory.getInstance();

		m.setNamespace("ns", LmdbStoreSchema.NAMESPACE);
		if (tripleIndexes != null) {
			m.add(implNode, LmdbStoreSchema.TRIPLE_INDEXES, vf.createLiteral(tripleIndexes));
		}
		if (forceSync) {
			m.add(implNode, LmdbStoreSchema.FORCE_SYNC, vf.createLiteral(forceSync));
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

		return implNode;
	}

	@Override
	public void parse(Model m, Resource implNode) throws SailConfigException {
		super.parse(m, implNode);

		try {
			Models.objectLiteral(m.getStatements(implNode, LmdbStoreSchema.TRIPLE_INDEXES, null))
					.ifPresent(lit -> setTripleIndexes(lit.getLabel()));
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
		} catch (ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
