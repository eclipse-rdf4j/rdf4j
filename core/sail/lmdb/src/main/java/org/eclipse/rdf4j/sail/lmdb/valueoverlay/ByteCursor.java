/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

public final class ByteCursor {
	private final byte[] bytes;
	private int position;

	public ByteCursor(byte[] bytes) {
		this.bytes = bytes;
	}

	public int position() {
		return position;
	}

	public int remaining() {
		return bytes.length - position;
	}

	public boolean hasRemaining() {
		return position < bytes.length;
	}

	public int readUnsignedByte() {
		if (position >= bytes.length) {
			throw malformed();
		}
		return bytes[position++] & 0xff;
	}

	public int readVarUInt() {
		int result = 0;
		for (int shift = 0; shift < 35; shift += 7) {
			int b = readUnsignedByte();
			result |= (b & 0x7f) << shift;
			if ((b & 0x80) == 0) {
				if (result < 0)
					throw malformed();
				return result;
			}
		}
		throw malformed();
	}

	public long readVarULong() {
		long result = 0;
		for (int shift = 0; shift < 64; shift += 7) {
			int b = readUnsignedByte();
			result |= (long) (b & 0x7f) << shift;
			if ((b & 0x80) == 0) {
				if (result < 0)
					throw malformed();
				return result;
			}
		}
		throw malformed();
	}

	public int readIntLE() {
		require(4);
		int result = (bytes[position] & 0xff)
				| ((bytes[position + 1] & 0xff) << 8)
				| ((bytes[position + 2] & 0xff) << 16)
				| ((bytes[position + 3] & 0xff) << 24);
		position += 4;
		return result;
	}

	public long readLongLE() {
		require(8);
		long result = 0;
		for (int shift = 0; shift < 64; shift += 8) {
			result |= (long) (bytes[position++] & 0xff) << shift;
		}
		return result;
	}

	public byte[] readBytes(int length) {
		require(length);
		byte[] result = new byte[length];
		System.arraycopy(bytes, position, result, 0, length);
		position += length;
		return result;
	}

	public byte[] readRemaining() {
		return readBytes(remaining());
	}

	public void requireFullyConsumed() {
		if (position != bytes.length) {
			throw malformed();
		}
	}

	private void require(int count) {
		if (count < 0 || position > bytes.length - count) {
			throw malformed();
		}
	}

	private static IllegalStateException malformed() {
		return new IllegalStateException("corrupt cached record");
	}
}
