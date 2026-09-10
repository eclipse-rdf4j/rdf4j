/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

/** Internal lexical addressing. Implementations expose bytes only under an owning callback/lease. */
interface RecordByteAccess {
	int length();

	int byteAt(int index);

	default void copy(int from, byte[] target, int offset, int count) {
		Objects.checkFromIndexSize(from, count, length());
		Objects.checkFromIndexSize(offset, count, target.length);
		for (int i = 0; i < count; i++)
			target[offset + i] = (byte) byteAt(from + i);
	}

	static RecordByteAccess raw(MemorySegment bytes) {
		int size = Math.toIntExact(bytes.byteSize());
		return new RecordByteAccess() {
			public int length() {
				return size;
			}

			public int byteAt(int index) {
				Objects.checkIndex(index, size);
				return bytes.get(ValueLayout.JAVA_BYTE, index) & 255;
			}

			@Override
			public void copy(int from, byte[] target, int offset, int count) {
				Objects.checkFromIndexSize(from, count, size);
				Objects.checkFromIndexSize(offset, count, target.length);
				MemorySegment.copy(bytes, from, MemorySegment.ofArray(target), offset, count);
			}
		};
	}
}
