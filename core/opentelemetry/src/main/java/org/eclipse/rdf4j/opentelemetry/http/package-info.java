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
 * OpenTelemetry tracing instrumentation for outbound SPARQL Protocol HTTP requests, built as a specialization of
 * {@link org.eclipse.rdf4j.http.client.SPARQLProtocolSession}/{@link org.eclipse.rdf4j.http.client.RDF4JProtocolSession}
 * created through a dedicated {@link org.eclipse.rdf4j.http.client.HttpClientSessionManager}. See
 * {@link org.eclipse.rdf4j.opentelemetry.OpenTelemetrySupport} for the one-line opt-in entry point.
 */
package org.eclipse.rdf4j.opentelemetry.http;
