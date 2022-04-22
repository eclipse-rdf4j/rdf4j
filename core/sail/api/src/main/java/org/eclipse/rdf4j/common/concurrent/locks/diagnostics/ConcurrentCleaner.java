/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.common.concurrent.locks.diagnostics;

import java.lang.ref.Cleaner;

import org.eclipse.rdf4j.common.annotation.InternalUseOnly;

/**
 * Optimized for multithreaded use of the Java 9+ Cleaner
 *
 * @author Håvard M. Ottestad
 */
@InternalUseOnly
public class ConcurrentCleaner {

	// Each Cleaner instance starts its own Thread, so we use a conservative maximum so as to not create too many
	// threads.
	private static final int MAX = 128;

	private final static Cleaner[] cleaner;

	private static final int mask;

	static {
		int concurrency = powerOfTwoSize(Runtime.getRuntime().availableProcessors() * 2);
		mask = concurrency - 1;
		cleaner = new Cleaner[concurrency];
	}

	private static int powerOfTwoSize(int initialSize) {
		int n = -1 >>> Integer.numberOfLeadingZeros(initialSize - 1);
		return (n < 0) ? 1 : (n >= MAX) ? MAX : n + 1;
	}

	static int getIndex(Thread key) {
		if (key == null) {
			return 0;
		}
		return mask & ((int) key.getId());
	}

	public Cleaner.Cleanable register(Object obj, Runnable action) {
		Cleaner cleaner = ConcurrentCleaner.cleaner[getIndex(Thread.currentThread())];
		if (cleaner == null) {
			cleaner = instantiateCleaner(getIndex(Thread.currentThread()));
		}
		return cleaner.register(obj, action);
	}

	private synchronized static Cleaner instantiateCleaner(int index) {
		if (ConcurrentCleaner.cleaner[index] == null) {
			ConcurrentCleaner.cleaner[index] = Cleaner.create();
		}
		return ConcurrentCleaner.cleaner[index];
	}

}
