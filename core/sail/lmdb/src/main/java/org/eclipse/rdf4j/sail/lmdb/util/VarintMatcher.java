package org.eclipse.rdf4j.sail.lmdb.util;

import static org.eclipse.rdf4j.sail.lmdb.Varint.firstToLength;

import java.nio.ByteBuffer;

/**
 * Matcher for partial equality tests of varint lists in keys.
 */
public class VarintMatcher {
	public static final Bytes.RegionComparator NULL_REGION_COMPARATOR = (a, b) -> true;
	private final int length0;
	private final int length1;
	private final Bytes.RegionComparator cmp0;
	private final Bytes.RegionComparator cmp1;
	private final byte firstByte0;
	private final byte firstByte1;
	private final MatchFn matcher;

	public VarintMatcher(byte[] keyArray, boolean[] shouldMatch) {
		assert shouldMatch.length == 2;
		int baseOffset = 0;
		{
			byte fb = keyArray[0];
			this.firstByte0 = fb;
			int len = firstToLength(fb);
			this.length0 = len;
			if (shouldMatch[0]) {
				this.cmp0 = Bytes.capturedComparator(keyArray, 0, len);
			} else {
				this.cmp0 = NULL_REGION_COMPARATOR;
			}
			baseOffset += len;
		}
		{
			byte fb = keyArray[baseOffset];
			this.firstByte1 = fb;
			int len = firstToLength(fb);
			this.length1 = len;
			if (shouldMatch[1]) {
				this.cmp1 = Bytes.capturedComparator(keyArray, baseOffset, len);
			} else {
				this.cmp1 = NULL_REGION_COMPARATOR;
			}
		}
		this.matcher = selectMatcher(shouldMatch);
	}

	public boolean matches(ByteBuffer key) {
		return matcher.matches(key);
	}

	@FunctionalInterface
	private interface MatchFn {
		boolean matches(ByteBuffer key);
	}

	private MatchFn selectMatcher(boolean[] shouldMatch) {
		byte mask = 0;
		if (shouldMatch[0])
			mask |= 0b01;
		if (shouldMatch[1])
			mask |= 0b10;

		switch (mask) {
		case 0b00:
			return this::match00;
		case 0b01:
			return this::match01;
		case 0b10:
			return this::match10;
		case 0b11:
			return this::match11;
		default:
			throw new IllegalStateException("Unsupported matcher mask: " + mask);
		}
	}

	private boolean match00(ByteBuffer key) {
		return true;
	}

	private boolean match01(ByteBuffer key) {
		byte otherFirst0 = key.get();
		if (firstByte0 == otherFirst0) {
			return length0 == 1 || cmp0.equals(otherFirst0, key);
		}
		return false;
	}

	private boolean match10(ByteBuffer key) {
		skipVarint(key);
		byte otherFirst1 = key.get();
		if (firstByte1 == otherFirst1) {
			return length1 == 1 || cmp1.equals(otherFirst1, key);
		}
		return false;
	}

	private boolean match11(ByteBuffer key) {
		byte otherFirst0 = key.get();
		if (firstByte0 == otherFirst0) {
			if (length0 == 1 || cmp0.equals(otherFirst0, key)) {
				byte otherFirst1 = key.get();
				if (firstByte1 == otherFirst1) {
					return length1 == 1 || cmp1.equals(otherFirst1, key);
				}
			}
		}
		return false;
	}

	private void skipVarint(ByteBuffer bb) {
		int i = firstToLength(bb.get()) - 1;
		assert i >= 0;
		if (i > 0) {
			bb.position(i + bb.position());
		}
	}
}
