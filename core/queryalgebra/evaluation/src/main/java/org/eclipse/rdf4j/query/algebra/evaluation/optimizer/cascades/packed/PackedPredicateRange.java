/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer.cascades.packed;

import java.util.Arrays;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.base.CoreDatatype;

/**
 * Reusable predicate-object range result slot filled by a {@link PackedPredicateRangeProvider}. The slot owns no
 * collection; finite values are exposed through indexed access and the codec resets and reuses one instance for every
 * lookup.
 */
public final class PackedPredicateRange {

	/** No proof is available for the predicate. */
	public static final int STATE_UNKNOWN = 0;
	/** The store proves the predicate has no restriction worth encoding. */
	public static final int STATE_UNRESTRICTED = 1;
	/** The store proves a restriction described by the bit sets below. */
	public static final int STATE_KNOWN = 2;
	/** The store proves the predicate currently has no statements at all. */
	public static final int STATE_EMPTY = 3;

	public static final int KIND_IRI = 1;
	public static final int KIND_LITERAL = 1 << 1;
	public static final int KIND_BNODE = 1 << 2;

	public static final int LANGUAGE_WITH = 1;
	public static final int LANGUAGE_WITHOUT = 1 << 1;

	public static final int UNIVERSAL_NUMBER = 1;
	public static final int UNIVERSAL_CANONICAL_INTEGER = 1 << 1;
	public static final int UNIVERSAL_CANONICAL_DATETIME = 1 << 2;
	public static final int UNIVERSAL_CANONICAL_DATE = 1 << 3;

	static {
		if (CoreDatatype.XSD.values().length > 64) {
			throw new IllegalStateException("CoreDatatype.XSD no longer fits a 64-bit datatype mask");
		}
	}

	private int state;
	private int kindBits;
	private int languageBits;
	private long datatypeBits;
	private int universalBits;
	private boolean hasIntegerBounds;
	private long integerMinInclusive;
	private long integerMaxInclusive;
	private Value[] finiteValues = new Value[8];
	private int finiteValueCount;
	private boolean finite;
	private String description;

	public void reset() {
		state = STATE_UNKNOWN;
		kindBits = 0;
		languageBits = 0;
		datatypeBits = 0L;
		universalBits = 0;
		hasIntegerBounds = false;
		integerMinInclusive = 0L;
		integerMaxInclusive = 0L;
		Arrays.fill(finiteValues, 0, finiteValueCount, null);
		finiteValueCount = 0;
		finite = false;
		description = null;
	}

	public int state() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	/** True when the slot carries a usable proof ({@link #STATE_KNOWN} or {@link #STATE_EMPTY}). */
	public boolean isProof() {
		return state == STATE_KNOWN || state == STATE_EMPTY;
	}

	public int kindBits() {
		return kindBits;
	}

	public void setKindBits(int kindBits) {
		this.kindBits = kindBits;
	}

	public int languageBits() {
		return languageBits;
	}

	public void setLanguageBits(int languageBits) {
		this.languageBits = languageBits;
	}

	public long datatypeBits() {
		return datatypeBits;
	}

	public void addDatatype(CoreDatatype.XSD datatype) {
		datatypeBits |= 1L << datatype.ordinal();
	}

	public boolean hasDatatype(CoreDatatype.XSD datatype) {
		return (datatypeBits & (1L << datatype.ordinal())) != 0L;
	}

	public int universalBits() {
		return universalBits;
	}

	public void setUniversalBits(int universalBits) {
		this.universalBits = universalBits;
	}

	public boolean hasIntegerBounds() {
		return hasIntegerBounds;
	}

	public long integerMinInclusive() {
		return integerMinInclusive;
	}

	public long integerMaxInclusive() {
		return integerMaxInclusive;
	}

	public void setIntegerBounds(long minInclusive, long maxInclusive) {
		hasIntegerBounds = true;
		integerMinInclusive = minInclusive;
		integerMaxInclusive = maxInclusive;
	}

	/** True when {@link #finiteValueCount()} enumerates the complete current object domain. */
	public boolean isFinite() {
		return finite;
	}

	public void setFinite(boolean finite) {
		this.finite = finite;
	}

	public int finiteValueCount() {
		return finiteValueCount;
	}

	public Value finiteValue(int ordinal) {
		if (ordinal < 0 || ordinal >= finiteValueCount) {
			throw new IndexOutOfBoundsException("finite value " + ordinal + " of " + finiteValueCount);
		}
		return finiteValues[ordinal];
	}

	public void addFiniteValue(Value value) {
		if (finiteValueCount == finiteValues.length) {
			finiteValues = Arrays.copyOf(finiteValues, finiteValues.length << 1);
		}
		finiteValues[finiteValueCount++] = value;
	}

	/**
	 * Optional provider-rendered description of the underlying store guarantee, used verbatim for
	 * {@code optimizer.objectGuarantee} diagnostics.
	 */
	public String description() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
