/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.sail.lmdb.Varint;

/**
 * Cursor for iterating over consecutive tuples whose elements are encoded as unsigned varints.
 * <p>
 * The encoding supports <em>position reuse</em>: a zero byte acts as a reuse marker, meaning that the element at the
 * same index within the previously decoded tuple must be re-read from its original buffer position. This avoids storing
 * duplicate varint values in the underlying buffer.
 * </p>
 * <p>
 * Typical usage:
 * </p>
 *
 * <pre>
 * {
 * 	&#64;code
 * 	VarintTupleIO input = new VarintTupleIO(4, buffer);
 * 	while (input.hasNext()) {
 * 		for (int i = 0; i < 4; i++) {
 * 			input.next();
 * 			long value = input.readUnsigned();
 * 		}
 * 		input.nextTuple();
 * 	}
 * }
 * </pre>
 * <p>
 * This class operates directly on the supplied {@link ByteBuffer} and mutates its position. It is not thread-safe.
 * </p>
 */
public final class VarintTupleIO {

	public static final class Encoder {
		final int elements;
		final ByteBuffer out;
		final int[] reusePositions;;
		int lastTuplePosition;
		boolean deltaEncodeNextTuple;

		Encoder(int elements, ByteBuffer out, int[] reusePositions, int lastTuplePosition) {
			this.elements = elements;
			this.out = out;
			this.lastTuplePosition = lastTuplePosition;
			this.reusePositions = reusePositions.clone();
		}

		public void resetDeltaEncoding() {
			this.deltaEncodeNextTuple = false;
		}

		public void append(ByteBuffer tuple) {
			if (!deltaEncodeNextTuple) {
				int pos = out.position();
				lastTuplePosition = pos;
				out.put(tuple);
				reusePositions[0] = pos;
				for (int i = 1; i < elements; i++) {
					reusePositions[i] = reusePositions[i - 1] + Varint.firstToLength(out.get(reusePositions[i - 1]));
				}
				deltaEncodeNextTuple = true;
				return;
			}
			int rawPosition = lastTuplePosition;
			int otherValuePosition = tuple.position();
			for (int i = 0; i < elements; i++) {
				final int currentValuePosition = reusePositions[i];
				final byte firstByte = out.get(currentValuePosition);
				final int length = Varint.firstToLength(firstByte);
				if (out.get(rawPosition) == 0) {
					// Zero is a reuse marker, not the encoded value itself.
					rawPosition++;
				} else {
					rawPosition += length;
				}

				byte otherFirstByte = tuple.get(otherValuePosition);

				boolean reuse;
				if (firstByte == otherFirstByte) {
					int result = length == 1 ? 0
							: compareRegion(out, currentValuePosition + 1, tuple,
									otherValuePosition + 1, length - 1);
					reuse = result == 0;
				} else {
					reuse = false;
				}
				int otherLength;
				if (reuse) {
					out.put((byte) 0);
					otherLength = length;
				} else {
					var pos = out.position();
					reusePositions[i] = pos;
					otherLength = Varint.firstToLength(otherFirstByte);
					out.put(pos, tuple, otherValuePosition, otherLength);
					out.position(pos + otherLength);
				}
				otherValuePosition += otherLength;
			}
			lastTuplePosition = rawPosition;
		}

		public void appendNextTuple(VarintTupleIO input) {
			ByteBuffer buffer = input.getBuffer();
			if (!deltaEncodeNextTuple) {
				lastTuplePosition = out.position();
				for (int i = 0; i < elements; i++) {
					if (!input.next()) {
						throw new NoSuchElementException("No element at index " + i);
					}
					int otherValuePosition = buffer.position();
					final int otherLength = Varint.firstToLength(buffer.get(otherValuePosition));
					int pos = out.position();
					reusePositions[i] = pos;
					out.put(pos, buffer, otherValuePosition, otherLength);
					out.position(pos + otherLength);
				}
				input.nextTuple();
				deltaEncodeNextTuple = true;
				return;
			}
			int rawPosition = lastTuplePosition;
			for (int i = 0; i < elements; i++) {
				if (!input.next()) {
					throw new NoSuchElementException("No element at index " + i);
				}

				final int currentValuePosition = reusePositions[i];
				final byte firstByte = out.get(currentValuePosition);
				final int length = Varint.firstToLength(firstByte);
				if (out.get(rawPosition) == 0) {
					// Zero is a reuse marker, not the encoded value itself.
					rawPosition++;
				} else {
					rawPosition += length;
				}

				int otherValuePosition = buffer.position();
				byte otherFirstByte = buffer.get(otherValuePosition);

				boolean reuse;
				if (firstByte == otherFirstByte) {
					int result = length == 1 ? 0
							: compareRegion(out, currentValuePosition + 1, buffer,
									otherValuePosition + 1, length - 1);
					reuse = result == 0;
				} else {
					reuse = false;
				}
				if (reuse) {
					out.put((byte) 0);
				} else {
					var pos = out.position();
					reusePositions[i] = pos;
					int otherLength = Varint.firstToLength(otherFirstByte);
					out.put(pos, buffer, otherValuePosition, otherLength);
					out.position(pos + otherLength);
				}
			}
			lastTuplePosition = rawPosition;
			input.nextTuple();
		}

