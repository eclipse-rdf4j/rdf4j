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
package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.base.AbstractNamespace;

/**
 * A default implementation of the {@link Namespace} interface.
 */
public class SimpleNamespace extends AbstractNamespace {

	private static final long serialVersionUID = -5829768428912588171L;

	/*-----------*
	 * Variables *
	 *-----------*/

	/**
	 * The namespace's prefix.
	 */
	private String prefix;

	/**
	 * The namespace's name.
	 */
	private String name;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new Namespace object.
	 *
	 * @param prefix The namespace's prefix.
	 * @param name   The namespace's name.
	 */
	public SimpleNamespace(String prefix, String name) {
		setPrefix(prefix);
		setName(name);
	}

	/*---------*
	 * Methods *
	 *---------*/

	/**
	 * Gets the prefix of the namespace.
	 *
	 * @return prefix of the namespace
	 */
	@Override
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Sets the prefix of the namespace.
	 *
	 * @param prefix The (new) prefix for this namespace.
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Gets the name of the namespace.
	 *
	 * @return name of the namespace
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the namespace.
	 *
	 * @param name The (new) name for this namespace.
	 */
	public void setName(String name) {
		this.name = name;
	}

}
