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
	private final int keyElements;
	private final int valueElements;

	public EntryMatcher(int split, byte[] keyArray, byte[] valueArray, boolean[] shouldMatch) {
		assert shouldMatch.length == 4;
		this.keyElements = split;
		this.valueElements = 4 - split;
		boolean[] keyShouldMatch;
		boolean[] valueShouldMatch;
		switch (split) {
		case 0:
			keyShouldMatch = new boolean[] {};
			valueShouldMatch = new boolean[] { shouldMatch[0], shouldMatch[1], shouldMatch[2], shouldMatch[3] };
			break;
		case 1:
			keyShouldMatch = new boolean[] { shouldMatch[0] };
			valueShouldMatch = new boolean[] { shouldMatch[1], shouldMatch[2], shouldMatch[3] };
			break;
		case 2:
			keyShouldMatch = new boolean[] { shouldMatch[0], shouldMatch[1] };
			valueShouldMatch = new boolean[] { shouldMatch[2], shouldMatch[3] };
			break;
		case 3:
			keyShouldMatch = new boolean[] { shouldMatch[0], shouldMatch[1], shouldMatch[2] };
			valueShouldMatch = new boolean[] { shouldMatch[3] };
			break;
		case 4:
			keyShouldMatch = new boolean[] { shouldMatch[0], shouldMatch[1], shouldMatch[2], shouldMatch[3] };
			valueShouldMatch = new boolean[] {};
			break;
		default:
			throw new IllegalArgumentException("Split must be between 0 and 4 inclusive");
		}
		this.keyMatcher = new VarintMatcher(keyArray, keyShouldMatch);
		this.valueMatcher = new VarintMatcher(valueArray, valueShouldMatch);
	}

	public boolean matches(VarintTupleInput key, VarintTupleInput value) {
		return keyMatcher.matches(key) && valueMatcher.matches(value);
	}

	public boolean matches(ByteBuffer key, ByteBuffer value) {
		return matches(new VarintTupleInput(keyElements, key), new VarintTupleInput(valueElements, value));
	}

	public boolean matchesKey(VarintTupleInput key) {
		return keyMatcher.matches(key);
	}

	public boolean matchesValue(VarintTupleInput value) {
		return valueMatcher.matches(value);
	}
}
