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

package org.eclipse.rdf4j.spring.uuidsource.predictable;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.spring.support.UUIDSource;

/**
 * UUID source that generates the same sequence of UUIDs by counting up a <code>long</code> counter and using that as
 * the value for generating a UUID. Useful for unit tests as newly generated entities will receive the same UUIDs each
 * time the tests are executed.
 *
 * @since 4.0.0
 * @author Florian Kleedorfer
 */
public class PredictableUUIDSource implements UUIDSource {

	private final AtomicLong counter = new AtomicLong(0);

	public PredictableUUIDSource() {
	}

	@Override
	public IRI nextUUID() {
		long value = counter.incrementAndGet();
		return toURNUUID(UUID.nameUUIDFromBytes(Long.toString(value).getBytes()).toString());
	}

}