		/**
		 * Appends as many tuples as possible from the given input to the output buffer, without exceeding the specified
		 * maximum buffer size.
		 *
		 * @param input         the input cursor providing tuples to append
		 * @param maxBufferSize the maximum allowed size of the output buffer
		 * @return {@code true} if all remaining tuples were appended; {@code false} if not all tuples could be appended without
		 *         exceeding the maximum buffer size
		 */
		public boolean appendAllTuples(VarintTupleIO input, int maxBufferSize) {
			if (out.position() < maxBufferSize && input.hasNext()) {
				appendNextTuple(input);

				int tailLength = input.getBuffer().limit() - input.getTupleStartPosition();
				if (input.hasNext() && out.position() + tailLength < maxBufferSize) {
					out.put(out.position(), input.getBuffer(), input.getTupleStartPosition(), tailLength);
					out.position(out.position() + tailLength);
					return true;
				} else {
					// TODO can be further optimized to avoid decoding and re-encoding
					while (out.position() < maxBufferSize && input.hasNext()) {
						appendNextTuple(input);
					}
				}
			}
			return !input.hasNext();
		}
	}

	private static final ByteBuffer EMPTY_READ_ONLY_BUFFER = ByteBuffer.allocate(0).asReadOnlyBuffer();

	/** Number of elements in each tuple. */
	private final int elements;

	/** Buffer containing the encoded tuple data. */
	private ByteBuffer buffer;

	/** Buffer positions of the most recently decoded values, indexed by element position within a tuple. */
	private final int[] reusePositions;

	/** Index of the current element within the tuple, or {@code -1} before the first element of a tuple. */
	private int index = -1;

	/**
	 * Buffer position at which iteration continues.
	 */
	private int nextPosition;

	/** Buffer position of the first byte of the current tuple. */
	private int tupleStartPosition;

	/**
	 * Creates an empty tuple input cursor.
	 *
	 * @param elements the number of elements contained in each tuple, must be positive
	 * @throws IllegalArgumentException if {@code elements} is not positive
	 */
	public VarintTupleIO(int elements) {
		this(elements, EMPTY_READ_ONLY_BUFFER);
	}

	/**
	 * Creates a tuple input cursor starting at the current position of the given buffer.
	 *
	 * @param elements the number of elements contained in each tuple, must be positive
	 * @param buffer   the buffer containing varint-encoded tuple data
	 * @throws IllegalArgumentException if {@code elements} is not positive
	 * @throws NullPointerException     if {@code buffer} is {@code null}
	 */
	public VarintTupleIO(int elements, ByteBuffer buffer) {
		if (elements <= 0) {
			throw new IllegalArgumentException("elements must be positive, was: " + elements);
		}
		if (buffer == null) {
			throw new NullPointerException("buffer must not be null");
		}
		this.elements = elements;
		this.buffer = buffer;
		this.reusePositions = new int[elements];
		this.tupleStartPosition = buffer.position();
		this.nextPosition = this.tupleStartPosition;
	}

	/**
	 * Returns the underlying buffer. The buffer's position reflects the current cursor state and must only be modified
	 * through this cursor.
	 *
	 * @return the buffer backing this cursor
	 */
	public ByteBuffer getBuffer() {
		return buffer;
	}

	/**
	 * Sets the underlying buffer and resets the cursor state to the beginning of the buffer.
	 *
	 * @param buffer the buffer containing varint-encoded tuple data
	 * @throws NullPointerException if {@code buffer} is {@code null}
	 */
	public void setBuffer(ByteBuffer buffer) {
		if (buffer == null) {
			throw new NullPointerException("buffer must not be null");
		}
		this.buffer = buffer;
		this.tupleStartPosition = buffer.position();
		this.nextPosition = buffer.position();
		this.index = -1;
	}

