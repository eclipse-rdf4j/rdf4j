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
 * <H1>Rdf4j-Spring OperationCache</H1>
 *
 * Provides connection-level caching of SPARQL operations.
 *
 * <p>
 * To enable, set: <code>rdf4j.spring.operationcache.enabled=true</code>.
 *
 * <p>
 * If enabled, the {@link org.eclipse.rdf4j.spring.support.RDF4JTemplate Rdf4JTemplate}, set up by
 * {@link org.eclipse.rdf4j.spring.RDF4JConfig}, will use the
 * {@link org.eclipse.rdf4j.spring.operationcache.CachingOperationInstantiator CachingOperationInstantiator} to generate
 * new SPARQL operations instead of the default implementation.
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
package org.eclipse.rdf4j.spring.operationcache;
