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

import static org.eclipse.rdf4j.sail.lmdb.Varint.firstToLength;

import java.nio.ByteBuffer;

/**
 * A matcher for partial equality tests of varint lists.
 */
public class GroupMatcher {

	public static final Bytes.RegionComparator NULL_REGION_COMPARATOR = (a, b) -> true;
	private final int length0;
	private final int length1;
	private final int length2;
	private final int length3;
	private final Bytes.RegionComparator cmp0;
	private final Bytes.RegionComparator cmp1;
	private final Bytes.RegionComparator cmp2;
	private final Bytes.RegionComparator cmp3;
	private final byte firstByte0;
	private final byte firstByte1;
	private final byte firstByte2;
	private final byte firstByte3;
	private final MatchFn matcher;

	public GroupMatcher(byte[] keyArray, byte[] valueArray, boolean[] shouldMatch) {
		assert shouldMatch.length == 4;

		int baseOffset = 0;

		// Loop is unrolled for performance. Do not change back to a loop, do not extract into method, unless you
		// benchmark with QueryBenchmark first!
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
		baseOffset = 0;
		{
			byte fb = valueArray[baseOffset];
			this.firstByte2 = fb;
			int len = firstToLength(fb);
			this.length2 = len;
			if (shouldMatch[2]) {
				this.cmp2 = Bytes.capturedComparator(valueArray, baseOffset, len);
			} else {
				this.cmp2 = NULL_REGION_COMPARATOR;
			}

			baseOffset += len;
		}
		{
			byte fb = valueArray[baseOffset];
			this.firstByte3 = fb;
			int len = firstToLength(fb);
			this.length3 = len;

			if (shouldMatch[3]) {
				this.cmp3 = Bytes.capturedComparator(valueArray, baseOffset, len);
			} else {
				this.cmp3 = NULL_REGION_COMPARATOR;
			}
		}

		this.matcher = selectMatcher(shouldMatch);
	}

	public boolean matches(ByteBuffer key, ByteBuffer value) {
		return matcher.matches(key, value);
	}

	@FunctionalInterface
	private interface MatchFn {
		boolean matches(ByteBuffer key, ByteBuffer value);
	}

	private MatchFn selectMatcher(boolean[] shouldMatch) {
		byte mask = 0;
		if (shouldMatch[0]) {
			mask |= 0b0001;
		}
		if (shouldMatch[1]) {
			mask |= 0b0010;
		}
		if (shouldMatch[2]) {
			mask |= 0b0100;
		}
		if (shouldMatch[3]) {
			mask |= 0b1000;
		}

		switch (mask) {
		case 0b0000:
			return this::match0000;
		case 0b0001:
			return this::match0001;
		case 0b0010:
			return this::match0010;
		case 0b0011:
			return this::match0011;
		case 0b0100:
			return this::match0100;
		case 0b0101:
			return this::match0101;
		case 0b0110:
			return this::match0110;
		case 0b0111:
			return this::match0111;
		case 0b1000:
			return this::match1000;
		case 0b1001:
			return this::match1001;
		case 0b1010:
			return this::match1010;
		case 0b1011:
			return this::match1011;
		case 0b1100:
			return this::match1100;
		case 0b1101:
			return this::match1101;
		case 0b1110:
			return this::match1110;
		case 0b1111:
			return this::match1111;
		default:
			throw new IllegalStateException("Unsupported matcher mask: " + mask);
		}
	}

	private boolean match0000(ByteBuffer key, ByteBuffer value) {
		return true;
	}

	private boolean match0001(ByteBuffer key, ByteBuffer value) {
		byte otherFirst0 = key.get();
		if (firstByte0 == otherFirst0) {
			return length0 == 1 || cmp0.equals(otherFirst0, key);
		}
		return false;
	}

	private boolean match0010(ByteBuffer key, ByteBuffer value) {
		skipVarint(key);

		byte otherFirst1 = key.get();
		if (firstByte1 == otherFirst1) {
			return length1 == 1 || cmp1.equals(otherFirst1, key);
		}
		return false;
	}

