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
package org.eclipse.rdf4j.http.client.spi;

/**
 * A supplier of values that may throw a checked exception.
 *
 * <p>
 * Unlike {@link java.util.function.Supplier}, this interface allows implementations to throw checked exceptions, making
 * it suitable for token sources that perform I/O, call external services, or otherwise fail in checked ways (e.g.
 * fetching an OAuth access token or reading a secret from a vault).
 *
 * @param <T> the type of value produced
 */
@FunctionalInterface
public interface Producer<T> {

	/**
	 * Produces a value.
	 *
	 * @return the produced value
	 * @throws Exception if production fails
	 */
	T produce() throws Exception;
}
