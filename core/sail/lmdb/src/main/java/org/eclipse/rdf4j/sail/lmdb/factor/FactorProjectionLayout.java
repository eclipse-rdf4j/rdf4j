/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tier-independent aggregate demand decomposition. Request zero establishes group identities and
 * their first representatives without multiplying bags. Each remaining request contains group
 * bindings plus one argument, not the union of independent aggregate argument domains. Equal
 * demands share one traversal. A negative argument denotes a nullary channel (COUNT(*)).
 */
public final class FactorProjectionLayout {
	private final int[][] columns, channels;
	private final boolean[] exact;
	public FactorProjectionLayout(int[] groups, int[] arguments, boolean[] exactWeights) {
		if (arguments.length != exactWeights.length) throw new IllegalArgumentException("aggregate demand dimensions");
		List<int[]> requests = new ArrayList<>();
		List<Boolean> weights = new ArrayList<>();
		List<List<Integer>> updates = new ArrayList<>();
		int[] group = groups.clone(); Arrays.sort(group);
		for (int i = 0; i < group.length; i++)
			if (group[i] < 0 || group[i] >= 64 || i > 0 && group[i] == group[i-1])
				throw new IllegalArgumentException("invalid group demand");
		requests.add(group); weights.add(false); updates.add(new ArrayList<>());
		for (int channel = 0; channel < arguments.length; channel++) {
			int argument = arguments[channel];
			if (argument < -1 || argument >= 64) throw new IllegalArgumentException("invalid argument demand");
			int[] projection = group;
			if (argument >= 0 && Arrays.binarySearch(group, argument) < 0) {
				projection = Arrays.copyOf(group, group.length + 1); projection[group.length] = argument;
				Arrays.sort(projection);
			}
			int request = 0;
			for (; request < requests.size(); request++)
				if (Arrays.equals(requests.get(request), projection)
						&& (request != 0 || !exactWeights[channel])) break;
			if (request == requests.size()) {
				requests.add(projection); weights.add(exactWeights[channel]); updates.add(new ArrayList<>());
			}
			if (exactWeights[channel]) weights.set(request, true);
			updates.get(request).add(channel);
		}
		columns = requests.toArray(int[][]::new); channels = new int[columns.length][]; exact = new boolean[columns.length];
		for (int i = 0; i < columns.length; i++) {
			exact[i] = weights.get(i); channels[i] = updates.get(i).stream().mapToInt(Integer::intValue).toArray();
		}
	}
	public int size() { return columns.length; }
	public int[] columns(int projection) { return columns[projection].clone(); }
	public int[] channels(int projection) { return channels[projection].clone(); }
	public boolean[] exactWeights() { return exact.clone(); }
	public int[][] columns() { return Arrays.stream(columns).map(int[]::clone).toArray(int[][]::new); }
}
