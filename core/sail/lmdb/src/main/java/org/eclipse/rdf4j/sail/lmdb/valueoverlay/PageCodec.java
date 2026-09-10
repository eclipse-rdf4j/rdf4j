/* SPDX-License-Identifier: EPL-2.0 */
package org.eclipse.rdf4j.sail.lmdb.valueoverlay;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Immutable native page builder and bounded-random-access decoder. */
public final class PageCodec {
	public static final int MAGIC = 0x31435646; // "FVC1" in little endian
	private static final int HEADER_BYTES = 32;
	private static final int CHECKPOINT_GROUP = 16;
	private static final int CODEC_MASK = 0x03;
	private static final int FLAG_LOB = 0x04;

	public enum Mode {
		RAW(0),
		FRONT(1),
		TOKEN(2),
		VECTOR(3);

		final int code;

		Mode(int code) {
			this.code = code;
		}

		static Mode fromCode(int code) {
			return switch (code) {
			case 0 -> RAW;
			case 1 -> FRONT;
			case 2 -> TOKEN;
			case 3 -> VECTOR;
			default -> throw new IllegalStateException("unknown page codec " + code);
			};
		}
	}

	public record SealedPage(byte[] pageBytes, Mode mode, long uncompressedRecordBytes,
			long encodedPageBytes) {
	}

	public record DecodedRecord(byte[] bytes, boolean large) {
	}

	private record Representation(Mode mode, byte[][] records, byte[] dictionary, int restart) {
	}

	private PageCodec() {
	}

	public static SealedPage seal(PhysicalRecord[] physical, int count, int restart,
			int minimumSaving, boolean tokenCompression, int tokenCandidateLimit, int tokenLimit) {
		return seal(physical, count, restart, minimumSaving, tokenCompression, tokenCandidateLimit, tokenLimit, false);
	}

	public static SealedPage seal(PhysicalRecord[] physical, int count, int restart,
			int minimumSaving, boolean tokenCompression, int tokenCandidateLimit, int tokenLimit,
			boolean vectorCompression) {
		return seal(physical, count, restart, minimumSaving, tokenCompression, tokenCandidateLimit,
				tokenLimit, vectorCompression, false);
	}

	public static SealedPage seal(PhysicalRecord[] physical, int count, int restart,
			int minimumSaving, boolean tokenCompression, int tokenCandidateLimit, int tokenLimit,
			boolean vectorCompression, boolean adaptiveTokenTraining) {
		if (count <= 0 || count > physical.length || count > 32768)
			throw new IllegalArgumentException("invalid page count");
		if (restart <= 0 || (restart & (restart - 1)) != 0 || minimumSaving < 0)
			throw new IllegalArgumentException("invalid page policy");

		byte[][] rawRecords = new byte[count][];
		boolean[] large = new boolean[count];
		long rawPayload = 0;
		for (int i = 0; i < count; i++) {
			PhysicalRecord record = physical[i];
			if (record == null)
				throw new IllegalStateException("missing page record " + i);
			rawRecords[i] = record.bytes();
			large[i] = record.large();
			rawPayload = Math.addExact(rawPayload, record.bytes().length);
		}

		Representation best = new Representation(Mode.RAW, rawRecords, new byte[0], 0);
		int bestBytes = estimatedPageBytes(best, large);

		Representation front = frontCode(rawRecords, restart);
		int frontBytes = estimatedPageBytes(front, large);
		if (frontBytes <= bestBytes - minimumSaving) {
			best = front;
			bestBytes = frontBytes;
		}

		if (tokenCompression) {
			Representation token = tokenCode(rawRecords, large, tokenCandidateLimit, tokenLimit, adaptiveTokenTraining);
			if (token != null) {
				int tokenBytes = estimatedPageBytes(token, large);
				if (tokenBytes <= bestBytes - minimumSaving) {
					best = token;
					bestBytes = tokenBytes;
				}
			}
		}

		if (vectorCompression && !any(large)) {
			byte[] vector = VectorPageCodec.encode(physical, count, true);
			byte[] byteVector = VectorPageCodec.encode(physical, count, false);
			if (vector == null || byteVector != null && byteVector.length < vector.length)
				vector = byteVector;
			if (vector != null && vector.length <= bestBytes - minimumSaving)
				return new SealedPage(vector, Mode.VECTOR, rawPayload, vector.length);
		}
		byte[] page = materialize(best, large, bestBytes);
		return new SealedPage(page, best.mode(), rawPayload, page.length);
	}