	/**
	 * Returns the number of elements per tuple.
	 *
	 * @return the tuple arity
	 */
	public int getElements() {
		return elements;
	}

	/**
	 * Tests whether at least one further element can be decoded.
	 *
	 * @return {@code true} if {@link #next()} would succeed
	 */
	public boolean hasNext() {
		return nextPosition < buffer.limit();
	}

	/**
	 * Positions the buffer at the next tuple element without decoding it.
	 * <p>
	 * A non-zero byte marks the start of a directly encoded unsigned varint, which is then remembered as the reuse
	 * position for the current element index. A zero byte is a reuse marker; in that case the buffer is moved to the
	 * position stored for the current element index and iteration resumes after the marker on the following call.
	 * </p>
	 *
	 * @return {@code true} if an element is available; {@code false} if the buffer has no remaining data
	 */
	public boolean next() {
		buffer.position(nextPosition);
		if (!buffer.hasRemaining()) {
			return false;
		}
		if (++index == elements) {
			index = 0;
		}
		if (index == 0) {
			tupleStartPosition = buffer.position();
		}
		resolveReuse();
		return true;
	}

	/**
	 * Seeks forward to the first tuple that is greater than or equal to {@code tuple}.
	 * <p>
	 * Tuples are compared lexicographically using the encoded unsigned-varint byte representation. The cursor only
	 * moves forward: each tuple that compares smaller than {@code tuple} is consumed, and state is updated as if that
	 * tuple had been iterated element-by-element. As soon as a tuple compares equal or greater, iteration stops with
	 * the cursor positioned at the beginning of that tuple.
	 * </p>
	 *
	 * @param tuple tuple to seek to (buffer positioned at the first element)
	 * @return a negative value if all remaining tuples are smaller than {@code tuple}; otherwise zero or a positive
	 *         value for the first non-smaller tuple found
	 */
	public int seek(ByteBuffer tuple) {
		int diff = -1;
		final int limit = buffer.limit();

		while (nextPosition < limit) {
			int otherValuePosition = tuple.position();
			int rawPosition = tupleStartPosition;
			for (int i = 0; i < elements; i++) {
				final int currentValuePosition;
				final byte first = buffer.get(rawPosition);
				if (first == 0) {
					currentValuePosition = reusePositions[i];
					rawPosition++;
				} else {
					currentValuePosition = rawPosition;
					rawPosition += Varint.firstToLength(first);
				}

				final byte currentFirst = buffer.get(currentValuePosition);
				final byte otherFirst = tuple.get(otherValuePosition);
				final int length;
				if (currentFirst != otherFirst) {
					length = Varint.firstToLength(otherFirst);
					diff = (currentFirst & 0xff) - (otherFirst & 0xff);
				} else {
					length = Varint.firstToLength(currentFirst);
					diff = length == 1 ? 0
							: compareRegion(buffer, currentValuePosition + 1, tuple, otherValuePosition + 1,
									length - 1);
				}
				if (diff != 0) {
					break;
				}
				otherValuePosition += length;
			}

			if (diff >= 0) {
				return diff;
			}

			// consume tuple and update reuse positions
			rawPosition = tupleStartPosition;
			for (int i = 0; i < elements; i++) {
				final byte first = buffer.get(rawPosition);
				if (first == 0) {
					rawPosition++;
				} else {
					reusePositions[i] = rawPosition;
					rawPosition += Varint.firstToLength(first);
				}
			}

			nextPosition = rawPosition;
			tupleStartPosition = rawPosition;
			index = -1;
		}
		return -1;
	}

	/**
	 * Decodes the element the cursor currently points at. Must be called after a successful {@link #next()} and at most
	 * once per element.
	 *
	 * @return the decoded unsigned value
	 */
	public long readUnsigned() {
		return Varint.readUnsigned(buffer);
	}

	/**
	 * Advances past the current value.
	 * <p>
	 * Directly encoded values are skipped using {@link Varint#skipUnsigned(ByteBuffer)}. Reused values do not consume a
	 * varint at the current position, because their bytes reside at an earlier buffer position that is left untouched.
	 * </p>
	 *
	 * @return {@code true} if another value is available after skipping; {@code false} otherwise
	 */
	public boolean skip() {
		if (!hasNext()) {
			throw new NoSuchElementException("Buffer has no more elements.");
		}
		if (next()) {
			return hasNext();
		}
		return false;
	}

