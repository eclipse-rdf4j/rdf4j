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
package org.eclipse.rdf4j.sail.lmdb;

import java.util.Set;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;

/**
 * Read-only accessor for variable ID lookups against a record long[] produced by a RecordIterator.
 */
@InternalUseOnly
public interface IdAccessor {
	long getId(long[] record, String varName);

	Set<String> getVariableNames();

	/**
	 * Returns the index inside the {@code long[]} record where the given variable's ID is stored. Implementations
	 * should return {@code -1} when the variable is not part of the record.
	 */
	int getRecordIndex(String varName);
}
