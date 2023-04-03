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

import static org.eclipse.rdf4j.model.util.Values.iri;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.CONFIG;
import org.eclipse.rdf4j.repository.sail.SailRepository;

/**
 * Defines constants for the SailRepository schema which is used by {@link SailRepositoryFactory}s to initialize
 * {@link SailRepository}s.
 *
 * @author Arjohn Kampman
 * @deprecated use {@link CONFIG} instead.
 */
@Deprecated(since = "4.3.0", forRemoval = true)
public class SailRepositorySchema {

	/**
	 * The SailRepository schema namespace (<var>http://www.openrdf.org/config/repository/sail#</var>).
	 */
	public static final String NAMESPACE = "http://www.openrdf.org/config/repository/sail#";

	/**
	 * @deprecated use {@link CONFIG#impl} instead.
	 */
	public final static IRI SAILIMPL = iri(NAMESPACE, "sailImpl");
}
