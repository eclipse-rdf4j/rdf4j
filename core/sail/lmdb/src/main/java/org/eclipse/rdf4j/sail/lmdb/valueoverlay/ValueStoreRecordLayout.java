/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

/**
 * Compression hints for current ValueStoreRecordCodec bytes. Never normalizes labels, datatypes, language or direction.
 * Unknown/malformed layouts fall back to opaque records. Stable core tags are hints only, not a parser/type proof.
 */
public final class ValueStoreRecordLayout {
	private ValueStoreRecordLayout() {
	}

	public static RecordFamily family(long id, byte[] record) {
		if (record.length == 0)
			return RecordFamily.OTHER;
		return switch (record[0]) {
		case 0 -> RecordFamily.IRI;
		case 2 -> RecordFamily.BNODE;
		case 4 -> RecordFamily.NAMESPACE;
		case 1 -> literalFamily(id, record);
		default -> RecordFamily.OTHER;
		};
	}

	private static RecordFamily literalFamily(long id, byte[] record) {
		int header = headerLength(record);
		if (header == 0)
			return RecordFamily.OTHER;
		int lengthByte = 1 + varintLength(record[1]);
		if ((record[lengthByte] & 0x3f) != 0 || (record[lengthByte] & 0xc0) != 0)
			return RecordFamily.LANGUAGE;
		if ((id & 1) != 0 || ((id >>> 1) & 63) != 5)
			return RecordFamily.TEXT;
		int tag = (int) (id >>> 7) & 63;
		return switch (tag) {
		case 14 -> RecordFamily.BOOLEAN;
		case 15, 30, 31, 33, 34, 35, 36, 38, 39, 43, 44, 45, 46 -> RecordFamily.INTEGER;
		case 20, 21, 23 -> RecordFamily.DECIMAL_FLOAT;
		case 16, 17, 18, 19, 22, 24, 25, 26, 27, 28, 41, 47 -> RecordFamily.TEMPORAL;
		case 13, 29 -> RecordFamily.BINARY;
		default -> RecordFamily.TEXT;
		};
	}

	public static long affinity(byte[] record) {
		int length = headerLength(record);
		long h = 0xcbf29ce484222325L;
		for (int i = 0; i < length; i++) {
			h ^= record[i] & 255L;
			h *= 0x100000001b3L;
		}
		return h;
	}

	static int headerLength(byte[] record) {
		if (record.length == 0)
			return 0;
		if (record[0] != 0 && record[0] != 1)
			return 1;
		if (record.length < 2)
			return 0;
		int prefix = 1 + varintLength(record[1]);
		if (prefix > record.length)
			return 0;
		if (record[0] == 0)
			return prefix;
		if (prefix == record.length)
			return 0;
		int length = (record[prefix] & 63) + prefix + 1;
		return length <= record.length ? length : 0;
	}

	private static int varintLength(byte first) {
		int b = first & 255;
		return b <= 240 ? 1 : b <= 248 ? 2 : b - 246;
	}
}
