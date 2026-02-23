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

/**
 * A source of raw key/flag entries for the {@link MergeIterator}. Both {@link MemTable} and {@link SSTable} expose this
 * interface over a key range.
 */
public interface RawEntrySource {

	boolean hasNext();

	byte[] peekKey();

	byte peekFlag();

	void advance();
}
