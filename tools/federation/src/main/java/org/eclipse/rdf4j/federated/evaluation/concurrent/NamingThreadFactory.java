/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.federated.evaluation.concurrent;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for named threads
 */
public class NamingThreadFactory implements ThreadFactory {
	private final AtomicInteger nextThreadId = new AtomicInteger();

	private final String baseName;

	public NamingThreadFactory(String baseName) {
		super();
		this.baseName = baseName;
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = Executors.defaultThreadFactory().newThread(r);
		t.setName(baseName + "-" + nextThreadId.incrementAndGet());
		return t;
	}

}
