/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.model;

import java.io.Serializable;

/**
 * A namespace, consisting of a namespace name and a prefix that has been assigned to it.
 */
public interface Namespace extends Serializable, Comparable<Namespace> {

	/**
	 * Gets the name of the current namespace (i.e. its IRI).
	 * 
	 * @return name of namespace
	 */
	public String getName();

	/**
	 * Gets the prefix of the current namespace. The default namespace is represented by an empty prefix string.
	 * 
	 * @return prefix of namespace, or an empty string in case of the default namespace.
	 */
	public String getPrefix();
}
