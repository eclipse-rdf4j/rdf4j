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
import java.nio.ByteOrder;

import org.eclipse.rdf4j.sail.lmdb.Varint;

/**
 * A matcher for partial equality tests of varint lists.
 */
public class GroupMatcher {

	public static final byte[] BYTES_FOR_ZERO = { 0 };
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

	public GroupMatcher(long value0, long value1, long value2, long value3, boolean[] shouldMatch) {
		assert shouldMatch.length == 4;

		// Loop is unrolled for performance. Do not change back to a loop, do not extract into method, unless you
		// benchmark with QueryBenchmark first!
		{
			if (value0 == 0) {
				this.length0 = 1;
				this.firstByte0 = BYTES_FOR_ZERO[0];
				this.cmp0 = null;
			} else {
				byte[] valueArray = getByteArray(value0);

				this.length0 = valueArray.length;
				this.firstByte0 = valueArray[0];
				this.cmp0 = Bytes.capturedComparator(valueArray, 0, length0);
			}
		}
		{
			if (value1 == 0) {
				byte[] valueArray = BYTES_FOR_ZERO;
				this.length1 = valueArray.length;
				this.firstByte1 = valueArray[0];
				this.cmp1 = null;
			} else {
				byte[] valueArray = getByteArray(value1);

				this.length1 = valueArray.length;
				this.firstByte1 = valueArray[0];
				this.cmp1 = Bytes.capturedComparator(valueArray, 0, length1);
			}
		}
		{
			if (value2 == 0) {
				byte[] valueArray = BYTES_FOR_ZERO;

				this.length2 = valueArray.length;
				this.firstByte2 = valueArray[0];
				this.cmp2 = null;
			} else {
				byte[] valueArray = getByteArray(value2);

				this.length2 = valueArray.length;
				this.firstByte2 = valueArray[0];
				this.cmp2 = Bytes.capturedComparator(valueArray, 0, length2);
			}
		}
		{
			if (value3 == 0) {
				byte[] valueArray = BYTES_FOR_ZERO;

				this.length3 = valueArray.length;
				this.firstByte3 = valueArray[0];
				this.cmp3 = null;
			} else {
				byte[] valueArray = getByteArray(value3);

				this.length3 = valueArray.length;
				this.firstByte3 = valueArray[0];
				this.cmp3 = Bytes.capturedComparator(valueArray, 0, length3);
			}
		}

		this.matcher = selectMatcher(shouldMatch);

	}

	private static byte[] getByteArray(long value0) {

		if (value0 <= 240) {
			return new byte[] { (byte) value0 };
		} else if (value0 <= 2287) {

			// header: 241..248, then 1 payload byte
			// Using bit ops instead of div/mod and putShort to batch the two bytes.
			long v = value0 - 240; // 1..2047

			int hi = (int) (v >>> 8) + 241; // 241..248
			int lo = (int) (v & 0xFF); // 0..255
			return new byte[] { (byte) hi, (byte) lo };

		} else if (value0 <= 67823) {
			var buffer = ByteBuffer.allocate(3);

			// header 249, then 2 payload bytes (value - 2288), big-endian
			long v = value0 - 2288; // 0..65535

			buffer.put((byte) 249);
			final ByteOrder prev = buffer.order();
			if (prev != ByteOrder.BIG_ENDIAN) {
				buffer.order(ByteOrder.BIG_ENDIAN);
			}
			try {
				buffer.putShort((short) v);
			} finally {
				if (prev != ByteOrder.BIG_ENDIAN) {
					buffer.order(prev);
				}
			}
			return buffer.array();
		} else {
			int bytes = Varint.descriptor(value0) + 1; // 3..8

			var buffer = ByteBuffer.allocate(bytes + 1);

			buffer.put((byte) (250 + (bytes - 3))); // header 250..255
			// payload (batched)
			final ByteOrder prev = buffer.order();
			if (prev != ByteOrder.BIG_ENDIAN) {
				buffer.order(ByteOrder.BIG_ENDIAN);
			}
			try {
				int i = bytes;

				// If odd number of bytes, write the leading MSB first
				if ((i & 1) != 0) {
					buffer.put((byte) (value0 >>> ((i - 1) * 8)));
					i--;
				}

				// Now i is even: prefer largest chunks first
				if (i == 8) { // exactly 8 bytes
					buffer.putLong(value0);
				} else {
					if (i >= 4) { // write next 4 bytes, if any
						int shift = (i - 4) * 8;
						buffer.putInt((int) (value0 >>> shift));
						i -= 4;
					}
					while (i >= 2) { // write remaining pairs
						int shift = (i - 2) * 8;
						buffer.putShort((short) (value0 >>> shift));
						i -= 2;
					} // i must be 0 here.
				}

			} finally {
				if (prev != ByteOrder.BIG_ENDIAN) {
					buffer.order(prev);
				}
			}
			return buffer.array();

		}

	}

