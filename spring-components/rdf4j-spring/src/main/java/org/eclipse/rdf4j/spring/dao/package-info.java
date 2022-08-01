/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

/**
 *
 *
 * <H1>Rdf4j-Spring DAO</H1>
 *
 * Support for custom DAO (data access object) implementations.
 *
 * <p>
 * Such custom DAO implementations get access to the following subsystems:
 *
 * <ul>
 * <li>{@link org.eclipse.rdf4j.spring.support.RDF4JTemplate Rdf4JTemplate}: Central service for accessing
 * repositories,executing queries and updates, as well as transforming results into java entities or collections
 * <li>{@link org.eclipse.rdf4j.spring.dao.support.sparql.NamedSparqlSupplier NamedSparqlSupplier}: DAO-specific map of
 * SPARQL Strings aiding efficient generation and caching of operaitons
 * </ul>
 *
 * <p>
 * There are two variants of DAOs:
 *
 * <ul>
 * <li>{@link org.eclipse.rdf4j.spring.dao.RDF4JDao Rdf4JDao}: Base class for DAOs with support for named operations and
 * access
 * <li>{@link org.eclipse.rdf4j.spring.dao.RDF4JCRUDDao Rdf4JCRUDDao}: Base class for DAOs that are associated with
 * specific entity classes, providing additional support for CRUD operations on these entities.
 * </ul>
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 *
 */
package org.eclipse.rdf4j.spring.dao;
