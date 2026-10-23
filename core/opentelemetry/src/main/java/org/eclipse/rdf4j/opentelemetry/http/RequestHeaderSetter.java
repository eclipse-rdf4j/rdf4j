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
package org.eclipse.rdf4j.opentelemetry.http;

import org.eclipse.rdf4j.http.client.spi.HttpRequest;

import io.opentelemetry.context.propagation.TextMapSetter;

/**
 * Injects W3C trace-context headers into an outbound {@link HttpRequest} by delegating to its mutable
 * {@link HttpRequest#setHeader(String, String)}.
 */
enum RequestHeaderSetter implements TextMapSetter<HttpRequest> {

	INSTANCE;

	@Override
	public void set(HttpRequest carrier, String key, String value) {
		if (carrier != null) {
			carrier.setHeader(key, value);
		}
	}
}
