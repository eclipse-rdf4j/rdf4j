/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.queryrender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalizes anonymous variable tokens so structurally identical trees compare equal even if hashed suffixes differ.
 * Standalone identifiers only (left boundary must be a non-word char). Word chars = [A-Za-z0-9_].
 *
 * Families are prefixes (including trailing underscore), e.g. "_anon_path_". Pre-numbered tails (digits-only) are
 * preserved and reserve their numbers.
 */
public final class VarNameNormalizer {

	private static final List<String> DEFAULT_PREFIXES = Arrays.asList(
			"_anon_collection_",
			"_anon_path_inverse_",
			"_anon_path_",
			"_anon_having_",
			"_anon_"
	);

	private VarNameNormalizer() {
	}

	public static String normalizeVars(String input) {
		return normalizeVars(input, DEFAULT_PREFIXES);
	}

	public static String normalizeVars(String input, List<String> families) {
		if (input == null || input.isEmpty()) {
			return input;
		}

		// Longest-first so more specific families win (e.g., path_inverse before path).
		List<String> fams = new ArrayList<>(families);
		fams.sort((a, b) -> Integer.compare(b.length(), a.length()));

		// Reserve numbers per family with BitSet for O(1) next-id.
		final Map<String, BitSet> reserved = new HashMap<>();
		for (String f : fams) {
			reserved.put(f, new BitSet());
		}

		// If there is a shared underscore-terminated prefix (e.g., "_anon_"), use the fast path.
		final String shared = sharedPrefixEndingWithUnderscore(fams);

		if (!shared.isEmpty()) {
			reservePreNumberedFast(input, fams, reserved, shared);
			return rewriteHashedFast(input, fams, reserved, shared);
		}

		// Generic path: bucket by first char; still no regionMatches.
		final Map<Character, List<String>> byFirst = bucketByFirstChar(fams);
		reservePreNumberedGeneric(input, byFirst, reserved);
		return rewriteHashedGeneric(input, byFirst, reserved);
	}

	/* ============================ Fast path (shared prefix) ============================ */

	private static void reservePreNumberedFast(String s, List<String> fams, Map<String, BitSet> reserved,
			String shared) {
		final int n = s.length();
		int i = s.indexOf(shared, 0);
		while (i >= 0) {
			if ((i == 0 || !isWordChar(s.charAt(i - 1)))) {
				String family = matchFamilyAt(s, i, fams);
				if (family != null) {
					final int tailStart = i + family.length();
					if (tailStart < n && isWordChar(s.charAt(tailStart))) {
						int j = tailStart + 1;
						while (j < n && isWordChar(s.charAt(j))) {
							j++;
						}
						int num = parsePositiveIntOrMinusOne(s, tailStart, j);
						if (num >= 0) {
							reserved.get(family).set(num);
						}
					}
				}
			}
			i = s.indexOf(shared, i + 1);
		}
	}

	private static String rewriteHashedFast(String s, List<String> fams, Map<String, BitSet> reserved, String shared) {
		final int n = s.length();
		final StringBuilder out = new StringBuilder(n + 16);
		final Map<String, String> mapping = new LinkedHashMap<>();

		int writePos = 0;
		int i = s.indexOf(shared, 0);
		while (i >= 0) {
			if (!(i == 0 || !isWordChar(s.charAt(i - 1)))) {
				i = s.indexOf(shared, i + 1);
				continue;
			}

			String family = matchFamilyAt(s, i, fams);
			if (family == null) {
				i = s.indexOf(shared, i + 1);
				continue;
			}

			final int tailStart = i + family.length();
			if (tailStart >= n || !isWordChar(s.charAt(tailStart))) {
				i = s.indexOf(shared, i + 1);
				continue;
			}

			int j = tailStart + 1;
			while (j < n && isWordChar(s.charAt(j))) {
				j++;
			}

			if (isAllDigits(s, tailStart, j)) {
				// keep as-is
				out.append(s, writePos, j);
				writePos = j;
			} else {
				String original = s.substring(i, j); // small, acceptable allocation
				String replacement = mapping.get(original);
				if (replacement == null) {
					BitSet bs = reserved.get(family);
					int next = bs.nextClearBit(1);
					bs.set(next);
					replacement = family + next;
					mapping.put(original, replacement);
				}
				out.append(s, writePos, i).append(replacement);
				writePos = j;
			}

			i = s.indexOf(shared, j);
		}
		out.append(s, writePos, n);
		return out.toString();
	}

	/**
	 * Find the specific family that matches at offset i. fams must be sorted longest-first. No regionMatches; inline
	 * char checks.
	 */
	private static String matchFamilyAt(String s, int i, List<String> fams) {
		final int n = s.length();
		for (String f : fams) {
			int len = f.length();
			if (i + len > n) {
				continue;
			}
			// manual "startsWithAt"
			boolean ok = true;
			for (int k = 0; k < len; k++) {
				if (s.charAt(i + k) != f.charAt(k)) {
					ok = false;
					break;
				}
			}
			if (ok) {
				return f;
			}
		}
		return null;
	}

