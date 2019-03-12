/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model.impl;

import org.eclipse.rdf4j.model.Namespace;

/**
 * A default implementation of the {@link Namespace} interface.
 */
public class SimpleNamespace implements Namespace {

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

	/**
	 * Returns a string representation of the object.
	 * 
	 * @return String representation of the namespace
	 */
	@Override
	public String toString() {
		return prefix + " :: " + name;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Namespace)) {
			return false;
		}
		Namespace other = (Namespace) obj;
		if (prefix == null) {
			if (other.getPrefix() != null) {
				return false;
			}
		} else if (!prefix.equals(other.getPrefix())) {
			return false;
		}
		if (name == null) {
			if (other.getName() != null) {
				return false;
			}
		} else if (!name.equals(other.getName())) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((prefix == null) ? 0 : prefix.hashCode());
		return result;
	}

	@Override
	public int compareTo(Namespace o) {
		if (getPrefix().equals(o.getPrefix())) {
			if (getName().equals(o.getName())) {
				return 0;
			} else {
				return getName().compareTo(o.getName());
			}
		} else {
			return getPrefix().compareTo(o.getPrefix());
		}
	}
}
