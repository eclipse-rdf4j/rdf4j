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
		int compare(ByteBuffer other, int otherPos);
	}

	private static int d(int a, int b) {
		return (a & 0xFF) - (b & 0xFF);
	}

	public static RegionComparator capturedComparator(byte[] array, int offset, int len) {
		if (len <= 0) {
			return (b, bi) -> 0;
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
		default:
			return comparatorGeneric(array, offset, len);
		}
	}

	private static RegionComparator comparatorLen1(byte[] array, int offset) {
		final int i0 = offset;
		return (b, bi) -> d(array[i0], b.get(bi));
	}

	private static RegionComparator comparatorLen2(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		return (b, bi) -> {
			int r = d(array[i0], b.get(bi));
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
		return (b, bi) -> {
			int r = d(array[i0], b.get(bi));
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
		return (b, bi) -> {
			int r = d(array[i0], b.get(bi));
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
			return d(array[i3], b.get(bi + 3));
		};
	}

	private static RegionComparator comparatorLen5(byte[] array, int offset) {
		final int i0 = offset;
		final int i1 = offset + 1;
		final int i2 = offset + 2;
		final int i3 = offset + 3;
		final int i4 = offset + 4;
		return (b, bi) -> {
			int r = d(array[i0], b.get(bi));
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
		return (b, bi) -> {
			int r = d(array[i0], b.get(bi));
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
		return (b, bi) -> {
			int r = d(array[i0], b.get(bi));
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
		return (b, bi) -> {
			int r = d(array[i0], b.get(bi));
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
		return (b, bi) -> {
			int r = d(array[i0], b.get(bi));
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

	private static RegionComparator comparatorGeneric(byte[] array, int offset, int len) {
		final int start = offset;
		final int end = offset + len;
		return (b, bi) -> {
			int idx = start;
			int bj = bi;
			while (idx < end) {
				int r = d(array[idx], b.get(bj));
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