	public boolean matches(ByteBuffer other) {
		return matcher.matches(other);
	}

	@FunctionalInterface
	private interface MatchFn {
		boolean matches(ByteBuffer other);
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

	private boolean match0000(ByteBuffer other) {
		return true;
	}

	private boolean match0001(ByteBuffer other) {
		byte otherFirst0 = other.get();
		if (firstByte0 == otherFirst0) {
			return length0 == 1 || cmp0.equals(otherFirst0, other);
		}
		return false;
	}

	private boolean match0010(ByteBuffer other) {

		skipAhead(other);

		byte otherFirst1 = other.get();
		if (firstByte1 == otherFirst1) {
			return length1 == 1 || cmp1.equals(otherFirst1, other);
		}
		return false;
	}

	private boolean match0011(ByteBuffer other) {
		byte otherFirst0 = other.get();
		if (firstByte0 == otherFirst0) {
			if (length0 == 1 || cmp0.equals(otherFirst0, other)) {
				byte otherFirst1 = other.get();
				if (firstByte1 == otherFirst1) {
					return length1 == 1 || cmp1.equals(otherFirst1, other);
				}
			}
		}

		return false;
	}

	private boolean match0100(ByteBuffer other) {

		skipAhead(other);
		skipAhead(other);

		byte otherFirst2 = other.get();
		if (firstByte2 == otherFirst2) {
			return length2 == 1 || cmp2.equals(otherFirst2, other);
		}
		return false;
	}

	private boolean match0101(ByteBuffer other) {

		byte otherFirst0 = other.get();
		if (firstByte0 == otherFirst0) {
			if (length0 == 1 || cmp0.equals(otherFirst0, other)) {
				skipAhead(other);

				byte otherFirst2 = other.get();
				if (firstByte2 == otherFirst2) {
					return length2 == 1 || cmp2.equals(otherFirst2, other);
				}
			}
		}
		return false;
	}

	private boolean match0110(ByteBuffer other) {

		skipAhead(other);

		byte otherFirst1 = other.get();
		if (firstByte1 == otherFirst1) {
			if (length1 == 1 || cmp1.equals(otherFirst1, other)) {
				byte otherFirst2 = other.get();
				if (firstByte2 == otherFirst2) {
					return length2 == 1 || cmp2.equals(otherFirst2, other);
				}
			}
		}
		return false;
	}

	private void skipAhead(ByteBuffer other) {
		int i = firstToLength(other.get()) - 1;
		assert i >= 0;
		if (i > 0) {
			other.position(i + other.position());
		}
	}

	private boolean match0111(ByteBuffer other) {

		byte otherFirst0 = other.get();
		if (firstByte0 == otherFirst0) {
			if (length0 == 1 || cmp0.equals(otherFirst0, other)) {
				byte otherFirst1 = other.get();
				if (firstByte1 == otherFirst1) {
					if (length1 == 1 || cmp1.equals(otherFirst1, other)) {
						byte otherFirst2 = other.get();
						if (firstByte2 == otherFirst2) {
							return length2 == 1 || cmp2.equals(otherFirst2, other);
						}
					}
				}
			}
		}
		return false;
	}

	private boolean match1000(ByteBuffer other) {

		skipAhead(other);
		skipAhead(other);
		skipAhead(other);

		byte otherFirst3 = other.get();
		if (firstByte3 == otherFirst3) {
			return length3 == 1 || cmp3.equals(otherFirst3, other);
		}
		return false;
	}

