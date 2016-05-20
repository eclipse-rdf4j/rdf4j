/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf.config;

import static org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreSchema.FORCE_SYNC;
import static org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreSchema.NAMESPACE_CACHE_SIZE;
import static org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreSchema.NAMESPACE_ID_CACHE_SIZE;
import static org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreSchema.TRIPLE_INDEXES;
import static org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreSchema.VALUE_CACHE_SIZE;
import static org.eclipse.rdf4j.sail.nativerdf.config.NativeStoreSchema.VALUE_ID_CACHE_SIZE;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelException;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.sail.config.AbstractSailImplConfig;
import org.eclipse.rdf4j.sail.config.SailConfigException;

/**
 * @author Arjohn Kampman
 */
public class NativeStoreConfig extends AbstractSailImplConfig {

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
		ValueFactory vf = SimpleValueFactory.getInstance();

		if (tripleIndexes != null) {
			m.add(implNode, TRIPLE_INDEXES, vf.createLiteral(tripleIndexes));
		}
		if (forceSync) {
			m.add(implNode, FORCE_SYNC, vf.createLiteral(forceSync));
		}
		if (valueCacheSize >= 0) {
			m.add(implNode, VALUE_CACHE_SIZE, vf.createLiteral(valueCacheSize));
		}
		if (valueIDCacheSize >= 0) {
			m.add(implNode, VALUE_ID_CACHE_SIZE, vf.createLiteral(valueIDCacheSize));
		}
		if (namespaceCacheSize >= 0) {
			m.add(implNode, NAMESPACE_CACHE_SIZE, vf.createLiteral(namespaceCacheSize));
		}
		if (namespaceIDCacheSize >= 0) {
			m.add(implNode, NAMESPACE_ID_CACHE_SIZE, vf.createLiteral(namespaceIDCacheSize));
		}

		return implNode;
	}

	@Override
	public void parse(Model m, Resource implNode)
		throws SailConfigException
	{
		super.parse(m, implNode);

		try {

			Literal indexes = Models.objectLiteral(m.filter(implNode, TRIPLE_INDEXES, null));

			if (indexes != null) {
				setTripleIndexes(indexes.getLabel());
			}

			Literal lit = Models.objectLiteral(m.filter(implNode, FORCE_SYNC, null));

			if (lit != null) {
				try {
					setForceSync(lit.booleanValue());
				}
				catch (IllegalArgumentException e) {
					throw new SailConfigException(
							"Boolean value required for " + FORCE_SYNC + " property, found " + lit);
				}
			}

			lit = Models.objectLiteral(m.filter(implNode, VALUE_CACHE_SIZE, null));

			if (lit != null) {
				try {
					setValueCacheSize(lit.intValue());
				}
				catch (NumberFormatException e) {
					throw new SailConfigException(
							"Integer value required for " + VALUE_CACHE_SIZE + " property, found " + lit);
				}
			}

			lit = Models.objectLiteral(m.filter(implNode, VALUE_ID_CACHE_SIZE, null));

			if (lit != null) {
				try {
					setValueIDCacheSize(lit.intValue());
				}
				catch (NumberFormatException e) {
					throw new SailConfigException(
							"Integer value required for " + VALUE_ID_CACHE_SIZE + " property, found " + lit);
				}
			}

			lit = Models.objectLiteral(m.filter(implNode, NAMESPACE_CACHE_SIZE, null));

			if (lit != null) {
				try {
					setNamespaceCacheSize(lit.intValue());
				}
				catch (NumberFormatException e) {
					throw new SailConfigException(
							"Integer value required for " + NAMESPACE_CACHE_SIZE + " property, found " + lit);
				}
			}

			lit = Models.objectLiteral(m.filter(implNode, NAMESPACE_ID_CACHE_SIZE, null));

			if (lit != null) {
				try {
					setNamespaceIDCacheSize(lit.intValue());
				}
				catch (NumberFormatException e) {
					throw new SailConfigException("Integer value required for " + NAMESPACE_ID_CACHE_SIZE
							+ " property, found " + lit);
				}
			}
		}
		catch (

		ModelException e) {
			throw new SailConfigException(e.getMessage(), e);
		}
	}
}