	private boolean match0011(ByteBuffer key, ByteBuffer value) {
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

	private boolean match0100(ByteBuffer key, ByteBuffer value) {
		byte otherFirst2 = value.get();
		if (firstByte2 == otherFirst2) {
			return length2 == 1 || cmp2.equals(otherFirst2, value);
		}
		return false;
	}

	private boolean match0101(ByteBuffer key, ByteBuffer value) {
		byte otherFirst0 = key.get();
		if (firstByte0 == otherFirst0) {
			if (length0 == 1 || cmp0.equals(otherFirst0, key)) {
				byte otherFirst2 = value.get();
				if (firstByte2 == otherFirst2) {
					return length2 == 1 || cmp2.equals(otherFirst2, value);
				}
			}
		}
		return false;
	}

	private boolean match0110(ByteBuffer key, ByteBuffer value) {
		skipVarint(key);

		byte otherFirst1 = key.get();
		if (firstByte1 == otherFirst1) {
			if (length1 == 1 || cmp1.equals(otherFirst1, key)) {
				byte otherFirst2 = value.get();
				if (firstByte2 == otherFirst2) {
					return length2 == 1 || cmp2.equals(otherFirst2, value);
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

	private boolean match0111(ByteBuffer key, ByteBuffer value) {
		byte otherFirst0 = key.get();
		if (firstByte0 == otherFirst0) {
			if (length0 == 1 || cmp0.equals(otherFirst0, key)) {
				byte otherFirst1 = key.get();
				if (firstByte1 == otherFirst1) {
					if (length1 == 1 || cmp1.equals(otherFirst1, key)) {
						byte otherFirst2 = value.get();
						if (firstByte2 == otherFirst2) {
							return length2 == 1 || cmp2.equals(otherFirst2, value);
						}
					}
				}
			}
		}
		return false;
	}

	private boolean match1000(ByteBuffer key, ByteBuffer value) {
		skipVarint(value);

		byte otherFirst3 = value.get();
		if (firstByte3 == otherFirst3) {
			return length3 == 1 || cmp3.equals(otherFirst3, value);
		}
		return false;
	}

	private boolean match1001(ByteBuffer key, ByteBuffer value) {
		byte otherFirst0 = key.get();
		if (firstByte0 == otherFirst0) {
			if (length0 == 1 || cmp0.equals(otherFirst0, key)) {
				skipVarint(value);
				byte otherFirst3 = value.get();
				if (firstByte3 == otherFirst3) {
					return length3 == 1 || cmp3.equals(otherFirst3, value);
				}
			}
		}
		return false;
	}

	private boolean match1010(ByteBuffer key, ByteBuffer value) {
		skipVarint(key);
		byte otherFirst1 = key.get();
		if (firstByte1 == otherFirst1) {
			if (length1 == 1 || cmp1.equals(otherFirst1, key)) {
				skipVarint(value);

				byte otherFirst3 = value.get();
				if (firstByte3 == otherFirst3) {
					return length3 == 1 || cmp3.equals(otherFirst3, value);
				}
			}
		}
		return false;
	}

	private boolean match1011(ByteBuffer key, ByteBuffer value) {
		byte otherFirst0 = key.get();
		if (firstByte0 == otherFirst0) {
			if (length0 == 1 || cmp0.equals(otherFirst0, key)) {
				byte otherFirst1 = key.get();
				if (firstByte1 == otherFirst1) {
					if (length1 == 1 || cmp1.equals(otherFirst1, key)) {
						skipVarint(value);

						byte otherFirst3 = value.get();
						if (firstByte3 == otherFirst3) {
							return length3 == 1 || cmp3.equals(otherFirst3, value);
						}
					}
				}
			}
		}
		return false;
	}

	private boolean match1100(ByteBuffer key, ByteBuffer value) {
		byte otherFirst2 = value.get();
		if (firstByte2 == otherFirst2) {
			if (length2 == 1 || cmp2.equals(otherFirst2, value)) {
				byte otherFirst3 = value.get();
				if (firstByte3 == otherFirst3) {
					return length3 == 1 || cmp3.equals(otherFirst3, value);
				}
			}
		}
		return false;
	}

	private boolean match1101(ByteBuffer key, ByteBuffer value) {
		byte otherFirst0 = key.get();
		if (firstByte0 == otherFirst0) {
			if (length0 == 1 || cmp0.equals(otherFirst0, key)) {
				byte otherFirst2 = value.get();
				if (firstByte2 == otherFirst2) {
					if (length2 == 1 || cmp2.equals(otherFirst2, value)) {
						byte otherFirst3 = value.get();
						if (firstByte3 == otherFirst3) {
							return length3 == 1 || cmp3.equals(otherFirst3, value);
						}
					}
				}
			}
		}
		return false;
	}

	private boolean match1110(ByteBuffer key, ByteBuffer value) {
		skipVarint(key);

		byte otherFirst1 = key.get();
		if (firstByte1 == otherFirst1) {
			if (length1 == 1 || cmp1.equals(otherFirst1, key)) {
				byte otherFirst2 = value.get();
				if (firstByte2 == otherFirst2) {
					if (length2 == 1 || cmp2.equals(otherFirst2, value)) {
						byte otherFirst3 = value.get();
						if (firstByte3 == otherFirst3) {
							return length3 == 1 || cmp3.equals(otherFirst3, value);
						}
					}
				}
			}
		}
		return false;
	}

	private boolean match1111(ByteBuffer key, ByteBuffer value) {
		byte otherFirst0 = key.get();
		if (firstByte0 == otherFirst0) {
			if (length0 == 1 || cmp0.equals(otherFirst0, key)) {
				byte otherFirst1 = key.get();
				if (firstByte1 == otherFirst1) {
					if (length1 == 1 || cmp1.equals(otherFirst1, key)) {
						byte otherFirst2 = value.get();
						if (firstByte2 == otherFirst2) {
							if (length2 == 1 || cmp2.equals(otherFirst2, value)) {
								byte otherFirst3 = value.get();
								if (firstByte3 == otherFirst3) {
									return length3 == 1 || cmp3.equals(otherFirst3, value);
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

}
