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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Bounded lock-free MPMC queue (Vyukov scheme).
 * <p>
 * Each slot carries a sequence number that encodes its state: a slot at index {@code i} is writable when its sequence
 * equals the current enqueue position, and readable when it equals the dequeue position plus one.
 */
public final class MpmcRingBuffer<E> {

	private static final VarHandle SEQ = MethodHandles.arrayElementVarHandle(long[].class);
	private static final VarHandle VAL = MethodHandles.arrayElementVarHandle(Object[].class);

	/**
	 * Holds a single sequence counter in its own object to avoid false sharing.
	 */
	private static final class Sequence {

		private static final VarHandle VALUE;

		static {
			try {
				VALUE = MethodHandles.lookup()
						.findVarHandle(Sequence.class, "value", long.class);
			} catch (ReflectiveOperationException e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		@SuppressWarnings("unused")
		private long p1, p2, p3, p4, p5, p6, p7;
		private long value;
		@SuppressWarnings("unused")
		private long p8, p9, p10, p11, p12, p13, p14;

		long getAcquire() {
			return (long) VALUE.getAcquire(this);
		}

		boolean compareAndSet(long expected, long update) {
			return VALUE.compareAndSet(this, expected, update);
		}

		void setRelease(long v) {
			VALUE.setRelease(this, v);
		}
	}

	private final int mask;
	private final long[] sequences;
	private final Object[] values;

	private final Sequence enqueuePos = new Sequence();
	private final Sequence dequeuePos = new Sequence();

	public MpmcRingBuffer(int capacity) {
		if (Integer.bitCount(capacity) != 1) {
			throw new IllegalArgumentException("Capacity must be a power of two: " + capacity);
		}
		this.mask = capacity - 1;
		this.sequences = new long[capacity];
		this.values = new Object[capacity];
		for (int i = 0; i < capacity; i++) {
			// plain writes are safe here: publication happens via the final fields
			sequences[i] = i;
		}
	}

	public boolean offer(E item) {
		if (item == null) {
			throw new NullPointerException("item");
		}

		long pos;
		int index;

		for (;;) {
			pos = enqueuePos.getAcquire();
			index = (int) (pos & mask);
			long diff = (long) SEQ.getAcquire(sequences, index) - pos;

			if (diff == 0L) {
				if (enqueuePos.compareAndSet(pos, pos + 1L)) {
					break;
				}
				Thread.onSpinWait(); // lost the race against another producer
			} else if (diff < 0L) {
				return false; // full
			} else {
				Thread.onSpinWait(); // another producer still publishing this slot
			}
		}

		VAL.setRelease(values, index, item);
		SEQ.setRelease(sequences, index, pos + 1L);
		return true;
	}

	@SuppressWarnings("unchecked")
	public E poll() {
		long pos;
		int index;

		for (;;) {
			pos = dequeuePos.getAcquire();
			index = (int) (pos & mask);
			long diff = (long) SEQ.getAcquire(sequences, index) - (pos + 1L);

			if (diff == 0L) {
				if (dequeuePos.compareAndSet(pos, pos + 1L)) {
					break;
				}
				Thread.onSpinWait(); // lost the race against another consumer
			} else if (diff < 0L) {
				return null; // empty
			} else {
				Thread.onSpinWait(); // another consumer still draining this slot
			}
		}

		E value = (E) VAL.getAcquire(values, index);
		VAL.setRelease(values, index, null);
		SEQ.setRelease(sequences, index, pos + mask + 1L);
		return value;
	}
}
