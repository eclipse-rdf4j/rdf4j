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
 * A quad entry with subject, predicate, object, context value IDs and a flag byte.
 */
public final class QuadEntry {
	public final long subject;
	public final long predicate;
	public final long object;
	public final long context;
	public final byte flag;

	public QuadEntry(long subject, long predicate, long object, long context, byte flag) {
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		this.context = context;
		this.flag = flag;
	}
}
