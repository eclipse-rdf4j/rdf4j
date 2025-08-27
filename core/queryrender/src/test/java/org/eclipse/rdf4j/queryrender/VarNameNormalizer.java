package org.eclipse.rdf4j.queryrender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Normalizes variable names that appear inside "Var (name=...)" tokens.
 *
 * Families normalized by default:
 *   - _anon_collection_
 *   - _anon_path_
 *   - _anon_
 *
 * For each family, distinct original names (e.g., _anon_collection_9821d..., _anon_collection_abcd...)
 * are mapped to _anon_collection_1, _anon_collection_2, ... in first-seen order.
 *
 * Pre-normalized names like _anon_7 are detected and their numbers are reserved to avoid collisions.
 * Constants (e.g., _const_*) and ordinary names (e.g., el) are left untouched.
 */
/**
 * Normalizes anonymous variable tokens within algebra dumps so structurally identical trees compare equal even if
 * hashed suffixes differ.
 *
 * It renumbers any standalone token that starts with a configured family prefix, for example:
 * _anon_collection_9821d155... -> _anon_collection_1 _anon_path_2031d15... -> _anon_path_1 _anon_having_0510da5... ->
 * _anon_having_1 _anon_0921d15... -> _anon_1
 *
 * It matches these tokens anywhere (including but not limited to within "Var (name=...)" fragments), as long as they
 * appear as standalone identifiers, i.e., delimited by non-word characters (not letters/digits/_).
 *
 * Pre-numbered forms like _anon_3 or _anon_having_12 are preserved and their numbers are reserved, so new assignments
 * use the smallest positive unused integer.
 */
public final class VarNameNormalizer {

	/**
	 * Default families to normalize (include trailing underscore). Order doesn’t matter; longest-first is enforced
	 * internally.
	 */
	private static final List<String> DEFAULT_PREFIXES = Arrays.asList(
			"_anon_collection_",
			"_anon_path_",
			"_anon_having_",
			"_anon_path_inverse_",
			"_anon_"
	);

	private VarNameNormalizer() {
	}

	/** Normalize using the default families. */
	public static String normalizeVars(String input) {
		return normalizeVars(input, DEFAULT_PREFIXES);
	}

	/**
	 * Normalize using an explicit, ordered list of families (prefixes) to normalize. Each string should include the
	 * trailing underscore, e.g. "_anon_having_".
	 */
	public static String normalizeVars(String input, List<String> families) {
		if (input == null || input.isEmpty())
			return input;

		// Sort families by descending length so that more specific prefixes (e.g., _anon_collection_) win over _anon_.
		List<String> fams = new ArrayList<>(families);
		fams.sort((a, b) -> Integer.compare(b.length(), a.length()));

		Pattern familyTokenPattern = buildFamilyTokenPattern(fams);

		// Reserved numbers per family (already present in input as digits-only tails).
		final Map<String, SortedSet<Integer>> reserved = new HashMap<>();
		for (String f : fams)
			reserved.put(f, new TreeSet<>());

		// Pass 1: Reserve any digits-only tails already present (e.g., _anon_17).
		{
			Matcher m = familyTokenPattern.matcher(input);
			while (m.find()) {
				String full = m.group(1); // entire token, e.g., _anon_having_0510da5...
				String family = leadingFamily(full, fams);
				if (family != null) {
					String tail = full.substring(family.length());
					if (tail.matches("\\d+")) {
						reserved.get(family).add(Integer.parseInt(tail));
					}
				}
			}
		}

		// Pass 2: Replace hashed/random tails with next available sequential numbers per family.
		final Map<String, String> mapping = new LinkedHashMap<>(); // full original token -> normalized token
		Matcher m = familyTokenPattern.matcher(input);
		StringBuffer out = new StringBuffer(input.length());

		while (m.find()) {
			String original = m.group(1); // matched token
			String family = leadingFamily(original, fams);
			String replacement = original;

			if (family != null) {
				String tail = original.substring(family.length());
				boolean alreadyNumbered = tail.matches("\\d+");
				if (!alreadyNumbered) {
					replacement = mapping.computeIfAbsent(original, k -> {
						int next = nextAvailableIndex(reserved.get(family));
						reserved.get(family).add(next);
						return family + next;
					});
				}
			}

			// Replace this single token instance.
			m.appendReplacement(out, Matcher.quoteReplacement(replacement));
		}
		m.appendTail(out);

		return out.toString();
	}

	/** Build a regex that matches a single standalone family token and captures it as group(1). */
	private static Pattern buildFamilyTokenPattern(List<String> families) {
		// Join families into an alternation, quoting each literally.
		String alt = families.stream()
				.map(Pattern::quote)
				.collect(Collectors.joining("|"));

		// Explanation:
		// (?<![A-Za-z0-9_]) ensures a left boundary that is not a word char, so we don't match inside longer tokens.
		// ( (?:alt) [A-Za-z0-9_]+ ) captures the entire token: family prefix + tail (hex, digits, underscores, etc.).
		return Pattern.compile("(?<![A-Za-z0-9_])((?:" + alt + ")[A-Za-z0-9_]+)");
	}

	/** Find the first matching family prefix for this name, or null if none. */
	private static String leadingFamily(String name, List<String> families) {
		for (String f : families) {
			if (name.startsWith(f))
				return f;
		}
		return null;
	}

	/** Smallest positive integer not already reserved. */
	private static int nextAvailableIndex(SortedSet<Integer> taken) {
		int i = 1;
		for (int used : taken) {
			if (used == i)
				i++;
			else if (used > i)
				break;
		}
		return i;
	}

	// Optional quick demo
	public static void main(String[] args) {
		String s = "GroupElem (_anon_having_0510da5d5008b3a440184f8d038af26b279012345)\n" +
				"  Count\n" +
				"    Var (name=t)\n" +
				"ExtensionElem (_anon_having_0510da5d5008b3a440184f8d038af26b279012345)\n";
		System.out.println(normalizeVars(s));
		// -> GroupElem (_anon_having_1) ... ExtensionElem (_anon_having_1)
	}
}
