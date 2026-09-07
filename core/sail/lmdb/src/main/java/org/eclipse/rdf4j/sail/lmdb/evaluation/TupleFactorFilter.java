/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import org.eclipse.rdf4j.sail.lmdb.factor.BorrowedTupleBatch;
import org.eclipse.rdf4j.sail.lmdb.factor.FactorEnvironment;
import org.eclipse.rdf4j.sail.lmdb.factor.SelectedTupleSource;
import org.eclipse.rdf4j.sail.lmdb.factor.TupleSelection;

/**
 * Bounded restriction of one correlated group. All columns remain zipped; independent siblings
 * stay borrowed. Direct input captures resolved payload offsets, not tuple values or hash links.
 * This helper owns readers/selections only, never the upstream source or shared expression.
 */
final class TupleFactorFilter implements AutoCloseable {
	private static final int WINDOW = 256;
	private final RowState row;
	private final NativeBooleanFilter filter;
	private final long reads;
	private BorrowedTupleBatch batch;
	private BorrowedTupleBatch.Cursor reader;
	private SelectedTupleSource selected;
	private TupleSelection selection;
	private FactorEnvironment factors;
	private int[] slots;
	private long[] firstValues;
	private int mark, tick;
	private long group, dependencies, inputWeight, firstWeight, weight;
	private boolean firstPart, exhausted, closed;

	TupleFactorFilter(RowState row, NativeBooleanFilter filter, long reads) {
		this.row = row; this.filter = filter; this.reads = reads;
	}

	void bind(FactorEnvironment factors, int leader, long inputWeight, int mark) {
		if (closed) throw new IllegalStateException("tuple filter closed");
		clearOutput();
		this.factors = factors; this.inputWeight = inputWeight; this.mark = mark;
		group = factors.groupMask(leader);
		dependencies = factors.dependencies(leader) | (reads & ~group);
		BorrowedTupleBatch source = factors.tupleBatch(leader);
		int width = source.columns();
		if (slots == null || slots.length != width) {
			slots = new int[width]; firstValues = new long[width];
		}
		for (long rest = group; rest != 0L; rest &= rest - 1L) {
			int slot = Long.numberOfTrailingZeros(rest), column = factors.tupleColumn(slot);
			slots[column] = slot;
		}
		if (source != batch) {
			if (reader != null) reader.close();
			reader = null; batch = null;
			reader = source.cursor(WINDOW); batch = source;
		}
		reader.bind(factors.lane(leader)); firstPart = true; exhausted = false;
	}

	/** Invalidates a published part before upstream advancement, but retains reusable buffers. */
	void clearOutput() {
		if (selected != null) selected.clear();
		if (selection != null) selection.clear();
	}

	boolean next(FactorEnvironment result) {
		while (!exhausted) {
			int accepted = scanPart();
			if (accepted < 0) { exhausted = true; return false; }
			if (accepted == 0) continue;
			result.append(factors, ~group);
			if (accepted == 1) {
				for (int column = 0; column < slots.length; column++)
					if (!row.bindOrCheckTerm(slots[column], firstValues[column]))
						throw new IllegalStateException("inconsistent selected tuple binding");
				weight = Math.multiplyExact(inputWeight, firstWeight);
			} else {
				boolean direct = selection.hasDirectRows();
				if (selected == null || selected.columns() != slots.length || selected.hasDirectRows() != direct) {
					if (selected != null) selected.close();
					selected = new SelectedTupleSource(slots.length, 8, direct);
				}
				result.bindTuple(slots, selected.select(selection), 0, dependencies);
				weight = inputWeight;
			}
			return true;
		}
		return false;
	}

	/** The first witness is prompt; subsequent parts examine at most WINDOW physical fragments. */
	private int scanPart() {
		boolean witness = firstPart;
		int accepted = 0;
		for (int visited = 0; visited < WINDOW; visited++) {
			if (LmdbNativeFactorRows.cancelled(row, ++tick)) return -1;
			if (!reader.next()) { exhausted = true; break; }
			boolean match = true;
			try {
				for (int column = 0; column < slots.length; column++) {
					match &= row.bindOrCheckTerm(slots[column], reader.value(column));
				}
				match = match && filter.accept(row);
			} finally { row.rollback(mark); }
			if (!match) continue;
			if (accepted++ == 0) {
				for (int column = 0; column < slots.length; column++) firstValues[column] = reader.value(column);
				firstWeight = reader.weight();
			}
			if (witness) { firstPart = false; break; }
			if (accepted == 1) {
				if (selection == null) selection = new TupleSelection(8);
				selection.begin(reader);
			}
			selection.add(reader);
		}
		return accepted;
	}

	long multiplicity() { return weight; }

	@Override public void close() {
		if (closed) return;
		closed = true; factors = null;
		try { if (reader != null) reader.close(); }
		finally {
			reader = null; batch = null;
			if (selection != null) selection.clear();
			if (selected != null) selected.close();
		}
	}
}
