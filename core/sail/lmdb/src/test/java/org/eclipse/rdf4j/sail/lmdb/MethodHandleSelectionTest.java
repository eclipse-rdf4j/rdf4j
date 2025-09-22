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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;

import org.eclipse.rdf4j.sail.lmdb.Varint.GroupMatcher;
import org.eclipse.rdf4j.sail.lmdb.util.Bytes;
import org.junit.jupiter.api.Test;

class MethodHandleSelectionTest {

	@Test
	void comparatorHandlePointsToSpecializedMethods() throws Throwable {
		MethodHandle handle = Bytes.comparatorHandle(true, true, 4);
		MethodHandleInfo info = MethodHandles.privateLookupIn(Bytes.class, MethodHandles.lookup()).revealDirect(handle);
		assertEquals("cmp_AH_BH_LEN4", info.getName());
		assertSame(handle, Bytes.comparatorHandle(true, true, 4));
	}

	@Test
	void comparatorHandleCachesLoopVariants() {
		MethodHandle handle = Bytes.comparatorHandle(true, true, 12);
		assertSame(handle, Bytes.comparatorHandle(true, true, 12));
	}

	@Test
	void groupMatcherUsesMethodHandlesForComparisons() {
		ByteBuffer encoded = encodedValues();
		GroupMatcher matcher = new GroupMatcher(encoded.duplicate(), new boolean[] { true, true, true, true });

		assertTrue(matcher.matches(encoded.duplicate()));
		assertTrue(matcher.matches(toDirect(encoded)));
	}

	private static ByteBuffer encodedValues() {
		ByteBuffer buffer = ByteBuffer.allocate(10);
		buffer.put((byte) 1);
		buffer.put((byte) 241).put((byte) 0);
		buffer.put((byte) 249).put((byte) 0).put((byte) 0);
		buffer.put((byte) 250).put((byte) 0).put((byte) 0).put((byte) 0);
		buffer.flip();
		return buffer;
	}

	private static ByteBuffer toDirect(ByteBuffer source) {
		ByteBuffer direct = ByteBuffer.allocateDirect(source.remaining());
		direct.put(source.duplicate());
		direct.flip();
		return direct;
	}
}
