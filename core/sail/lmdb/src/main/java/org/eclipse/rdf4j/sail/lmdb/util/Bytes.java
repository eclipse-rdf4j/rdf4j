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

public final class Bytes {
	private Bytes() {
	}

	@FunctionalInterface
	public interface RegionComparator {
		int compare(byte firstByte, ByteBuffer other, int otherPos);
	}

	private static int d(int a, int b) {
		return (a & 0xFF) - (b & 0xFF);
	}

	public static RegionComparator capturedComparator(byte[] array, int offset, int len) {
		if (len <= 0) {
			return (firstByte, b, bi) -> 0;
		}
		switch (len) {
		case 1:
			return comparatorLen1(array, offset);
		case 2:
			return comparatorLen2(array, offset);
		case 3:
			return comparatorLen3(array, offset);
		case 4:
			return comparatorLen4(array, offset);
		case 5:
			return comparatorLen5(array, offset);
		case 6:
			return comparatorLen6(array, offset);
		case 7:
			return comparatorLen7(array, offset);
		case 8:
			return comparatorLen8(array, offset);
		case 9:
			return comparatorLen9(array, offset);
		case 10:
			return comparatorLen10(array, offset);
		case 11:
			return comparatorLen11(array, offset);
		case 12:
			return comparatorLen12(array, offset);
		case 13:
			return comparatorLen13(array, offset);
		case 14:
			return comparatorLen14(array, offset);
		case 15:
			return comparatorLen15(array, offset);
		case 16:
			return comparatorLen16(array, offset);
		case 17:
			return comparatorLen17(array, offset);
		case 18:
			return comparatorLen18(array, offset);
		case 19:
			return comparatorLen19(array, offset);
		case 20:
			return comparatorLen20(array, offset);
		default:
			return comparatorGeneric(array, offset, len);
		}
	}

	private static RegionComparator comparatorLen1(byte[] array, int offset) {
		final int i0 = offset;
		return (firstByte, b, bi) -> d(array[i0], firstByte);
	}

