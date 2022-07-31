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
package org.eclipse.rdf4j.repository.sail.config;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/**
 * Defines constants for the HTTPRepository schema which is used by {@link ProxyRepositoryFactory}s to initialize
 * {@link org.eclipse.rdf4j.repository.sail.ProxyRepository}s.
 *
 * @author Dale Visser
 */
public class ProxyRepositorySchema {

	/**
	 * The {@link org.eclipse.rdf4j.repository.sail.ProxyRepository} schema namespace (
	 * <var>http://www.openrdf.org/config/repository/proxy#</var>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/config/repository/proxy#";

	/** <var>http://www.openrdf.org/config/repository/proxy#proxiedID</var> */
	public final static IRI PROXIED_ID;

	static {
		ValueFactory factory = SimpleValueFactory.getInstance();
		PROXIED_ID = factory.createIRI(NAMESPACE, "proxiedID");
	}
}
