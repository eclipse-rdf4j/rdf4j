/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

/**
 * {@link org.eclipse.rdf4j.repository.manager.RepositoryManager} subclasses
 * ({@link org.eclipse.rdf4j.opentelemetry.repository.manager.TracingLocalRepositoryManager}/
 * {@link org.eclipse.rdf4j.opentelemetry.repository.manager.TracingRemoteRepositoryManager}) that instrument every
 * repository they create with {@link org.eclipse.rdf4j.opentelemetry.repository}-level tracing, gated by
 * {@link org.eclipse.rdf4j.opentelemetry.repository.manager.OpenTelemetrySettings#isEnabled()}.
 * <p>
 * Because {@code RepositoryManager.getRepository(String)} caches its result by id and only ever calls
 * {@code createRepository(String)} once per id, wrapping there instruments every repository the manager serves exactly
 * once, regardless of which application (RDF4J Server, Workbench, a custom embedder, ...) is using the manager.
 */
package org.eclipse.rdf4j.opentelemetry.repository.manager;
