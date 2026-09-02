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
 * 	VarintTupleInput input = new VarintTupleInput(4, buffer);
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
public final class VarintTupleInput {

	/** Sentinel value indicating that no pending resume position exists. */
	private static final int NO_POSITION = -1;

	/** Number of elements in each tuple. */
	private final int elements;

	/** Buffer containing the encoded tuple data. */
	private final ByteBuffer buffer;

	/** Buffer positions of the most recently decoded values, indexed by element position within a tuple. */
	private final int[] reusePositions;

	/** Index of the current element within the tuple, or {@code -1} before the first element of a tuple. */
	private int index = -1;

	/**
	 * Buffer position at which iteration continues after a reuse marker has been resolved, or {@link #NO_POSITION} if
	 * the current element is stored directly in the buffer.
	 */
	private int nextPosition = NO_POSITION;

	/** Buffer position of the first byte of the current tuple. */
	private int tupleStartPosition;

	/**
	 * Creates a tuple input cursor starting at the current position of the given buffer.
	 *
	 * @param elements the number of elements contained in each tuple, must be positive
	 * @param buffer   the buffer containing varint-encoded tuple data
	 * @throws IllegalArgumentException if {@code elements} is not positive
	 * @throws NullPointerException     if {@code buffer} is {@code null}
	 */
	public VarintTupleInput(int elements, ByteBuffer buffer) {
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
		return nextPosition >= 0 ? nextPosition < buffer.limit() : buffer.hasRemaining();
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
		if (nextPosition >= 0) {
			buffer.position(nextPosition);
			nextPosition = NO_POSITION;
		}
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
		if (next()) {
			if (nextPosition < 0) {
				Varint.skipUnsigned(buffer);
			}
			return hasNext();
		}
		return false;
	}

	/**
	 * Rewinds the cursor to the beginning of the current tuple, so that its elements can be decoded again.
	 */
	public void resetTuple() {
		buffer.position(tupleStartPosition);
		nextPosition = NO_POSITION;
		index = -1;
	}

	/**
	 * Marks the current tuple as fully consumed and prepares the cursor for the next tuple.
	 *
	 * @throws IllegalStateException if not all elements of the current tuple have been processed
	 */
	public void nextTuple() {
		if (index != elements - 1) {
			throw new IllegalStateException(
					"Cannot advance to next tuple until all elements of the current tuple have been processed");
		}
		index = -1;
		tupleStartPosition = nextPosition >= 0 ? nextPosition : buffer.position();
	}

	/**
	 * Resolves whether the element at the current buffer position is stored directly or reuses a previous position.
	 */
	private void resolveReuse() {
		if (buffer.get(buffer.position()) == 0) {
			nextPosition = buffer.position() + 1; // resume after the reuse marker
			buffer.position(reusePositions[index]);
		} else {
			nextPosition = NO_POSITION;
			reusePositions[index] = buffer.position();
		}
	}
}