	/* ============================ Generic path (no common prefix) ============================ */

	private static void reservePreNumberedGeneric(String s, Map<Character, List<String>> byFirst,
			Map<String, BitSet> reserved) {
		final int n = s.length();
		for (int i = 0; i < n;) {
			char c = s.charAt(i);
			if (!(i == 0 || !isWordChar(s.charAt(i - 1)))) {
				i++;
				continue;
			}
			List<String> cand = byFirst.get(c);
			if (cand == null) {
				i++;
				continue;
			}

			String family = matchFamilyAtFromBucket(s, i, cand);
			if (family == null) {
				i++;
				continue;
			}

			int tailStart = i + family.length();
			if (tailStart >= n || !isWordChar(s.charAt(tailStart))) {
				i++;
				continue;
			}

			int j = tailStart + 1;
			while (j < n && isWordChar(s.charAt(j))) {
				j++;
			}

			int num = parsePositiveIntOrMinusOne(s, tailStart, j);
			if (num >= 0) {
				reserved.get(family).set(num);
			}

			i = j; // jump past the token
		}
	}

	private static String rewriteHashedGeneric(String s, Map<Character, List<String>> byFirst,
			Map<String, BitSet> reserved) {
		final int n = s.length();
		final StringBuilder out = new StringBuilder(n + 16);
		final Map<String, String> mapping = new LinkedHashMap<>();

		int writePos = 0;
		for (int i = 0; i < n;) {
			char c = s.charAt(i);
			if (!(i == 0 || !isWordChar(s.charAt(i - 1)))) {
				i++;
				continue;
			}
			List<String> cand = byFirst.get(c);
			if (cand == null) {
				i++;
				continue;
			}

			String family = matchFamilyAtFromBucket(s, i, cand);
			if (family == null) {
				i++;
				continue;
			}

			int tailStart = i + family.length();
			if (tailStart >= n || !isWordChar(s.charAt(tailStart))) {
				i++;
				continue;
			}

			int j = tailStart + 1;
			while (j < n && isWordChar(s.charAt(j))) {
				j++;
			}

			if (isAllDigits(s, tailStart, j)) {
				// keep as-is
				out.append(s, writePos, j);
				writePos = j;
			} else {
				String original = s.substring(i, j); // small, acceptable allocation
				String replacement = mapping.get(original);
				if (replacement == null) {
					BitSet bs = reserved.get(family);
					int next = bs.nextClearBit(1);
					bs.set(next);
					replacement = family + next;
					mapping.put(original, replacement);
				}
				out.append(s, writePos, i).append(replacement);
				writePos = j;
			}

			i = j;
		}
		out.append(s, writePos, n);
		return out.toString();
	}

	private static Map<Character, List<String>> bucketByFirstChar(List<String> fams) {
		final Map<Character, List<String>> byFirst = new HashMap<>();
		for (String f : fams) {
			char c = f.charAt(0);
			byFirst.computeIfAbsent(c, k -> new ArrayList<>()).add(f);
		}
		return byFirst;
	}

	private static String matchFamilyAtFromBucket(String s, int i, List<String> fams) {
		final int n = s.length();
		for (String f : fams) {
			int len = f.length();
			if (i + len > n) {
				continue;
			}
			boolean ok = true;
			for (int k = 0; k < len; k++) {
				if (s.charAt(i + k) != f.charAt(k)) {
					ok = false;
					break;
				}
			}
			if (ok) {
				return f;
			}
		}
		return null;
	}

	/* =============================== Utilities =============================== */

	private static String sharedPrefixEndingWithUnderscore(List<String> fams) {
		if (fams.isEmpty()) {
			return "";
		}
		char[] acc = fams.get(0).toCharArray();
		int end = acc.length;
		for (int i = 1; i < fams.size(); i++) {
			String f = fams.get(i);
			end = Math.min(end, f.length());
			for (int k = 0; k < end; k++) {
				if (acc[k] != f.charAt(k)) {
					end = k;
					break;
				}
			}
		}
		while (end > 0 && acc[end - 1] != '_') {
			end--;
		}
		if (end == 0) {
			return "";
		}
		return new String(acc, 0, end);
	}

	private static boolean isAllDigits(String s, int start, int end) {
		for (int i = start; i < end; i++) {
			if (!Character.isDigit(s.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isWordChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_';
	}

	private static int parsePositiveIntOrMinusOne(String s, int start, int end) {
		int n = 0;
		for (int i = start; i < end; i++) {
			char c = s.charAt(i);
			if (!Character.isDigit(c)) {
				return -1;
			}
			n = (n * 10) + (c - '0');
		}
		return n;
	}
}
