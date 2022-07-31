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
package org.eclipse.rdf4j.model;

/**
 * The supertype of all RDF resources (IRIs and blank nodes).
 */
public interface Resource extends Value {

	// Empty place holder as common supertype of IRI and BNode

	@Override
	default boolean isResource() {
		return true;
	}

}