	/**
	 * Skips the current tuple and positions the cursor at the beginning of the next tuple.
	 * <p>
	 * This method does not decode any values, but it updates the reuse positions for all elements in the current tuple.
	 * </p>
	 */
	public void skipTuple() {
		int rawPosition = nextPosition;
		for (int i = index + 1; i < elements; i++) {
			final byte first = buffer.get(rawPosition);
			if (first == 0) {
				rawPosition++;
			} else {
				reusePositions[i] = rawPosition;
				rawPosition += Varint.firstToLength(first);
			}
		}
		nextPosition = rawPosition;
		tupleStartPosition = rawPosition;
		index = -1;
	}

	/**
	 * Rewinds the cursor to the beginning of the current tuple, so that its elements can be decoded again.
	 */
	public void resetTuple() {
		buffer.position(tupleStartPosition);
		nextPosition = tupleStartPosition;
		index = -1;
	}

	/**
	 * Marks the current tuple as fully consumed and prepares the cursor for the next tuple.
	 *
	 * @throws IllegalStateException if not all elements of the current tuple have been processed
	 */
	public void nextTuple() {
		if (index != elements - 1 && index != -1) {
			throw new IllegalStateException(
					"Cannot advance to next tuple until all elements of the current tuple have been processed");
		}
		index = -1;
		tupleStartPosition = nextPosition;
	}

	public int getTupleStartPosition() {
		return tupleStartPosition;
	}

	/**
	 * Resolves whether the element at the current buffer position is stored directly or reuses a previous position.
	 */
	private void resolveReuse() {
		byte first = buffer.get(nextPosition);
		if (first == 0) {
			nextPosition += 1; // resume after the reuse marker
			buffer.position(reusePositions[index]);
		} else {
			nextPosition += Varint.firstToLength(first);
			reusePositions[index] = buffer.position();
		}
	}

	/**
	 * Compares the current tuple with an independently encoded tuple.
	 * <p>
	 * Reuse markers in this cursor's tuple are resolved through {@code reusePositions}. This method does not change the
	 * cursor state or either buffer's position.
	 * </p>
	 *
	 * @param otherTuple buffer positioned at the first element of the tuple to compare
	 * @return a negative value, zero, or a positive value if this tuple is respectively less than, equal to, or greater
	 *         than {@code otherTuple}
	 */
	public int compareTuple(ByteBuffer otherTuple) {
		if (otherTuple == null) {
			throw new NullPointerException("otherTuple must not be null");
		}

		int rawPosition = tupleStartPosition;
		int otherValuePosition = otherTuple.position();
		for (int i = 0; i < elements; i++) {
			boolean reuse = buffer.get(rawPosition) == 0;
			final int currentValuePosition;
			if (reuse) {
				currentValuePosition = reusePositions[i];
			} else {
				currentValuePosition = rawPosition;
			}

			final byte currentFirst = buffer.get(currentValuePosition);
			final byte otherFirst = otherTuple.get(otherValuePosition);
			if (currentFirst != otherFirst) {
				return (currentFirst & 0xff) - (otherFirst & 0xff);
			}

			final int length = Varint.firstToLength(currentFirst);
			int result = length == 1 ? 0
					: compareRegion(buffer, currentValuePosition + 1, otherTuple,
							otherValuePosition + 1, length - 1);
			if (result != 0) {
				return result;
			}

			if (reuse) {
				rawPosition++;
			} else {
				rawPosition += length;
			}

			otherValuePosition += length;
		}
		return 0;
	}

	private static int compareRegion(ByteBuffer bb1, int startIdx1, ByteBuffer bb2, int startIdx2, int length) {
		int result = 0;
		for (int i = 0; result == 0 && i < length; i++) {
			result = (bb1.get(startIdx1 + i) & 0xff) - (bb2.get(startIdx2 + i) & 0xff);
		}
		return result;
	}

	public Encoder createEncoder(ByteBuffer out) {
		int outStartPosition = out.position();
		int[] encoderReusePositions = reusePositions.clone();
		boolean deltaEncodeNextTuple = false;
		if (tupleStartPosition > 0) {
			out.put(outStartPosition, buffer, 0, tupleStartPosition);
			out.position(outStartPosition + tupleStartPosition);
			if (outStartPosition > 0) {
				for (int i = 0; i < elements; i++) {
					encoderReusePositions[i] += outStartPosition;
				}
			}
			deltaEncodeNextTuple = true;
		}
		var encoder = new Encoder(elements, out, encoderReusePositions, tupleStartPosition + outStartPosition);
		encoder.deltaEncodeNextTuple = deltaEncodeNextTuple;
		return encoder;
	}
}
