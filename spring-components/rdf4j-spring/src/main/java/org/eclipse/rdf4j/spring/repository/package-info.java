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
 * <H1>Rdf4j-Spring Repository</H1>
 *
 * Automatically configures {@link org.eclipse.rdf4j.repository.Repository Repository} beans via
 * {@link org.eclipse.rdf4j.spring.RDF4JConfig Rdf4JConfig}.
 *
 * <p>
 * To configure a remote repostitory, use
 *
 * <ul>
 * <li><code>rdf4j.spring.repository.remote.manager-url=[manager-url]</code>
 * <li><code>rdf4j.spring.repository.remote.name=[name]</code>
 * </ul>
 *
 * (see {@link org.eclipse.rdf4j.spring.repository.remote.RemoteRepositoryProperties RemoteRepositoryProperties})
 *
 * <p>
 * To configure an in-memory Repository use <code>rdf4j.spring.repository.inmemory.enabled=true
 * </code> (see {@link org.eclipse.rdf4j.spring.repository.inmemory.InMemoryRepositoryProperties
 * InMemoryRepositoryProperties})
 *
 * <p>
 * <b>Note: Exactly one repository has to be configured.</b>
 *
 * @since 4.0.0
 * @author Gabriel Pickl
 * @author Florian Kleedorfer
 */
package org.eclipse.rdf4j.spring.repository;
