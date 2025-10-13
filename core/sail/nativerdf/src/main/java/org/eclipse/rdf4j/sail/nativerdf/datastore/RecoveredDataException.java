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
package org.eclipse.rdf4j.sail.nativerdf.datastore;

import java.io.IOException;

/**
 * Signals that data for a given id was recovered heuristically (e.g., by inferring the length from neighboring
 * offsets). Carries the recovered bytes to enable callers to construct a CorruptValue.
 */
public class RecoveredDataException extends IOException {

	private static final long serialVersionUID = 1L;

	private final int id;
	private final byte[] data;

	public RecoveredDataException(int id, byte[] data) {
		super("Recovered data for id " + id + " using neighboring offsets");
		this.id = id;
		this.data = data;
	}

	public int getId() {
		return id;
	}

	public byte[] getData() {
		return data;
	}
}
