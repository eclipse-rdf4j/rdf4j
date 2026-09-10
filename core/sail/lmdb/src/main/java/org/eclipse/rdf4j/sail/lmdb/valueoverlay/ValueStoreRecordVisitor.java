/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

import java.lang.foreign.MemorySegment;
import java.util.Objects;

/**
 * Scoped, lexical-preserving typed access. A visitor never receives a native pointer or mutable decoder buffer. Only
 * requested byte regions are decoded. Kind/reference/header inspection does not reconstruct the label. The callback
 * MUST NOT retain the record or close its source lease; every accessor rejects use after the callback, including
 * metadata. The caller owns and holds the source lease throughout the callback. This is not Unicode normalization,
 * numeric coercion, or SPARQL value equality. An IRI contains a local name.
 */
public final class ValueStoreRecordVisitor {
	private ValueStoreRecordVisitor() {
	}

	@FunctionalInterface
	public interface Visitor {
		void accept(Record record);
	}

	/** Owned primitive header; it can safely be retained independently of the callback and native owner. */
	public record Header(ValueStoreRecordView.Kind kind, long referenceId, int languageBytes, int direction) {
	}

	/** Reusable result holder for batch metadata consumers; no per-row Value, String, or record array. */
	public static final class HeaderReader implements Visitor {
		private ValueStoreRecordView.Kind kind;
		private long referenceId;
		private int languageBytes, direction;

		@Override
		public void accept(Record record) {
			kind = record.kind();
			referenceId = record.referenceId();
			languageBytes = record.languageByteLength();
			direction = record.direction();
		}

		public ValueStoreRecordView.Kind kind() {
			return kind;
		}

		public long referenceId() {
			return referenceId;
		}

		public Header header() {
			return kind == null ? null : new Header(kind, referenceId, languageBytes, direction);
		}
	}

	public static boolean visitOwnedBytes(byte[] bytes, Visitor visitor) {
		return visit(RecordByteAccess.raw(MemorySegment.ofArray(Objects.requireNonNull(bytes))), visitor);
	}

	static boolean visit(RecordByteAccess bytes, Visitor visitor) {
		Objects.requireNonNull(visitor);
		Record record;
		try {
			record = new Record(bytes);
		} catch (UnsupportedRecord unsupported) {
			return false;
		}
		try {
			visitor.accept(record);
			return true;
		} finally {
			record.bytes = null;
		}
	}

	private static final class UnsupportedRecord extends RuntimeException {
		private static final long serialVersionUID = 1;

		@Override
		public synchronized Throwable fillInStackTrace() {
			return this;
		}
	}

	public static final class Record {
		private RecordByteAccess bytes;
		private final ValueStoreRecordView.Kind kind;
		private final long reference;
		private final int lexical, language, languageLength, direction;

		private Record(RecordByteAccess bytes) {
			this.bytes = bytes;
			try {
				kind = switch (bytes.byteAt(0)) {
				case 0 -> ValueStoreRecordView.Kind.IRI;
				case 1 -> ValueStoreRecordView.Kind.LITERAL;
				case 2 -> ValueStoreRecordView.Kind.BNODE;
				case 4 -> ValueStoreRecordView.Kind.NAMESPACE;
				default -> throw new UnsupportedRecord();
				};
				int p = 1;
				long id = 0;
				if (kind == ValueStoreRecordView.Kind.IRI || kind == ValueStoreRecordView.Kind.LITERAL) {
					int first = bytes.byteAt(p++);
					if (first <= 240)
						id = first;
					else if (first <= 248)
						id = 240L + 256L * (first - 241) + bytes.byteAt(p++);
					else if (first == 249) {
						id = 2288L + 256L * bytes.byteAt(p++) + bytes.byteAt(p++);
					} else
						for (int n = first - 247; n > 0; n--)
							id = (id << 8) | bytes.byteAt(p++);
				}
				reference = id;
				if (kind == ValueStoreRecordView.Kind.LITERAL) {
					int flags = bytes.byteAt(p++);
					languageLength = flags & 63;
					direction = flags >>> 6;
					language = p;
					if (direction == 3)
						throw new UnsupportedRecord();
					p += languageLength;
					if (languageLength != 0)
						bytes.byteAt(p - 1); // validate only the required bounded header
				} else {
					language = p;
					languageLength = 0;
					direction = 0;
				}
				lexical = p;
			} catch (IndexOutOfBoundsException truncated) {
				throw new UnsupportedRecord();
			}
		}

		private RecordByteAccess checked() {
			if (bytes == null)
				throw new IllegalStateException("typed record escaped its owning callback");
			return bytes;
		}

		public ValueStoreRecordView.Kind kind() {
			checked();
			return kind;
		}

		public long referenceId() {
			checked();
			return reference;
		}

		public long datatypeId() {
			checked();
			if (kind != ValueStoreRecordView.Kind.LITERAL)
				throw new IllegalStateException("not a literal");
			return reference;
		}

		public long namespaceId() {
			checked();
			if (kind != ValueStoreRecordView.Kind.IRI)
				throw new IllegalStateException("not an IRI");
			return reference;
		}

		public int direction() {
			checked();
			return direction;
		}

		public int languageByteLength() {
			checked();
			return languageLength;
		}

		public int lexicalByteLength() {
			return checked().length() - lexical;
		}

		public void copyLanguage(byte[] target, int offset) {
			checked().copy(language, target, offset, languageLength);
		}

		public void copyLexical(int from, byte[] target, int offset, int count) {
			RecordByteAccess source = checked();
			Objects.checkFromIndexSize(from, count, source.length() - lexical);
			source.copy(lexical + from, target, offset, count);
		}

		/** Stops at the first unequal byte. Does not ask a token source to decode/count the rest of its label. */
		public boolean lexicalStartsWith(byte[] prefix) {
			Objects.requireNonNull(prefix);
			RecordByteAccess source = checked();
			for (int i = 0; i < prefix.length; i++) {
				try {
					if (source.byteAt(Math.addExact(lexical, i)) != (prefix[i] & 255))
						return false;
				} catch (IndexOutOfBoundsException end) {
					return false;
				}
			}
			return true;
		}

		public boolean lexicalEquals(byte[] value) {
			Objects.requireNonNull(value);
			return lexicalByteLength() == value.length && lexicalStartsWith(value);
		}
	}
}
