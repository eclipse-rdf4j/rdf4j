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
package org.eclipse.rdf4j.sail.lucene;

import java.util.Collection;
import java.util.List;

public interface SearchDocument {

	String getId();

	String getResource();

	String getContext();

	/**
	 * Returns a set of the property names.
	 */
	Collection<String> getPropertyNames();

	/**
	 * Adds/creates a new property with the given name.
	 */
	void addProperty(String name);

	/**
	 * Adds a value to the property with the given name.
	 */
	void addProperty(String name, String value);

	void addGeoProperty(String name, String value);

	/**
	 * Checks whether a field occurs with a specified value in a Document.
	 */
	boolean hasProperty(String name, String value);

	List<String> getProperty(String name);
}