	private static RegionComparator comparatorLen2(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			return d(array[i1], b.get(bi + 1));
		};
	}

	private static RegionComparator comparatorLen3(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			return d(array[i2], b.get(bi + 2));
		};
	}

	private static RegionComparator comparatorLen4(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			b.position(bi+1);
			r = d(array[i1], b.get());
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get());
			if (r != 0) {
				return r;
			}
			return d(array[i3], b.get());
		};
	}

	private static RegionComparator comparatorLen5(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get(bi + 2));
			if (r != 0) {
				return r;
			}
			r = d(array[i3], b.get(bi + 3));
			if (r != 0) {
				return r;
			}
			return d(array[i4], b.get(bi + 4));
		};
	}

	private static RegionComparator comparatorLen6(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		final int i5 = offset + 5;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get(bi + 2));
			if (r != 0) {
				return r;
			}
			r = d(array[i3], b.get(bi + 3));
			if (r != 0) {
				return r;
			}
			r = d(array[i4], b.get(bi + 4));
			if (r != 0) {
				return r;
			}
			return d(array[i5], b.get(bi + 5));
		};
	}

	private static RegionComparator comparatorLen7(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		final int i5 = offset + 5;
		final int i6 = offset + 6;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get(bi + 2));
			if (r != 0) {
				return r;
			}
			r = d(array[i3], b.get(bi + 3));
			if (r != 0) {
				return r;
			}
			r = d(array[i4], b.get(bi + 4));
			if (r != 0) {
				return r;
			}
			r = d(array[i5], b.get(bi + 5));
			if (r != 0) {
				return r;
			}
			return d(array[i6], b.get(bi + 6));
		};
	}

	private static RegionComparator comparatorLen8(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		final int i5 = offset + 5;
		final int i6 = offset + 6;
		final int i7 = offset + 7;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get(bi + 2));
			if (r != 0) {
				return r;
			}
			r = d(array[i3], b.get(bi + 3));
			if (r != 0) {
				return r;
			}
			r = d(array[i4], b.get(bi + 4));
			if (r != 0) {
				return r;
			}
			r = d(array[i5], b.get(bi + 5));
			if (r != 0) {
				return r;
			}
			r = d(array[i6], b.get(bi + 6));
			if (r != 0) {
				return r;
			}
			return d(array[i7], b.get(bi + 7));
		};
	}

	private static RegionComparator comparatorLen9(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		final int i5 = offset + 5;
		final int i6 = offset + 6;
		final int i7 = offset + 7;
		final int i8 = offset + 8;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get(bi + 2));
			if (r != 0) {
				return r;
			}
			r = d(array[i3], b.get(bi + 3));
			if (r != 0) {
				return r;
			}
			r = d(array[i4], b.get(bi + 4));
			if (r != 0) {
				return r;
			}
			r = d(array[i5], b.get(bi + 5));
			if (r != 0) {
				return r;
			}
			r = d(array[i6], b.get(bi + 6));
			if (r != 0) {
				return r;
			}
			r = d(array[i7], b.get(bi + 7));
			if (r != 0) {
				return r;
			}
			return d(array[i8], b.get(bi + 8));
		};
	}

	private static RegionComparator comparatorLen10(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		final int i5 = offset + 5;
		final int i6 = offset + 6;
		final int i7 = offset + 7;
		final int i8 = offset + 8;
		final int i9 = offset + 9;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get(bi + 2));
			if (r != 0) {
				return r;
			}
			r = d(array[i3], b.get(bi + 3));
			if (r != 0) {
				return r;
			}
			r = d(array[i4], b.get(bi + 4));
			if (r != 0) {
				return r;
			}
			r = d(array[i5], b.get(bi + 5));
			if (r != 0) {
				return r;
			}
			r = d(array[i6], b.get(bi + 6));
			if (r != 0) {
				return r;
			}
			r = d(array[i7], b.get(bi + 7));
			if (r != 0) {
				return r;
			}
			r = d(array[i8], b.get(bi + 8));
			if (r != 0) {
				return r;
			}
			r = d(array[i9], b.get(bi + 9));
			if (r != 0) {
				return r;
			}
			return 0;
		};
	}

	private static RegionComparator comparatorLen11(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		final int i5 = offset + 5;
		final int i6 = offset + 6;
		final int i7 = offset + 7;
		final int i8 = offset + 8;
		final int i9 = offset + 9;
		final int i10 = offset + 10;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get(bi + 2));
			if (r != 0) {
				return r;
			}
			r = d(array[i3], b.get(bi + 3));
			if (r != 0) {
				return r;
			}
			r = d(array[i4], b.get(bi + 4));
			if (r != 0) {
				return r;
			}
			r = d(array[i5], b.get(bi + 5));
			if (r != 0) {
				return r;
			}
			r = d(array[i6], b.get(bi + 6));
			if (r != 0) {
				return r;
			}
			r = d(array[i7], b.get(bi + 7));
			if (r != 0) {
				return r;
			}
			r = d(array[i8], b.get(bi + 8));
			if (r != 0) {
				return r;
			}
			r = d(array[i9], b.get(bi + 9));
			if (r != 0) {
				return r;
			}
			r = d(array[i10], b.get(bi + 10));
			if (r != 0) {
				return r;
			}
			return 0;
		};
	}

	private static RegionComparator comparatorLen12(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		final int i5 = offset + 5;
		final int i6 = offset + 6;
		final int i7 = offset + 7;
		final int i8 = offset + 8;
		final int i9 = offset + 9;
		final int i10 = offset + 10;
		final int i11 = offset + 11;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get(bi + 2));
			if (r != 0) {
				return r;
			}
			r = d(array[i3], b.get(bi + 3));
			if (r != 0) {
				return r;
			}
			r = d(array[i4], b.get(bi + 4));
			if (r != 0) {
				return r;
			}
			r = d(array[i5], b.get(bi + 5));
			if (r != 0) {
				return r;
			}
			r = d(array[i6], b.get(bi + 6));
			if (r != 0) {
				return r;
			}
			r = d(array[i7], b.get(bi + 7));
			if (r != 0) {
				return r;
			}
			r = d(array[i8], b.get(bi + 8));
			if (r != 0) {
				return r;
			}
			r = d(array[i9], b.get(bi + 9));
			if (r != 0) {
				return r;
			}
			r = d(array[i10], b.get(bi + 10));
			if (r != 0) {
				return r;
			}
			r = d(array[i11], b.get(bi + 11));
			if (r != 0) {
				return r;
			}
			return 0;
		};
	}

	private static RegionComparator comparatorLen13(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		final int i5 = offset + 5;
		final int i6 = offset + 6;
		final int i7 = offset + 7;
		final int i8 = offset + 8;
		final int i9 = offset + 9;
		final int i10 = offset + 10;
		final int i11 = offset + 11;
		final int i12 = offset + 12;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get(bi + 2));
			if (r != 0) {
				return r;
			}
			r = d(array[i3], b.get(bi + 3));
			if (r != 0) {
				return r;
			}
			r = d(array[i4], b.get(bi + 4));
			if (r != 0) {
				return r;
			}
			r = d(array[i5], b.get(bi + 5));
			if (r != 0) {
				return r;
			}
			r = d(array[i6], b.get(bi + 6));
			if (r != 0) {
				return r;
			}
			r = d(array[i7], b.get(bi + 7));
			if (r != 0) {
				return r;
			}
			r = d(array[i8], b.get(bi + 8));
			if (r != 0) {
				return r;
			}
			r = d(array[i9], b.get(bi + 9));
			if (r != 0) {
				return r;
			}
			r = d(array[i10], b.get(bi + 10));
			if (r != 0) {
				return r;
			}
			r = d(array[i11], b.get(bi + 11));
			if (r != 0) {
				return r;
			}
			r = d(array[i12], b.get(bi + 12));
			if (r != 0) {
				return r;
			}
			return 0;
		};
	}

	private static RegionComparator comparatorLen14(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		final int i5 = offset + 5;
		final int i6 = offset + 6;
		final int i7 = offset + 7;
		final int i8 = offset + 8;
		final int i9 = offset + 9;
		final int i10 = offset + 10;
		final int i11 = offset + 11;
		final int i12 = offset + 12;
		final int i13 = offset + 13;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get(bi + 2));
			if (r != 0) {
				return r;
			}
			r = d(array[i3], b.get(bi + 3));
			if (r != 0) {
				return r;
			}
			r = d(array[i4], b.get(bi + 4));
			if (r != 0) {
				return r;
			}
			r = d(array[i5], b.get(bi + 5));
			if (r != 0) {
				return r;
			}
			r = d(array[i6], b.get(bi + 6));
			if (r != 0) {
				return r;
			}
			r = d(array[i7], b.get(bi + 7));
			if (r != 0) {
				return r;
			}
			r = d(array[i8], b.get(bi + 8));
			if (r != 0) {
				return r;
			}
			r = d(array[i9], b.get(bi + 9));
			if (r != 0) {
				return r;
			}
			r = d(array[i10], b.get(bi + 10));
			if (r != 0) {
				return r;
			}
			r = d(array[i11], b.get(bi + 11));
			if (r != 0) {
				return r;
			}
			r = d(array[i12], b.get(bi + 12));
			if (r != 0) {
				return r;
			}
			r = d(array[i13], b.get(bi + 13));
			if (r != 0) {
				return r;
			}
			return 0;
		};
	}

	private static RegionComparator comparatorLen15(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		final int i5 = offset + 5;
		final int i6 = offset + 6;
		final int i7 = offset + 7;
		final int i8 = offset + 8;
		final int i9 = offset + 9;
		final int i10 = offset + 10;
		final int i11 = offset + 11;
		final int i12 = offset + 12;
		final int i13 = offset + 13;
		final int i14 = offset + 14;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get(bi + 2));
			if (r != 0) {
				return r;
			}
			r = d(array[i3], b.get(bi + 3));
			if (r != 0) {
				return r;
			}
			r = d(array[i4], b.get(bi + 4));
			if (r != 0) {
				return r;
			}
			r = d(array[i5], b.get(bi + 5));
			if (r != 0) {
				return r;
			}
			r = d(array[i6], b.get(bi + 6));
			if (r != 0) {
				return r;
			}
			r = d(array[i7], b.get(bi + 7));
			if (r != 0) {
				return r;
			}
			r = d(array[i8], b.get(bi + 8));
			if (r != 0) {
				return r;
			}
			r = d(array[i9], b.get(bi + 9));
			if (r != 0) {
				return r;
			}
			r = d(array[i10], b.get(bi + 10));
			if (r != 0) {
				return r;
			}
			r = d(array[i11], b.get(bi + 11));
			if (r != 0) {
				return r;
			}
			r = d(array[i12], b.get(bi + 12));
			if (r != 0) {
				return r;
			}
			r = d(array[i13], b.get(bi + 13));
			if (r != 0) {
				return r;
			}
			r = d(array[i14], b.get(bi + 14));
			if (r != 0) {
				return r;
			}
			return 0;
		};
	}

	private static RegionComparator comparatorLen16(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		final int i5 = offset + 5;
		final int i6 = offset + 6;
		final int i7 = offset + 7;
		final int i8 = offset + 8;
		final int i9 = offset + 9;
		final int i10 = offset + 10;
		final int i11 = offset + 11;
		final int i12 = offset + 12;
		final int i13 = offset + 13;
		final int i14 = offset + 14;
		final int i15 = offset + 15;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get(bi + 2));
			if (r != 0) {
				return r;
			}
			r = d(array[i3], b.get(bi + 3));
			if (r != 0) {
				return r;
			}
			r = d(array[i4], b.get(bi + 4));
			if (r != 0) {
				return r;
			}
			r = d(array[i5], b.get(bi + 5));
			if (r != 0) {
				return r;
			}
			r = d(array[i6], b.get(bi + 6));
			if (r != 0) {
				return r;
			}
			r = d(array[i7], b.get(bi + 7));
			if (r != 0) {
				return r;
			}
			r = d(array[i8], b.get(bi + 8));
			if (r != 0) {
				return r;
			}
			r = d(array[i9], b.get(bi + 9));
			if (r != 0) {
				return r;
			}
			r = d(array[i10], b.get(bi + 10));
			if (r != 0) {
				return r;
			}
			r = d(array[i11], b.get(bi + 11));
			if (r != 0) {
				return r;
			}
			r = d(array[i12], b.get(bi + 12));
			if (r != 0) {
				return r;
			}
			r = d(array[i13], b.get(bi + 13));
			if (r != 0) {
				return r;
			}
			r = d(array[i14], b.get(bi + 14));
			if (r != 0) {
				return r;
			}
			r = d(array[i15], b.get(bi + 15));
			if (r != 0) {
				return r;
			}
			return 0;
		};
	}

	private static RegionComparator comparatorLen17(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		final int i5 = offset + 5;
		final int i6 = offset + 6;
		final int i7 = offset + 7;
		final int i8 = offset + 8;
		final int i9 = offset + 9;
		final int i10 = offset + 10;
		final int i11 = offset + 11;
		final int i12 = offset + 12;
		final int i13 = offset + 13;
		final int i14 = offset + 14;
		final int i15 = offset + 15;
		final int i16 = offset + 16;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get(bi + 2));
			if (r != 0) {
				return r;
			}
			r = d(array[i3], b.get(bi + 3));
			if (r != 0) {
				return r;
			}
			r = d(array[i4], b.get(bi + 4));
			if (r != 0) {
				return r;
			}
			r = d(array[i5], b.get(bi + 5));
			if (r != 0) {
				return r;
			}
			r = d(array[i6], b.get(bi + 6));
			if (r != 0) {
				return r;
			}
			r = d(array[i7], b.get(bi + 7));
			if (r != 0) {
				return r;
			}
			r = d(array[i8], b.get(bi + 8));
			if (r != 0) {
				return r;
			}
			r = d(array[i9], b.get(bi + 9));
			if (r != 0) {
				return r;
			}
			r = d(array[i10], b.get(bi + 10));
			if (r != 0) {
				return r;
			}
			r = d(array[i11], b.get(bi + 11));
			if (r != 0) {
				return r;
			}
			r = d(array[i12], b.get(bi + 12));
			if (r != 0) {
				return r;
			}
			r = d(array[i13], b.get(bi + 13));
			if (r != 0) {
				return r;
			}
			r = d(array[i14], b.get(bi + 14));
			if (r != 0) {
				return r;
			}
			r = d(array[i15], b.get(bi + 15));
			if (r != 0) {
				return r;
			}
			r = d(array[i16], b.get(bi + 16));
			if (r != 0) {
				return r;
			}
			return 0;
		};
	}

	private static RegionComparator comparatorLen18(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		final int i5 = offset + 5;
		final int i6 = offset + 6;
		final int i7 = offset + 7;
		final int i8 = offset + 8;
		final int i9 = offset + 9;
		final int i10 = offset + 10;
		final int i11 = offset + 11;
		final int i12 = offset + 12;
		final int i13 = offset + 13;
		final int i14 = offset + 14;
		final int i15 = offset + 15;
		final int i16 = offset + 16;
		final int i17 = offset + 17;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get(bi + 2));
			if (r != 0) {
				return r;
			}
			r = d(array[i3], b.get(bi + 3));
			if (r != 0) {
				return r;
			}
			r = d(array[i4], b.get(bi + 4));
			if (r != 0) {
				return r;
			}
			r = d(array[i5], b.get(bi + 5));
			if (r != 0) {
				return r;
			}
			r = d(array[i6], b.get(bi + 6));
			if (r != 0) {
				return r;
			}
			r = d(array[i7], b.get(bi + 7));
			if (r != 0) {
				return r;
			}
			r = d(array[i8], b.get(bi + 8));
			if (r != 0) {
				return r;
			}
			r = d(array[i9], b.get(bi + 9));
			if (r != 0) {
				return r;
			}
			r = d(array[i10], b.get(bi + 10));
			if (r != 0) {
				return r;
			}
			r = d(array[i11], b.get(bi + 11));
			if (r != 0) {
				return r;
			}
			r = d(array[i12], b.get(bi + 12));
			if (r != 0) {
				return r;
			}
			r = d(array[i13], b.get(bi + 13));
			if (r != 0) {
				return r;
			}
			r = d(array[i14], b.get(bi + 14));
			if (r != 0) {
				return r;
			}
			r = d(array[i15], b.get(bi + 15));
			if (r != 0) {
				return r;
			}
			r = d(array[i16], b.get(bi + 16));
			if (r != 0) {
				return r;
			}
			r = d(array[i17], b.get(bi + 17));
			if (r != 0) {
				return r;
			}
			return 0;
		};
	}

	private static RegionComparator comparatorLen19(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		final int i5 = offset + 5;
		final int i6 = offset + 6;
		final int i7 = offset + 7;
		final int i8 = offset + 8;
		final int i9 = offset + 9;
		final int i10 = offset + 10;
		final int i11 = offset + 11;
		final int i12 = offset + 12;
		final int i13 = offset + 13;
		final int i14 = offset + 14;
		final int i15 = offset + 15;
		final int i16 = offset + 16;
		final int i17 = offset + 17;
		final int i18 = offset + 18;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get(bi + 2));
			if (r != 0) {
				return r;
			}
			r = d(array[i3], b.get(bi + 3));
			if (r != 0) {
				return r;
			}
			r = d(array[i4], b.get(bi + 4));
			if (r != 0) {
				return r;
			}
			r = d(array[i5], b.get(bi + 5));
			if (r != 0) {
				return r;
			}
			r = d(array[i6], b.get(bi + 6));
			if (r != 0) {
				return r;
			}
			r = d(array[i7], b.get(bi + 7));
			if (r != 0) {
				return r;
			}
			r = d(array[i8], b.get(bi + 8));
			if (r != 0) {
				return r;
			}
			r = d(array[i9], b.get(bi + 9));
			if (r != 0) {
				return r;
			}
			r = d(array[i10], b.get(bi + 10));
			if (r != 0) {
				return r;
			}
			r = d(array[i11], b.get(bi + 11));
			if (r != 0) {
				return r;
			}
			r = d(array[i12], b.get(bi + 12));
			if (r != 0) {
				return r;
			}
			r = d(array[i13], b.get(bi + 13));
			if (r != 0) {
				return r;
			}
			r = d(array[i14], b.get(bi + 14));
			if (r != 0) {
				return r;
			}
			r = d(array[i15], b.get(bi + 15));
			if (r != 0) {
				return r;
			}
			r = d(array[i16], b.get(bi + 16));
			if (r != 0) {
				return r;
			}
			r = d(array[i17], b.get(bi + 17));
			if (r != 0) {
				return r;
			}
			r = d(array[i18], b.get(bi + 18));
			if (r != 0) {
				return r;
			}
			return 0;
		};
	}

	private static RegionComparator comparatorLen20(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		final int i5 = offset + 5;
		final int i6 = offset + 6;
		final int i7 = offset + 7;
		final int i8 = offset + 8;
		final int i9 = offset + 9;
		final int i10 = offset + 10;
		final int i11 = offset + 11;
		final int i12 = offset + 12;
		final int i13 = offset + 13;
		final int i14 = offset + 14;
		final int i15 = offset + 15;
		final int i16 = offset + 16;
		final int i17 = offset + 17;
		final int i18 = offset + 18;
		final int i19 = offset + 19;
		return (firstByte, b, bi) -> {
			int r = d(array[i0], firstByte);
			if (r != 0) {
				return r;
			}
			r = d(array[i1], b.get(bi + 1));
			if (r != 0) {
				return r;
			}
			r = d(array[i2], b.get(bi + 2));
			if (r != 0) {
				return r;
			}
			r = d(array[i3], b.get(bi + 3));
			if (r != 0) {
				return r;
			}
			r = d(array[i4], b.get(bi + 4));
			if (r != 0) {
				return r;
			}
			r = d(array[i5], b.get(bi + 5));
			if (r != 0) {
				return r;
			}
			r = d(array[i6], b.get(bi + 6));
			if (r != 0) {
				return r;
			}
			r = d(array[i7], b.get(bi + 7));
			if (r != 0) {
				return r;
			}
			r = d(array[i8], b.get(bi + 8));
			if (r != 0) {
				return r;
			}
			r = d(array[i9], b.get(bi + 9));
			if (r != 0) {
				return r;
			}
			r = d(array[i10], b.get(bi + 10));
			if (r != 0) {
				return r;
			}
			r = d(array[i11], b.get(bi + 11));
			if (r != 0) {
				return r;
			}
			r = d(array[i12], b.get(bi + 12));
			if (r != 0) {
				return r;
			}
			r = d(array[i13], b.get(bi + 13));
			if (r != 0) {
				return r;
			}
			r = d(array[i14], b.get(bi + 14));
			if (r != 0) {
				return r;
			}
			r = d(array[i15], b.get(bi + 15));
			if (r != 0) {
				return r;
			}
			r = d(array[i16], b.get(bi + 16));
			if (r != 0) {
				return r;
			}
			r = d(array[i17], b.get(bi + 17));
			if (r != 0) {
				return r;
			}
			r = d(array[i18], b.get(bi + 18));
			if (r != 0) {
				return r;
			}
			r = d(array[i19], b.get(bi + 19));
			if (r != 0) {
				return r;
			}
			return 0;
		};
	}

	private static RegionComparator comparatorGeneric(byte[] array, int offset, int len) {
		final int start = offset;
		final int end = offset + len;
		return (firstByte, b, bi) -> {
			int r = d(array[start], firstByte);
			if (r != 0) {
				return r;
			}
			int idx = start + 1;
			int bj = bi + 1;
			while (idx < end) {
				r = d(array[idx], b.get(bj));
				if (r != 0) {
					return r;
				}
				idx++;
				bj++;
			}
			return 0;
		};
	}
}
