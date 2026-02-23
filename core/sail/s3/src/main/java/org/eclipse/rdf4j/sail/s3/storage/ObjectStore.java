/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.s3.storage;

import java.io.Closeable;
import java.util.List;

/**
 * Abstraction over object storage (S3-compatible or filesystem).
 */
public interface ObjectStore extends Closeable {

	void put(String key, byte[] data);

	byte[] get(String key);

	byte[] getRange(String key, long offset, long length);

	void delete(String key);

	List<String> list(String subPrefix);
}