	public static DecodedRecord read(NativeSlabAllocator allocator, long pageHandle, int slot) {
		MemorySegment segment = allocator.segment(pageHandle);
		long base = NativeSlabAllocator.offset(pageHandle);
		int magic = segment.get(FfmAccess.INT_LE, base);
		if (magic == VectorPageCodec.MAGIC)
			return VectorPageCodec.read(allocator, pageHandle, slot);
		if (magic != MAGIC)
			throw new IllegalStateException("corrupt cache page magic");
		int count = Short.toUnsignedInt(segment.get(FfmAccess.SHORT_LE, base + 4));
		if (slot < 0 || slot >= count)
			throw new IllegalStateException("page slot out of range");
		int bitWidth = Byte.toUnsignedInt(segment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, base + 6));
		int flags = Byte.toUnsignedInt(segment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, base + 7));
		int restart = Short.toUnsignedInt(segment.get(FfmAccess.SHORT_LE, base + 8));
		int dictionaryBytes = Short.toUnsignedInt(segment.get(FfmAccess.SHORT_LE, base + 10));
		int checkpointCount = segment.get(FfmAccess.INT_LE, base + 12);
		int packedOffset = segment.get(FfmAccess.INT_LE, base + 16);
		int payloadOffset = segment.get(FfmAccess.INT_LE, base + 20);
		int payloadBytes = segment.get(FfmAccess.INT_LE, base + 24);
		int totalBytes = segment.get(FfmAccess.INT_LE, base + 28);
		boolean hasLob = (flags & FLAG_LOB) != 0;
		Mode mode = Mode.fromCode(flags & CODEC_MASK);
		validateHeader(count, bitWidth, flags, mode, restart, dictionaryBytes, checkpointCount, packedOffset,
				payloadOffset, payloadBytes, totalBytes, segment.byteSize() - base);

		byte[] physical = switch (mode) {
		case RAW -> encodedRecord(segment, base, slot, bitWidth, packedOffset, payloadOffset, payloadBytes);
		case FRONT -> decodeFront(segment, base, slot, restart, bitWidth, packedOffset, payloadOffset,
				payloadBytes);
		case TOKEN -> decodeToken(segment, base, slot, count, dictionaryBytes, bitWidth,
				packedOffset, payloadOffset, payloadBytes, checkpointCount, hasLob);
		case VECTOR -> throw new IllegalStateException("vector codec requires vector page header");
		};
		boolean large = hasLob && bitmapBit(segment, base, checkpointCount, slot);
		return new DecodedRecord(physical, large);
	}

	/** Physical lookup without reconstructing a record. The caller must retain the allocator's owner. */
	static RecordByteAccess openRecord(NativeSlabAllocator allocator, long handle, int slot) {
		MemorySegment segment = allocator.segment(handle);
		long base = NativeSlabAllocator.offset(handle);
		int magic = segment.get(FfmAccess.INT_LE, base);
		if (magic == VectorPageCodec.MAGIC)
			return VectorPageCodec.openRecord(allocator, handle, slot);
		if (magic != MAGIC)
			throw new IllegalStateException("corrupt cache page magic");
		int count = Short.toUnsignedInt(segment.get(FfmAccess.SHORT_LE, base + 4));
		if (slot < 0 || slot >= count)
			throw new IllegalStateException("page slot out of range");
		int bits = segment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, base + 6) & 255;
		int flags = segment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, base + 7) & 255;
		int restart = Short.toUnsignedInt(segment.get(FfmAccess.SHORT_LE, base + 8));
		int dictBytes = Short.toUnsignedInt(segment.get(FfmAccess.SHORT_LE, base + 10));
		int checkpoints = segment.get(FfmAccess.INT_LE, base + 12);
		int packed = segment.get(FfmAccess.INT_LE, base + 16), payload = segment.get(FfmAccess.INT_LE, base + 20);
		int payloadBytes = segment.get(FfmAccess.INT_LE, base + 24), total = segment.get(FfmAccess.INT_LE, base + 28);
		Mode mode = Mode.fromCode(flags & CODEC_MASK);
		validateHeader(count, bits, flags, mode, restart, dictBytes, checkpoints, packed, payload,
				payloadBytes, total, segment.byteSize() - base);
		MemorySegment page = segment.asSlice(base, total);
		if (mode == Mode.RAW || (flags & FLAG_LOB) != 0 && bitmapBit(page, 0, checkpoints, slot))
			return RecordByteAccess.raw(encodedSlice(page, slot, bits, packed, payload, payloadBytes));
		if (mode == Mode.FRONT) {
			int first = slot - slot % restart, n = slot - first + 1;
			MemorySegment[] suffix = new MemorySegment[n];
			int[] prefixes = new int[n];
			suffix[0] = encodedSlice(page, first, bits, packed, payload, payloadBytes);
			int size = Math.toIntExact(suffix[0].byteSize());
			for (int i = 1; i < n; i++) {
				MemorySegment encoded = encodedSlice(page, first + i, bits, packed, payload, payloadBytes);
				long value = 0;
				int at = 0;
				for (;;) {
					if (at >= encoded.byteSize() || at == 5)
						throw new IllegalStateException("corrupt front prefix");
					int b = encoded.get(java.lang.foreign.ValueLayout.JAVA_BYTE, at) & 255;
					value |= (long) (b & 127) << (7 * at++);
					if ((b & 128) == 0)
						break;
				}
				if (value > size)
					throw new IllegalStateException("front prefix exceeds prior record");
				prefixes[i] = (int) value;
				suffix[i] = encoded.asSlice(at);
				size = Math.addExact(prefixes[i], Math.toIntExact(suffix[i].byteSize()));
			}
			final int length = size;
			return new RecordByteAccess() {
				public int length() {
					return length;
				}

				public int byteAt(int index) {
					java.util.Objects.checkIndex(index, length);
					int i = n - 1;
					while (i > 0 && index < prefixes[i])
						i--;
					return suffix[i].get(java.lang.foreign.ValueLayout.JAVA_BYTE, index - prefixes[i]) & 255;
				}
			};
		}
		if (mode != Mode.TOKEN)
			throw new IllegalStateException("vector page has wrong header");
		long dictionary = HEADER_BYTES + checkpoints * 4L + ((flags & FLAG_LOB) == 0 ? 0 : (count + 7L) >>> 3);
		return new TokenAccess(encodedSlice(page, slot, bits, packed, payload, payloadBytes),
				page.asSlice(dictionary, dictBytes));
	}

	private static MemorySegment encodedSlice(MemorySegment page, int slot, int bits, int packed,
			int payload, int payloadBytes) {
		int group = slot / CHECKPOINT_GROUP;
		long relative = page.get(FfmAccess.INT_LE, HEADER_BYTES + group * 4L);
		for (int i = group * CHECKPOINT_GROUP; i < slot; i++)
			relative += packedLength(page, 0, packed, bits, i);
		int length = packedLength(page, 0, packed, bits, slot);
		if (relative < 0 || length < 0 || relative > payloadBytes - length)
			throw new IllegalStateException("corrupt record boundary");
		return page.asSlice(payload + relative, length);
	}

	/** Token commands are scanned as needed; token payload outside the requested region is never expanded. */
	private static final class TokenAccess implements RecordByteAccess {
		private final MemorySegment encoded, dictionary;
		private final int tokens, tableEnd;
		private int commandPosition, runStart, runEnd, knownLength = -1;
		private MemorySegment run;

		TokenAccess(MemorySegment encoded, MemorySegment dictionary) {
			this.encoded = encoded;
			this.dictionary = dictionary;
			if (dictionary.byteSize() < 1)
				throw new IllegalStateException("empty token dictionary");
			tokens = dictionary.get(java.lang.foreign.ValueLayout.JAVA_BYTE, 0) & 255;
			tableEnd = 1 + (tokens + 1) * 2;
			if (tokens > 64 || tableEnd > dictionary.byteSize())
				throw new IllegalStateException("corrupt token dictionary");
		}

		private MemorySegment token(int command) {
			int start = Short.toUnsignedInt(dictionary.get(FfmAccess.SHORT_LE, 1 + 2L * command));
			int end = Short.toUnsignedInt(dictionary.get(FfmAccess.SHORT_LE, 3 + 2L * command));
			if (end < start || tableEnd + (long) end > dictionary.byteSize())
				throw new IllegalStateException("corrupt token offset");
			return dictionary.asSlice(tableEnd + start, end - start);
		}

		private boolean advance() {
			if (commandPosition == encoded.byteSize()) {
				knownLength = runEnd;
				return false;
			}
			int command = encoded.get(java.lang.foreign.ValueLayout.JAVA_BYTE, commandPosition++) & 255;
			if (command < tokens)
				run = token(command);
			else if (command >= 64 && command <= 191) {
				int length = command - 63;
				if (commandPosition > encoded.byteSize() - length)
					throw new IllegalStateException("corrupt token literal");
				run = encoded.asSlice(commandPosition, length);
				commandPosition += length;
			} else
				throw new IllegalStateException("corrupt token command");
			runStart = runEnd;
			runEnd = Math.addExact(runEnd, Math.toIntExact(run.byteSize()));
			return true;
		}

		public int byteAt(int index) {
			if (index < 0 || knownLength >= 0 && index >= knownLength)
				throw new IndexOutOfBoundsException(index);
			if (index < runStart) {
				commandPosition = runStart = runEnd = 0;
				run = null;
			}
			while (index >= runEnd)
				if (!advance())
					throw new IndexOutOfBoundsException(index);
			return run.get(java.lang.foreign.ValueLayout.JAVA_BYTE, index - runStart) & 255;
		}

		public int length() {
			if (knownLength < 0)
				while (advance()) {
					/* count commands, not decoded bytes */ }
			return knownLength;
		}
	}

	private static void validateHeader(int count, int bitWidth, int flags, Mode mode, int restart,
			int dictionaryBytes, int checkpointCount, int packedOffset, int payloadOffset, int payloadBytes,
			int totalBytes, long available) {
		if (count <= 0 || count > 32_768 || bitWidth <= 0 || bitWidth > 31
				|| (flags & ~(CODEC_MASK | FLAG_LOB)) != 0
				|| checkpointCount != (count + CHECKPOINT_GROUP - 1) / CHECKPOINT_GROUP
				|| dictionaryBytes < 0 || payloadBytes < 0 || totalBytes < HEADER_BYTES
				|| totalBytes > available) {
			throw new IllegalStateException("corrupt cache page header");
		}
		if ((mode == Mode.FRONT && (restart <= 0 || (restart & (restart - 1)) != 0))
				|| (mode != Mode.FRONT && restart != 0)
				|| (mode == Mode.TOKEN && dictionaryBytes == 0)
				|| (mode != Mode.TOKEN && dictionaryBytes != 0)) {
			throw new IllegalStateException("corrupt cache page codec metadata");
		}
		long bitmapBytes = (flags & FLAG_LOB) != 0 ? (count + 7L) >>> 3 : 0;
		long dictionaryEnd = HEADER_BYTES + checkpointCount * 4L + bitmapBytes + dictionaryBytes;
		long packedBytes = ((long) count * bitWidth + 7L) >>> 3;
		long minimumPayloadOffset = (long) packedOffset + packedBytes + Long.BYTES;
		long payloadEnd = (long) payloadOffset + payloadBytes;
		if (packedOffset < dictionaryEnd || (packedOffset & 7) != 0
				|| payloadOffset < minimumPayloadOffset || (payloadOffset & 7) != 0
				|| payloadEnd != totalBytes) {
			throw new IllegalStateException("corrupt cache page offsets");
		}
	}

	private static Representation frontCode(byte[][] raw, int restart) {
		byte[][] encoded = new byte[raw.length][];
		for (int i = 0; i < raw.length; i++) {
			if (i % restart == 0) {
				encoded[i] = raw[i];
				continue;
			}
			byte[] previous = raw[i - 1];
			byte[] current = raw[i];
			int prefix = commonPrefix(previous, current);
			ByteArrayBuilder builder = new ByteArrayBuilder(current.length - prefix + 5);
			builder.writeVarUInt(prefix);
			builder.writeBytes(current, prefix, current.length - prefix);
			encoded[i] = builder.toByteArray();
		}
		return new Representation(Mode.FRONT, encoded, new byte[0], restart);
	}

	private static Representation tokenCode(byte[][] raw, boolean[] large, int candidateLimit, int tokenLimit,
			boolean adaptiveTraining) {
		List<Token> tokens;
		if (adaptiveTraining) {
			tokens = learnUncoveredTokens(raw, large, candidateLimit, tokenLimit);
		} else {
			CandidateCollector collector = new CandidateCollector(candidateLimit);
			for (int i = 0; i < raw.length; i++) {
				if (!large[i] && raw[i].length >= 4)
					collector.addRecord(raw[i]);
			}
			tokens = collector.best(tokenLimit);
		}
		if (tokens.isEmpty())
			return null;

		@SuppressWarnings("unchecked")
		List<Token>[] byFirst = (List<Token>[]) new List<?>[256];
		for (int id = 0; id < tokens.size(); id++) {
			Token token = tokens.get(id).withId(id);
			tokens.set(id, token);
			int first = token.bytes[0] & 0xff;
			if (byFirst[first] == null)
				byFirst[first] = new ArrayList<>();
			byFirst[first].add(token);
		}
		for (List<Token> bucket : byFirst) {
			if (bucket != null)
				bucket.sort(Comparator.comparingInt((Token t) -> t.bytes.length).reversed());
		}

		byte[][] encoded = new byte[raw.length][];
		for (int i = 0; i < raw.length; i++) {
			encoded[i] = large[i] ? literalOnly(raw[i]) : encodeTokens(raw[i], byFirst);
		}
		byte[] dictionary = encodeDictionary(tokens);
		if (dictionary.length > 0xffff)
			return null;
		return new Representation(Mode.TOKEN, encoded, dictionary, 0);
	}

	/**
	 * Re-estimate marginal savings on bounded, evenly spaced samples after excluding already-covered byte spans. Static
	 * occurrence scores otherwise spend most token slots on mutually overlapping copies of one phrase. This is not
	 * FSST: training and encoding are this page codec's own greedy lexical scheme.
	 */
	private static List<Token> learnUncoveredTokens(byte[][] raw, boolean[] large, int candidateLimit, int tokenLimit) {
		List<Token> selected = new ArrayList<>();
		int samples = Math.min(16, raw.length);
		for (int round = 0; round < Math.min(tokenLimit, 16); round++) {
			CandidateCollector candidates = new CandidateCollector(candidateLimit);
			for (int sample = 0; sample < samples; sample++) {
				int index = (int) ((long) sample * raw.length / samples);
				if (large[index])
					continue;
				byte[] bytes = raw[index];
				int start = 0, position = 0;
				while (position < bytes.length) {
					Token match = null;
					for (Token token : selected) {
						if (token.bytes[0] == bytes[position] && token.bytes.length <= bytes.length - position
								&& (match == null || token.bytes.length > match.bytes.length)
								&& regionEquals(bytes, position, token.bytes))
							match = token;
					}
					if (match == null)
						position++;
					else {
						if (position - start >= 3)
							candidates.addRecord(Arrays.copyOfRange(bytes, start, position));
						position += match.bytes.length;
						start = position;
					}
				}
				if (position - start >= 3)
					candidates.addRecord(Arrays.copyOfRange(bytes, start, position));
			}
			List<Token> best = candidates.best(1);
			if (best.isEmpty())
				break;
			selected.add(best.get(0));
		}
		return selected;
	}

	private static byte[] encodeTokens(byte[] source, List<Token>[] byFirst) {
		ByteArrayBuilder out = new ByteArrayBuilder(source.length);
		int literalStart = 0;
		int position = 0;
		while (position < source.length) {
			Token match = longestMatch(source, position, byFirst[source[position] & 0xff]);
			if (match == null) {
				position++;
				if (position - literalStart == 128) {
					flushLiterals(out, source, literalStart, 128);
					literalStart = position;
				}
			} else {
				if (position > literalStart)
					flushLiterals(out, source, literalStart, position - literalStart);
				out.writeByte(match.id);
				position += match.bytes.length;
				literalStart = position;
			}
		}
		if (position > literalStart)
			flushLiterals(out, source, literalStart, position - literalStart);
		return out.toByteArray();
	}

	private static byte[] literalOnly(byte[] source) {
		ByteArrayBuilder out = new ByteArrayBuilder(source.length + (source.length + 127) / 128);
		for (int offset = 0; offset < source.length; offset += 128) {
			int length = Math.min(128, source.length - offset);
			flushLiterals(out, source, offset, length);
		}
		return out.toByteArray();
	}

	private static void flushLiterals(ByteArrayBuilder out, byte[] source, int offset, int length) {
		int remaining = length;
		while (remaining > 0) {
			int run = Math.min(128, remaining);
			out.writeByte(64 + run - 1);
			out.writeBytes(source, offset, run);
			offset += run;
			remaining -= run;
		}
	}

	private static Token longestMatch(byte[] source, int position, List<Token> candidates) {
		if (candidates == null)
			return null;
		for (Token candidate : candidates) {
			if (candidate.bytes.length <= source.length - position
					&& regionEquals(source, position, candidate.bytes)) {
				return candidate;
			}
		}
		return null;
	}

	private static byte[] encodeDictionary(List<Token> tokens) {
		int payload = tokens.stream().mapToInt(t -> t.bytes.length).sum();
		ByteArrayBuilder out = new ByteArrayBuilder(1 + (tokens.size() + 1) * 2 + payload);
		out.writeByte(tokens.size());
		int offset = 0;
		writeShortLE(out, 0);
		for (Token token : tokens) {
			offset += token.bytes.length;
			writeShortLE(out, offset);
		}
		for (Token token : tokens)
			out.writeBytes(token.bytes);
		return out.toByteArray();
	}

	private static void writeShortLE(ByteArrayBuilder out, int value) {
		out.writeByte(value);
		out.writeByte(value >>> 8);
	}

	private static int estimatedPageBytes(Representation representation, boolean[] large) {
		int count = representation.records.length;
		int checkpoints = (count + CHECKPOINT_GROUP - 1) / CHECKPOINT_GROUP;
		int bitmap = any(large) ? (count + 7) >>> 3 : 0;
		int maxLength = 0;
		long payload = 0;
		for (byte[] record : representation.records) {
			maxLength = Math.max(maxLength, record.length);
			payload += record.length;
		}
		if (payload > Integer.MAX_VALUE)
			throw new IllegalArgumentException("page payload exceeds Java array limit");
		int bits = Math.max(1, 32 - Integer.numberOfLeadingZeros(maxLength));
		long packed = ((long) count * bits + 7) >>> 3;
		long packedOffset = FfmAccess.alignUp((long) HEADER_BYTES + checkpoints * 4L + bitmap
				+ representation.dictionary.length, 8);
		long payloadOffset = FfmAccess.alignUp(packedOffset + packed + 8, 8);
		long total = payloadOffset + payload;
		if (total > Integer.MAX_VALUE)
			throw new IllegalArgumentException("page exceeds Java array limit");
		return (int) total;
	}

	private static byte[] materialize(Representation representation, boolean[] large, int totalBytes) {
		int count = representation.records.length;
		int checkpointCount = (count + CHECKPOINT_GROUP - 1) / CHECKPOINT_GROUP;
		boolean hasLob = any(large);
		int bitmapBytes = hasLob ? (count + 7) >>> 3 : 0;
		int maxLength = 0;
		int payloadBytes = 0;
		for (byte[] record : representation.records) {
			maxLength = Math.max(maxLength, record.length);
			payloadBytes = Math.addExact(payloadBytes, record.length);
		}
		int bits = Math.max(1, 32 - Integer.numberOfLeadingZeros(maxLength));
		int packedBytes = Math.toIntExact(((long) count * bits + 7) >>> 3);
		int dictionaryOffset = HEADER_BYTES + checkpointCount * 4 + bitmapBytes;
		int packedOffset = Math.toIntExact(FfmAccess.alignUp(dictionaryOffset + representation.dictionary.length, 8));
		int payloadOffset = Math.toIntExact(FfmAccess.alignUp((long) packedOffset + packedBytes + 8, 8));
		if (payloadOffset + payloadBytes != totalBytes)
			throw new AssertionError("page size mismatch");

		byte[] page = new byte[totalBytes];
		putInt(page, 0, MAGIC);
		putShort(page, 4, count);
		page[6] = (byte) bits;
		page[7] = (byte) (representation.mode.code | (hasLob ? FLAG_LOB : 0));
		putShort(page, 8, representation.restart);
		putShort(page, 10, representation.dictionary.length);
		putInt(page, 12, checkpointCount);
		putInt(page, 16, packedOffset);
		putInt(page, 20, payloadOffset);
		putInt(page, 24, payloadBytes);
		putInt(page, 28, totalBytes);

		int cumulative = 0;
		for (int group = 0; group < checkpointCount; group++) {
			putInt(page, HEADER_BYTES + group * 4, cumulative);
			int first = group * CHECKPOINT_GROUP;
			int last = Math.min(count, first + CHECKPOINT_GROUP);
			for (int i = first; i < last; i++)
				cumulative += representation.records[i].length;
		}
		if (hasLob) {
			int bitmapOffset = HEADER_BYTES + checkpointCount * 4;
			for (int i = 0; i < count; i++) {
				if (large[i])
					page[bitmapOffset + (i >>> 3)] |= (byte) (1 << (i & 7));
			}
		}
		System.arraycopy(representation.dictionary, 0, page, dictionaryOffset, representation.dictionary.length);
		for (int i = 0; i < count; i++) {
			pack(page, packedOffset, (long) i * bits, bits, representation.records[i].length);
		}
		int payloadCursor = payloadOffset;
		for (byte[] record : representation.records) {
			System.arraycopy(record, 0, page, payloadCursor, record.length);
			payloadCursor += record.length;
		}
		return page;
	}

	private static byte[] encodedRecord(MemorySegment segment, long base, int slot, int bits,
			int packedOffset, int payloadOffset, int payloadBytes) {
		int group = slot / CHECKPOINT_GROUP;
		int first = group * CHECKPOINT_GROUP;
		int relative = segment.get(FfmAccess.INT_LE, base + HEADER_BYTES + group * 4L);
		for (int i = first; i < slot; i++)
			relative += packedLength(segment, base, packedOffset, bits, i);
		int length = packedLength(segment, base, packedOffset, bits, slot);
		if (relative < 0 || length < 0 || relative > payloadBytes - length) {
			throw new IllegalStateException("corrupt cache page record boundary");
		}
		return segment.asSlice(base + payloadOffset + relative, length)
				.toArray(java.lang.foreign.ValueLayout.JAVA_BYTE);
	}

	private static byte[] decodeFront(MemorySegment segment, long base, int slot, int restart, int bits,
			int packedOffset, int payloadOffset, int payloadBytes) {
		if (restart <= 0)
			throw new IllegalStateException("corrupt front-coded page restart");
		int first = slot - (slot % restart);
		byte[] previous = encodedRecord(segment, base, first, bits, packedOffset, payloadOffset, payloadBytes);
		for (int i = first + 1; i <= slot; i++) {
			byte[] encoded = encodedRecord(segment, base, i, bits, packedOffset, payloadOffset, payloadBytes);
			ByteCursor cursor = new ByteCursor(encoded);
			int prefix = cursor.readVarUInt();
			if (prefix > previous.length)
				throw new IllegalStateException("corrupt front-coded prefix");
			byte[] current = new byte[prefix + cursor.remaining()];
			System.arraycopy(previous, 0, current, 0, prefix);
			byte[] suffix = cursor.readRemaining();
			System.arraycopy(suffix, 0, current, prefix, suffix.length);
			previous = current;
		}
		return previous;
	}

	private static byte[] decodeToken(MemorySegment segment, long base, int slot, int count,
			int dictionaryBytes, int bits, int packedOffset, int payloadOffset, int payloadBytes,
			int checkpointCount, boolean hasLob) {
		byte[] encoded = encodedRecord(segment, base, slot, bits, packedOffset, payloadOffset, payloadBytes);
		int bitmapBytes = hasLob ? (count + 7) >>> 3 : 0;
		long dictionaryOffset = base + HEADER_BYTES + checkpointCount * 4L + bitmapBytes;
		if (dictionaryBytes <= 0)
			throw new IllegalStateException("token page lacks dictionary");
		int tokenCount = Byte.toUnsignedInt(segment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, dictionaryOffset));
		if (tokenCount > 64 || 1L + (tokenCount + 1L) * 2 > dictionaryBytes) {
			throw new IllegalStateException("corrupt token dictionary");
		}
		long offsetsBase = dictionaryOffset + 1;
		long tokenPayload = offsetsBase + (tokenCount + 1L) * 2;
		// First count exactly, then copy directly into one caller-owned result. No temporary array per token.
		int resultBytes = 0, position = 0;
		while (position < encoded.length) {
			int command = encoded[position++] & 255;
			if (command < tokenCount) {
				int start = Short.toUnsignedInt(segment.get(FfmAccess.SHORT_LE, offsetsBase + command * 2L));
				int end = Short.toUnsignedInt(segment.get(FfmAccess.SHORT_LE, offsetsBase + (command + 1L) * 2));
				if (end < start || tokenPayload + end > dictionaryOffset + dictionaryBytes)
					throw new IllegalStateException("corrupt token offset");
				resultBytes = Math.addExact(resultBytes, end - start);
			} else if (command >= 64 && command <= 191) {
				int length = command - 64 + 1;
				if (position > encoded.length - length)
					throw new IllegalStateException("corrupt token literal run");
				resultBytes = Math.addExact(resultBytes, length);
				position += length;
			} else
				throw new IllegalStateException("corrupt token command " + command);
		}
		byte[] out = new byte[resultBytes];
		MemorySegment target = MemorySegment.ofArray(out);
		position = 0;
		int written = 0;
		while (position < encoded.length) {
			int command = encoded[position++] & 255;
			if (command < tokenCount) {
				int start = Short.toUnsignedInt(segment.get(FfmAccess.SHORT_LE, offsetsBase + command * 2L));
				int end = Short.toUnsignedInt(segment.get(FfmAccess.SHORT_LE, offsetsBase + (command + 1L) * 2));
				MemorySegment.copy(segment, tokenPayload + start, target, written, end - start);
				written += end - start;
			} else {
				int length = command - 64 + 1;
				System.arraycopy(encoded, position, out, written, length);
				position += length;
				written += length;
			}
		}
		return out;
	}

	private static boolean bitmapBit(MemorySegment segment, long base, int checkpointCount, int slot) {
		long offset = base + HEADER_BYTES + checkpointCount * 4L + (slot >>> 3);
		int value = Byte.toUnsignedInt(segment.get(java.lang.foreign.ValueLayout.JAVA_BYTE, offset));
		return (value & (1 << (slot & 7))) != 0;
	}

	private static int packedLength(MemorySegment segment, long base, int packedOffset, int bits, int index) {
		long bit = (long) index * bits;
		long word = segment.get(FfmAccess.LONG_LE, base + packedOffset + (bit >>> 3));
		long mask = (1L << bits) - 1;
		return (int) ((word >>> (bit & 7)) & mask);
	}

	private static void pack(byte[] target, int offset, long bitPosition, int bits, int value) {
		int bytePosition = offset + (int) (bitPosition >>> 3);
		int shift = (int) bitPosition & 7;
		long word = (long) value << shift;
		int bytes = (shift + bits + 7) >>> 3;
		for (int i = 0; i < bytes; i++) {
			target[bytePosition + i] |= (byte) (word >>> (i * 8));
		}
	}

	private static int commonPrefix(byte[] left, byte[] right) {
		int limit = Math.min(left.length, right.length);
		int i = 0;
		while (i < limit && left[i] == right[i])
			i++;
		return i;
	}

	private static boolean regionEquals(byte[] source, int offset, byte[] token) {
		for (int i = 0; i < token.length; i++)
			if (source[offset + i] != token[i])
				return false;
		return true;
	}

	private static boolean any(boolean[] values) {
		for (boolean value : values)
			if (value)
				return true;
		return false;
	}

	private static void putShort(byte[] target, int offset, int value) {
		target[offset] = (byte) value;
		target[offset + 1] = (byte) (value >>> 8);
	}

	private static void putInt(byte[] target, int offset, int value) {
		target[offset] = (byte) value;
		target[offset + 1] = (byte) (value >>> 8);
		target[offset + 2] = (byte) (value >>> 16);
		target[offset + 3] = (byte) (value >>> 24);
	}

	private static final class Token {
		final byte[] bytes;
		final int frequency;
		final long gain;
		final int id;

		Token(byte[] bytes, int frequency, int id) {
			this.bytes = bytes;
			this.frequency = frequency;
			this.gain = (long) frequency * (bytes.length - 1) - bytes.length - 2;
			this.id = id;
		}

		Token withId(int id) {
			return new Token(bytes, frequency, id);
		}
	}

	private static final class CandidateCollector {
		private static final int[] LENGTHS = { 4, 6, 8, 12, 16, 24 };
		private final int limit;
		private int count;
		private final Map<Long, List<MutableCandidate>> candidates = new HashMap<>();

		CandidateCollector(int limit) {
			this.limit = limit;
		}

		void addRecord(byte[] record) {
			int step = Math.max(1, record.length / 96);
			for (int position = 0; position < record.length - 3; position += step) {
				for (int length : LENGTHS) {
					if (position + length <= record.length)
						add(record, position, length);
				}
			}
			int start = 0;
			for (int i = 0; i <= record.length; i++) {
				if (i == record.length || isDelimiter(record[i])) {
					int length = i - start;
					if (length >= 3)
						add(record, start, Math.min(24, length));
					start = i + 1;
				}
			}
		}

		private void add(byte[] source, int offset, int length) {
			long hash = hash(source, offset, length);
			List<MutableCandidate> bucket = candidates.get(hash);
			if (bucket != null) {
				for (MutableCandidate candidate : bucket) {
					if (candidate.bytes.length == length && regionEquals(source, offset, candidate.bytes)) {
						candidate.frequency++;
						return;
					}
				}
			}
			if (count >= limit)
				return;
			byte[] bytes = Arrays.copyOfRange(source, offset, offset + length);
			if (bucket == null) {
				bucket = new ArrayList<>(1);
				candidates.put(hash, bucket);
			}
			bucket.add(new MutableCandidate(bytes));
			count++;
		}

		List<Token> best(int maximum) {
			List<Token> result = new ArrayList<>();
			for (List<MutableCandidate> bucket : candidates.values()) {
				for (MutableCandidate c : bucket) {
					Token token = new Token(c.bytes, c.frequency, -1);
					if (token.gain > 0 && c.frequency >= 2)
						result.add(token);
				}
			}
			result.sort(Comparator.<Token>comparingLong(t -> t.gain)
					.reversed()
					.thenComparing(Comparator.comparingInt((Token t) -> t.bytes.length).reversed())
					.thenComparing((a, b) -> Arrays.compareUnsigned(a.bytes, b.bytes)));
			if (result.size() > maximum)
				result.subList(maximum, result.size()).clear();
			return result;
		}

		private static long hash(byte[] source, int offset, int length) {
			long hash = 0xcbf29ce484222325L ^ length;
			for (int i = 0; i < length; i++) {
				hash ^= source[offset + i] & 0xffL;
				hash *= 0x100000001b3L;
			}
			return hash;
		}

		private static boolean isDelimiter(byte value) {
			return switch (value) {
			case '/', '#', ':', '?', '&', '=', '-', '_', '.', ' ', '\t', '\n', '\r' -> true;
			default -> false;
			};
		}
	}

	private static final class MutableCandidate {
		final byte[] bytes;
		int frequency = 1;

		MutableCandidate(byte[] bytes) {
			this.bytes = bytes;
		}
	}
}
