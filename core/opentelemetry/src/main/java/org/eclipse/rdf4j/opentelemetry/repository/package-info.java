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
 * OpenTelemetry tracing instrumentation at the {@link org.eclipse.rdf4j.repository.RepositoryConnection} level: every
 * SPARQL query/update evaluated through a wrapped connection is recorded as a span following the
 * <a href="https://opentelemetry.io/docs/specs/semconv/database/database-spans/">OpenTelemetry database client semantic
 * conventions</a> ({@code db.system.name}, {@code db.operation.name}, {@code db.namespace}, {@code db.query.text},
 * {@code db.response.returned_rows}, ...).
 * <p>
 * Unlike {@link org.eclipse.rdf4j.opentelemetry.http}, this instrumentation is backend-agnostic: it only depends on
 * {@link org.eclipse.rdf4j.repository.Repository}/{@link org.eclipse.rdf4j.repository.RepositoryConnection}, so it
 * works identically for a local SAIL-backed repository, a remote {@code HTTPRepository}/{@code SPARQLRepository}, or a
 * federation. This also makes it compose cleanly with repositories obtained from a
 * {@code org.eclipse.rdf4j.repository.manager.RepositoryManager}, which owns its own HTTP session-manager plumbing and
 * does not expose a seam for the {@code .http} package's session-level instrumentation.
 * <p>
 * See {@link org.eclipse.rdf4j.opentelemetry.OpenTelemetrySupport} for the one-line opt-in entry point.
 */
package org.eclipse.rdf4j.opentelemetry.repository;
