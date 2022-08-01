/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearch;

import java.util.concurrent.atomic.AtomicLong;

public class ElasticsearchTestUtils {

	/**
	 * Counter used to uniquely name test indexes without using UUID's that may be causing path length issues.
	 */
	private static final AtomicLong TEST_COUNTER = new AtomicLong(0);

	public static String getNextTestIndexName() {
		return "rdf4j-es-testindex-" + TEST_COUNTER.incrementAndGet();
	}
}