	private boolean match1001(ByteBuffer other) {

		byte otherFirst0 = other.get();
		if (firstByte0 == otherFirst0) {
			if (length0 == 1 || cmp0.equals(otherFirst0, other)) {
				skipAhead(other);
				skipAhead(other);

				byte otherFirst3 = other.get();
				if (firstByte3 == otherFirst3) {
					return length3 == 1 || cmp3.equals(otherFirst3, other);
				}
			}
		}
		return false;
	}

	private boolean match1010(ByteBuffer other) {

		skipAhead(other);
		byte otherFirst1 = other.get();
		if (firstByte1 == otherFirst1) {
			if (length1 == 1 || cmp1.equals(otherFirst1, other)) {
				skipAhead(other);

				byte otherFirst3 = other.get();
				if (firstByte3 == otherFirst3) {
					return length3 == 1 || cmp3.equals(otherFirst3, other);
				}
			}
		}
		return false;
	}

	private boolean match1011(ByteBuffer other) {

		byte otherFirst0 = other.get();
		if (firstByte0 == otherFirst0) {
			if (length0 == 1 || cmp0.equals(otherFirst0, other)) {
				byte otherFirst1 = other.get();
				if (firstByte1 == otherFirst1) {
					if (length1 == 1 || cmp1.equals(otherFirst1, other)) {
						skipAhead(other);

						byte otherFirst3 = other.get();
						if (firstByte3 == otherFirst3) {
							return length3 == 1 || cmp3.equals(otherFirst3, other);
						}
					}
				}
			}
		}
		return false;
	}

	private boolean match1100(ByteBuffer other) {

		skipAhead(other);
		skipAhead(other);

		byte otherFirst2 = other.get();
		if (firstByte2 == otherFirst2) {
			if (length2 == 1 || cmp2.equals(otherFirst2, other)) {
				byte otherFirst3 = other.get();
				if (firstByte3 == otherFirst3) {
					return length3 == 1 || cmp3.equals(otherFirst3, other);
				}
			}
		}
		return false;
	}

	private boolean match1101(ByteBuffer other) {

		byte otherFirst0 = other.get();
		if (firstByte0 == otherFirst0) {
			if (length0 == 1 || cmp0.equals(otherFirst0, other)) {
				skipAhead(other);

				byte otherFirst2 = other.get();
				if (firstByte2 == otherFirst2) {
					if (length2 == 1 || cmp2.equals(otherFirst2, other)) {
						byte otherFirst3 = other.get();
						if (firstByte3 == otherFirst3) {
							return length3 == 1 || cmp3.equals(otherFirst3, other);
						}
					}
				}
			}
		}
		return false;
	}

	private boolean match1110(ByteBuffer other) {

		skipAhead(other);

		byte otherFirst1 = other.get();
		if (firstByte1 == otherFirst1) {
			if (length1 == 1 || cmp1.equals(otherFirst1, other)) {
				byte otherFirst2 = other.get();
				if (firstByte2 == otherFirst2) {
					if (length2 == 1 || cmp2.equals(otherFirst2, other)) {
						byte otherFirst3 = other.get();
						if (firstByte3 == otherFirst3) {
							return length3 == 1 || cmp3.equals(otherFirst3, other);
						}
					}
				}
			}
		}
		return false;
	}

	private boolean match1111(ByteBuffer other) {
		byte otherFirst0 = other.get();
		if (firstByte0 == otherFirst0) {
			if (length0 == 1 || cmp0.equals(otherFirst0, other)) {
				byte otherFirst1 = other.get();
				if (firstByte1 == otherFirst1) {
					if (length1 == 1 || cmp1.equals(otherFirst1, other)) {
						byte otherFirst2 = other.get();
						if (firstByte2 == otherFirst2) {
							if (length2 == 1 || cmp2.equals(otherFirst2, other)) {
								byte otherFirst3 = other.get();
								if (firstByte3 == otherFirst3) {
									return length3 == 1 || cmp3.equals(otherFirst3, other);
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
