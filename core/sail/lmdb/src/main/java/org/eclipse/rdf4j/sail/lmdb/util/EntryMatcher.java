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

package org.eclipse.rdf4j.sail.lmdb.util;

import java.nio.ByteBuffer;

/**
 * A matcher for entries consisting of a key and a value, each matched by their respective matcher.
 */
public class EntryMatcher {
	private final VarintMatcher keyMatcher;
	private final VarintMatcher valueMatcher;

	public EntryMatcher(byte[] keyArray, byte[] valueArray, boolean[] shouldMatch) {
		assert shouldMatch.length == 4;
		boolean[] keyShouldMatch = new boolean[] { shouldMatch[0], shouldMatch[1] };
		boolean[] valueShouldMatch = new boolean[] { shouldMatch[2], shouldMatch[3] };
		this.keyMatcher = new VarintMatcher(keyArray, keyShouldMatch);
		this.valueMatcher = new VarintMatcher(valueArray, valueShouldMatch);
	}

	public boolean matches(ByteBuffer key, ByteBuffer value) {
		return keyMatcher.matches(key) && valueMatcher.matches(value);
	}

	public boolean matchesKey(ByteBuffer key) {
		return keyMatcher.matches(key);
	}

	public boolean matchesValue(ByteBuffer value) {
		return valueMatcher.matches(value);
	}
}
