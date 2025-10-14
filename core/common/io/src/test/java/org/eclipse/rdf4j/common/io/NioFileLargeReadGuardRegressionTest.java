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
package org.eclipse.rdf4j.common.io;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression test for heap guard misclassification: NioFile.read(ByteBuffer,long) must not preemptively throw
 * IOException based solely on buffer size when the buffer is already allocated. Previously, a guard attempted to
 * compare the requested read size to free heap and could throw before performing any IO, breaking legitimate large
 * heap-buffer reads.
 */
public class NioFileLargeReadGuardRegressionTest {

	@TempDir
	File tmp;

	@Test
	public void largeHeapBufferReadDoesNotPreemptivelyThrow() throws Exception {
		// Prepare a small file; the size/content are irrelevant to the regression
		Path p = tmp.toPath().resolve("small.dat");
		byte[] small = new byte[1024];
		Files.write(p, small);

		// Allocate a heap buffer just above the previous guard threshold (128MB)
		final int size = 129 * 1024 * 1024; // 129MB
		final ByteBuffer buf;
		try {
			buf = ByteBuffer.allocate(size);
		} catch (OutOfMemoryError oom) {
			// Not enough heap available in this environment to run the check; skip rather than fail
			Assumptions.assumeTrue(false, "Insufficient heap to allocate 129MB test buffer");
			return; // unreachable, but keeps compiler happy
		}

		// Reduce observed free heap below the requested read size so that a pre-check (if present) would misclassify
		// this legitimate large buffer read as risky and throw. We allocate temporary blocks to lower free heap.
		// If we cannot safely get below the threshold, skip the test to avoid flakiness across environments.
		if (!saturateHeapToBelow(size)) {
			Assumptions.assumeTrue(false, "Could not reduce free heap below threshold deterministically");
			return;
		}

		try (NioFile nf = new NioFile(p.toFile())) {
			int n = nf.read(buf, 0);
			// Success is simply: no IOException thrown; value can be -1 (EOF) or >=0 bytes read
			assertTrue(n >= -1, "read() returned unexpected value");
		}
	}

	private static boolean saturateHeapToBelow(long bytes) {
		final Runtime rt = Runtime.getRuntime();
		final java.util.List<byte[]> blocks = new java.util.ArrayList<>();
		final int block = 8 * 1024 * 1024; // 8MB steps to avoid big spikes
		try {
			for (int i = 0; i < 512; i++) { // cap allocations defensively
				long alloc = rt.totalMemory() - rt.freeMemory();
				long free = rt.maxMemory() - alloc;
				if (free <= bytes) {
					return true;
				}
				int size = (int) Math.min(block, Math.max(1, free - bytes));
				blocks.add(new byte[size]);
			}
		} catch (OutOfMemoryError oom) {
			// Best-effort: after OOM, the VM may still have reduced free; treat as inconclusive
		}
		long alloc = rt.totalMemory() - rt.freeMemory();
		long free = rt.maxMemory() - alloc;
		return free <= bytes;
	}
}
