/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
/**
 * The Repository API: the main API for accessing rdf databases and SPARQL endpoints.
 *
 * The class {@link org.eclipse.rdf4j.repository.Repository} is the main interface for rdf4j repositories. It provides
 * all sorts of operations for manipulating RDF in various ways, through a
 * {@link org.eclipse.rdf4j.repository.RepositoryConnection}.
 *
 * An important notion in a rdf4j repository is that of <strong>context</strong> . Within one repository, subsets of
 * statements can be identified by their context.
 *
 * @see <a href="https://rdf4j.org/documentation/programming/repository/">rdf4j repository API documentation</a>
 */
package org.eclipse.rdf4j.repository;
