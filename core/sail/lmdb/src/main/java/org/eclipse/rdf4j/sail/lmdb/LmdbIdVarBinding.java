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

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;

/**
 * Provides variable bindings as LMDB internal IDs during ID-only join evaluation.
 */
@InternalUseOnly
public interface LmdbIdVarBinding {

	/**
	 * Return the internal ID for the given variable name or
	 * {@link org.eclipse.rdf4j.sail.lmdb.model.LmdbValue#UNKNOWN_ID} if the variable is unbound in the current left
	 * record.
	 */
	long getIdOrUnknown(String varName);
}
