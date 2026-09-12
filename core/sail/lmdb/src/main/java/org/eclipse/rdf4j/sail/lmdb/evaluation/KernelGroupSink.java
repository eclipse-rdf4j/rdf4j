/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelContext;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelRuntime;

/** Shared count/DISTINCT output boundary for emitted and interpreted kernels. */
public final class KernelGroupSink implements AutoCloseable {
	private static final long[] NO_VALUES = {};
	private final NativeCountGroupStore store;
	private final KernelOrderSink order;
	private final long[] row;
	private final long offset, limit;
	private final int havingColumn, havingOp;
	private final long havingValue;
	private long skipped, emitted;
	private boolean finished, closed;

	/** Declines only for missing canonicality proof; memory or I/O failure must never fall back to unbounded state. */
	public static KernelGroupSink tryCreate(KernelContext context, int groupWidth, boolean[] distinct,
			int[] orderKeys, boolean[] descending, boolean valueOrder, long offset, long limit,
			int havingOutput, int havingOp, long havingValue) {
		if (context.hooks != null && !context.hooks.keySemantics().supportsCanonicalTermKeys())
			return null;
		return new KernelGroupSink(context, groupWidth, distinct, orderKeys, descending, valueOrder,
				offset, limit, havingOutput, havingOp, havingValue);
	}

	private KernelGroupSink(KernelContext context, int groupWidth, boolean[] distinct,
			int[] orderKeys, boolean[] descending, boolean valueOrder, long offset, long limit,
			int havingOutput, int havingOp, long havingValue) {
		if (offset < 0 || limit < -1 || havingOutput < -1 || havingOutput >= distinct.length) {
			throw new IllegalArgumentException("Invalid grouped output slice or HAVING");
		}
		row = new long[groupWidth + distinct.length];
		this.offset = offset;
		this.limit = limit;
		this.havingColumn = havingOutput < 0 ? -1 : groupWidth + havingOutput;
		this.havingOp = havingOp;
		this.havingValue = havingValue;
		store = new NativeCountGroupStore(groupWidth, distinct,
				context.hooks == null ? id -> id : context.hooks.keySemantics()::canonicalTermKey,
				context.cancellation, context.groupMemoryLedger());
		if (orderKeys != null)
			store.unorderedOutput();
		order = orderKeys == null ? null
				: new KernelOrderSink(row.length, orderKeys, descending,
						valueOrder ? context.hooks : null, context.cancellation, offset, limit);
	}

	public void add(long[] groupIds, long[] inputs, long weight) {
		store.add(groupIds, inputs, weight);
	}

	public void addChannel(long[] groupIds, int channel, long argument, long weight) {
		store.addChannel(groupIds, channel, argument, weight);
	}

	public void addSingleCount(long group, long argument, long weight) {
		store.addSingleCount(group, argument, weight);
	}

	public void addDistinct(long[] ids) {
		store.add(ids, NO_VALUES, 1L);
	}

	public void finish() {
		if (finished || closed)
			return;
		try {
			store.finish();
			if (order != null) {
				while (store.next(row))
					if (accept())
						order.addWithOrdinal(row, store.currentOrdinal());
				store.close();
				order.finish();
			}
			finished = true;
		} catch (RuntimeException | Error failure) {
			fail(failure);
		}
	}

	public int fill(long[] target, int maxRows) {
		if (maxRows <= 0 || closed)
			return 0;
		if (target == null || (long) maxRows * row.length > target.length)
			throw new IllegalArgumentException("Short group output");
		if (!finished)
			throw new IllegalStateException("Group input is unfinished");
		try {
			if (order != null)
				return order.fill(target, maxRows);
			int n = 0;
			while (n < maxRows && (limit < 0 || emitted < limit)) {
				if (!store.next(row)) {
					close();
					break;
				}
				if (!accept())
					continue;
				if (skipped < offset) {
					skipped++;
					continue;
				}
				System.arraycopy(row, 0, target, n * row.length, row.length);
				n++;
				emitted++;
			}
			if (limit >= 0 && emitted >= limit)
				close();
			return n;
		} catch (RuntimeException | Error failure) {
			fail(failure);
			return 0;
		}
	}

	private boolean accept() {
		if (havingColumn < 0)
			return true;
		long value = row[havingColumn];
		switch (havingOp) {
		case LmdbNativeKernelIr.OP_EQ:
			return value == havingValue;
		case LmdbNativeKernelIr.OP_NE:
			return value != havingValue;
		case LmdbNativeKernelIr.OP_LT:
			return value < havingValue;
		case LmdbNativeKernelIr.OP_LE:
			return value <= havingValue;
		case LmdbNativeKernelIr.OP_GT:
			return value > havingValue;
		case LmdbNativeKernelIr.OP_GE:
			return value >= havingValue;
		default:
			throw new IllegalStateException("Unknown grouped HAVING operator");
		}
	}

	public long spilledRecords() {
		return store.spilledRecords();
	}

	private void fail(Throwable failure) {
		KernelRuntime.closeResource(this, failure);
		KernelRuntime.rethrowCloseFailure(failure);
	}

	@Override
	public void close() {
		if (closed)
			return;
		closed = true;
		Throwable failure = KernelRuntime.closeResource(store, null);
		failure = KernelRuntime.closeResource(order, failure);
		KernelRuntime.rethrowCloseFailure(failure);
	}
}
