/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;

import org.eclipse.rdf4j.sail.lmdb.Varint.GroupMatcher;
import org.junit.jupiter.api.Test;

class GroupMatcherTest {

	@Test
	void matchesWhenAllSegmentsEqual() {
		ByteBuffer reference = allocateVarints(10, 256, 67823, 123456789L);
		GroupMatcher matcher = new GroupMatcher(reference.duplicate(), new boolean[] { true, true, true, true });

		ByteBuffer candidate = allocateVarints(10, 256, 67823, 123456789L);
		assertTrue(matcher.matches(candidate));
	}

	@Test
	void mismatchWhenSegmentDiffers() {
		ByteBuffer reference = allocateVarints(10, 256, 67823, 123456789L);
		GroupMatcher matcher = new GroupMatcher(reference.duplicate(), new boolean[] { true, true, true, true });

		ByteBuffer candidate = allocateVarints(10, 256, 67823, 123456790L);
		assertFalse(matcher.matches(candidate));
	}

	@Test
	void skipsUnmatchedSegments() {
		ByteBuffer reference = allocateVarints(10, 256, 67823, 123456789L);
		GroupMatcher matcher = new GroupMatcher(reference.duplicate(), new boolean[] { true, false, false, false });

		ByteBuffer candidate = allocateVarints(10, 0, 1, 2);
		assertTrue(matcher.matches(candidate));
	}

	private static ByteBuffer allocateVarints(long a, long b, long c, long d) {
		ByteBuffer buffer = ByteBuffer.allocate(4 * Long.BYTES + 4);
		Varint.writeUnsigned(buffer, a);
		Varint.writeUnsigned(buffer, b);
		Varint.writeUnsigned(buffer, c);
		Varint.writeUnsigned(buffer, d);
		buffer.flip();
		return buffer;
	}
}
