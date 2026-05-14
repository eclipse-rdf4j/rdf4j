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
package org.eclipse.rdf4j.sail.lmdb.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.sail.lmdb.RecordIterator;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;

final class LmdbIdNaryIndexScanIterator implements RecordIterator, LmdbIdJoinMetricReporter {

	static final class PatternSpec {
		final long[] patternIds;
		final int[] bindingIndexes;
		final String[] varNames;

		PatternSpec(long[] patternIds, int[] bindingIndexes, String[] varNames) {
			this.patternIds = patternIds;
			this.bindingIndexes = bindingIndexes;
			this.varNames = varNames;
		}
	}

	private final RecordIterator source;
	private final List<PatternSpec> patterns;
	private final long[] initialBinding;
	private final int anchorComponent;
	private final int anchorBindingIndex;
	private final List<long[]> pending = new ArrayList<>();

	private long[] nextGroupFirstRow;
	private int pendingIndex;
	private long sourceRowsScannedActual;
	private long sourceRowsMatchedActual;
	private long sourceRowsFilteredActual;

	LmdbIdNaryIndexScanIterator(RecordIterator source, List<PatternSpec> patterns, long[] initialBinding,
			int anchorComponent, int anchorBindingIndex) {
		this.source = source;
		this.patterns = patterns;
		this.initialBinding = initialBinding;
		this.anchorComponent = anchorComponent;
		this.anchorBindingIndex = anchorBindingIndex;
	}

	@Override
	public long[] next() {
		while (pendingIndex >= pending.size()) {
			pending.clear();
			pendingIndex = 0;
			if (!loadNextGroup()) {
				return null;
			}
		}
		return pending.get(pendingIndex++);
	}

	private boolean loadNextGroup() {
		while (true) {
			long[] row = nextGroupFirstRow != null ? nextGroupFirstRow : source.next();
			nextGroupFirstRow = null;
			if (row == null) {
				return false;
			}
			sourceRowsScannedActual++;

			long anchor = row[anchorComponent];
			List<List<long[]>> matchesByPattern = new ArrayList<>(patterns.size());
			for (int i = 0; i < patterns.size(); i++) {
				matchesByPattern.add(new ArrayList<>());
			}
			collectRowMatches(row, matchesByPattern);

			while ((row = source.next()) != null) {
				sourceRowsScannedActual++;
				if (row[anchorComponent] != anchor) {
					nextGroupFirstRow = row;
					break;
				}
				collectRowMatches(row, matchesByPattern);
			}

			if (buildBindingsForGroup(anchor, matchesByPattern)) {
				return true;
			}
		}
	}

	private void collectRowMatches(long[] row, List<List<long[]>> matchesByPattern) {
		boolean matchedAny = false;
		for (int i = 0; i < patterns.size(); i++) {
			long[] match = matchPattern(row, patterns.get(i));
			if (match != null) {
				matchesByPattern.get(i).add(match);
				matchedAny = true;
			}
		}
		if (matchedAny) {
			sourceRowsMatchedActual++;
		} else {
			sourceRowsFilteredActual++;
		}
	}

	private long[] matchPattern(long[] row, PatternSpec pattern) {
		long[] match = new long[initialBinding.length];
		Arrays.fill(match, LmdbValue.UNKNOWN_ID);
		for (int component = 0; component < 4; component++) {
			long constantId = pattern.patternIds[component];
			long value = row[component];
			if (constantId != LmdbValue.UNKNOWN_ID && constantId != value) {
				return null;
			}
			int bindingIndex = pattern.bindingIndexes[component];
			if (bindingIndex >= 0 && !applyValue(match, bindingIndex, value)) {
				return null;
			}
		}
		return match;
	}

	private boolean buildBindingsForGroup(long anchor, List<List<long[]>> matchesByPattern) {
		for (List<long[]> matches : matchesByPattern) {
			if (matches.isEmpty()) {
				return false;
			}
		}

		long[] base = Arrays.copyOf(initialBinding, initialBinding.length);
		if (anchorBindingIndex >= 0 && !applyValue(base, anchorBindingIndex, anchor)) {
			return false;
		}

		List<long[]> partials = new ArrayList<>();
		partials.add(base);
		for (List<long[]> patternMatches : matchesByPattern) {
			List<long[]> nextPartials = new ArrayList<>();
			for (long[] partial : partials) {
				for (long[] match : patternMatches) {
					long[] merged = merge(partial, match);
					if (merged != null) {
						nextPartials.add(merged);
					}
				}
			}
			if (nextPartials.isEmpty()) {
				return false;
			}
			partials = nextPartials;
		}
		pending.addAll(partials);
		return true;
	}

	private long[] merge(long[] left, long[] right) {
		long[] merged = Arrays.copyOf(left, left.length);
		for (int i = 0; i < right.length; i++) {
			long value = right[i];
			if (value != LmdbValue.UNKNOWN_ID && !applyValue(merged, i, value)) {
				return null;
			}
		}
		return merged;
	}

	private boolean applyValue(long[] target, int index, long value) {
		long existing = target[index];
		if (existing == LmdbValue.UNKNOWN_ID) {
			target[index] = value;
			return true;
		}
		return existing == value;
	}

	@Override
	public long getSourceRowsScannedActual() {
		return sourceRowsScannedActual;
	}

	@Override
	public long getSourceRowsMatchedActual() {
		return sourceRowsMatchedActual;
	}

	@Override
	public long getSourceRowsFilteredActual() {
		return sourceRowsFilteredActual;
	}

	@Override
	public long getLeftRowsProbedActual() {
		return 0;
	}

	@Override
	public long getRightRowsScannedActual() {
		return sourceRowsScannedActual;
	}

	@Override
	public void close() {
		source.close();
	}
}
