package org.eclipse.rdf4j.queryrender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SPARQL query shrinker / delta debugger (Java 11, no dependencies).
 *
 * Design: - Phase A: Greedy, structure-aware reducers (OPTIONAL/UNION/FILTER/BIND/VALUES/ORDER BY/etc.). Each reducer
 * proposes safe, syntactically-plausible deletions or flattenings. If the FailureOracle still reports failure (and
 * ValidityOracle OK if provided), accept and repeat. - Phase B: Token-level ddmin (Zeller) over the remaining token
 * list for extra minimization.
 *
 * You control "what is a failure?" with FailureOracle (e.g., "assertRoundTrip fails"). Optionally enforce "query must
 * remain valid" with ValidityOracle (e.g., a reference parser).
 */
public final class SparqlShrinker {

	private SparqlShrinker() {
	}

	// ===========================
	// Oracles & Config
	// ===========================

	/** Return true iff the query still exhibits the bug (e.g., parser throws, or round-trip mismatch). */
	@FunctionalInterface
	public interface FailureOracle {
		boolean fails(String query) throws Exception;
	}

	/** Return true iff the query is valid enough to consider (optional). */
	@FunctionalInterface
	public interface ValidityOracle {
		boolean isValid(String query) throws Exception;
	}

	/** Shrinker configuration. */
	public static final class Config {
		/** Max passes of greedy reductions before ddmin. */
		public int maxGreedyIterations = 30;
		/** Enable token-level ddmin after greedy reductions. */
		public boolean enableDdmin = true;
		/** Enforce validity using validityOracle when set. */
		public boolean enforceValidity = false;
		/** Hard cap on total candidate evaluations (guards endless oracles). */
		public int maxChecks = 10_000;
		/** Insert spaces around operators when rejoining tokens (safer for validity). */
		public boolean spaceyJoin = true;
		/** When removing UNION branches, try removing RIGHT first (often shrinks faster). */
		public boolean unionPreferRight = true;
		/** When removing VALUES rows, target batch factor (n, then n*2...) for bisection-like shrink. */
		public int valuesBatchStart = 8;

		public Config enforceValidity(ValidityOracle v) {
			this.enforceValidity = (v != null);
			return this;
		}
	}

	/** Shrink result. */
	public static final class Result {
		public final String minimized;
		public final int attempts;
		public final int accepted;
		public final List<String> log;

		Result(String minimized, int attempts, int accepted, List<String> log) {
			this.minimized = minimized;
			this.attempts = attempts;
			this.accepted = accepted;
			this.log = Collections.unmodifiableList(new ArrayList<>(log));
		}

		@Override
		public String toString() {
			return "SparqlShrinker.Result{len=" + minimized.length() +
					", attempts=" + attempts + ", accepted=" + accepted +
					", steps=" + log.size() + "}";
		}
	}

	// ===========================
	// Public API
	// ===========================

	/** Shrink a failing SPARQL query to a smaller counterexample. Validity oracle is optional. */
	public static Result shrink(String original,
			FailureOracle failureOracle,
			ValidityOracle validityOracle,
			Config cfg) throws Exception {
		Objects.requireNonNull(original, "original");
		Objects.requireNonNull(failureOracle, "failureOracle");
		if (cfg == null) {
			cfg = new Config();
		}

		// Initial check: if it doesn't fail, nothing to do.
		Guard g = new Guard(failureOracle, validityOracle, cfg);
		if (!g.fails(original)) {
			return new Result(original, g.attempts, g.accepted,
					Collections.singletonList("Original did not fail; no shrink."));
		}

		String q = original;
		List<String> log = new ArrayList<>();

		// Phase A: Greedy structure-aware reductions until fixpoint or limits reached
		boolean progress;
		int greedyRounds = 0;
		do {
			progress = false;
			greedyRounds++;

			// 1) Remove ORDER BY, LIMIT, OFFSET, DISTINCT/REDUCED
			String r1 = removeOrderByLimitOffsetDistinct(q, g, log);
			if (!r1.equals(q)) {
				q = r1;
				progress = true;
				continue;
			}

			// 2) Remove dataset clauses (FROM / FROM NAMED)
			String r2 = removeDatasetClauses(q, g, log);
			if (!r2.equals(q)) {
				q = r2;
				progress = true;
				continue;
			}

			// 3) Flatten SERVICE and GRAPH blocks (strip wrappers)
			String r3 = flattenServiceGraph(q, g, log);
			if (!r3.equals(q)) {
				q = r3;
				progress = true;
				continue;
			}

			// 4) Remove FILTERs (whole) and then simplify EXISTS/NOT EXISTS (flatten inner group)
			String r4 = removeOrSimplifyFilters(q, g, log);
			if (!r4.equals(q)) {
				q = r4;
				progress = true;
				continue;
			}

			// 5) Remove BIND clauses
			String r5 = removeBindClauses(q, g, log);
			if (!r5.equals(q)) {
				q = r5;
				progress = true;
				continue;
			}

			// 6) VALUES shrink: reduce rows, or remove entirely
			String r6 = shrinkValues(q, g, cfg, log);
			if (!r6.equals(q)) {
				q = r6;
				progress = true;
				continue;
			}

			// 7) UNION branch removal (keep left-only or right-only)
			String r7 = shrinkUnionBranches(q, g, cfg.unionPreferRight, log);
			if (!r7.equals(q)) {
				q = r7;
				progress = true;
				continue;
			}

			// 8) OPTIONAL removal / flatten
			String r8 = shrinkOptionalBlocks(q, g, log);
			if (!r8.equals(q)) {
				q = r8;
				progress = true;
				continue;
			}

			// 9) GROUP BY / HAVING removal
			String r9 = removeGroupByHaving(q, g, log);
			if (!r9.equals(q)) {
				q = r9;
				progress = true;
				continue;
			}

			// 10) SELECT projection simplification (to SELECT *), keep query form
			String r10 = simplifySelectProjection(q, g, log);
			if (!r10.equals(q)) {
				q = r10;
				progress = true;
				continue;
			}

			// 11) CONSTRUCT template shrinking (drop extra template triples)
			String r11 = shrinkConstructTemplate(q, g, log);
			if (!r11.equals(q)) {
				q = r11;
				progress = true;
				continue;
			}

			// 12) Trim extra triples/statements inside WHERE: drop dot-separated statements one by one
			String r12 = dropWhereStatements(q, g, log);
			if (!r12.equals(q)) {
				q = r12;
				progress = true;
				continue;
			}

		} while (progress && greedyRounds < cfg.maxGreedyIterations && g.withinBudget());

		// Phase B: ddmin over tokens
		if (cfg.enableDdmin && g.withinBudget()) {
			String dd = ddminTokens(q, g, cfg.spaceyJoin, log);
			q = dd;
		}

		return new Result(q, g.attempts, g.accepted, log);
	}

	public static Result shrink(String original, FailureOracle failureOracle) throws Exception {
		return shrink(original, failureOracle, null, new Config());
	}

	// ===========================
	// Greedy reductions (structure-aware)
	// ===========================

	private static String removeOrderByLimitOffsetDistinct(String q, Guard g, List<String> log) throws Exception {
		String qq = q;

		// DISTINCT / REDUCED (keep SELECT form)
		String qq1 = replaceIf(q, "(?i)\\bSELECT\\s+DISTINCT\\b", "SELECT ");
		if (!qq1.equals(q) && g.accept(qq1)) {
			log.add("Removed DISTINCT");
			q = qq1;
		}

		qq1 = replaceIf(q, "(?i)\\bSELECT\\s+REDUCED\\b", "SELECT ");
		if (!qq1.equals(q) && g.accept(qq1)) {
			log.add("Removed REDUCED");
			q = qq1;
		}

		// LIMIT / OFFSET (standalone or with ORDER BY)
		while (true) {
			String next = stripTailClause(q, "(?i)\\bLIMIT\\s+\\d+");
			if (!next.equals(q) && g.accept(next)) {
				log.add("Removed LIMIT");
				q = next;
				continue;
			}
			next = stripTailClause(q, "(?i)\\bOFFSET\\s+\\d+");
			if (!next.equals(q) && g.accept(next)) {
				log.add("Removed OFFSET");
				q = next;
				continue;
			}
			break;
		}

		// ORDER BY: from "ORDER BY" to before LIMIT/OFFSET or end
		int idx = indexOfKeyword(q, "ORDER", "BY");
		if (idx >= 0) {
			int end = endOfOrderBy(q, idx);
			String cand = q.substring(0, idx) + q.substring(end);
			if (g.accept(cand)) {
				log.add("Removed ORDER BY");
				q = cand;
			} else {
				// If whole removal fails, try reducing to just first key
				String reduced = keepFirstOrderKey(q, idx, end);
				if (!reduced.equals(q) && g.accept(reduced)) {
					log.add("Reduced ORDER BY to one key");
					q = reduced;
				}
			}
		}
		return q.equals(qq) ? qq : q;
	}

	private static String removeDatasetClauses(String q, Guard g, List<String> log) throws Exception {
		String out = q;
		// Remove standalone lines of FROM / FROM NAMED with an IRI.
		// Do repeated passes as long as we can delete one.
		while (true) {
			int idx = indexOfRegex(out, "(?i)\\bFROM\\s+(?:NAMED\\s+)?<[^>]+>");
			if (idx < 0) {
				break;
			}
			int end = endOfLineOrClause(out, idx);
			String cand = out.substring(0, idx) + out.substring(end);
			if (g.accept(cand)) {
				log.add("Removed FROM/FROM NAMED");
				out = cand;
			} else {
				break;
			}
		}
		return out;
	}

	private static String flattenServiceGraph(String q, Guard g, List<String> log) throws Exception {
		// Flatten SERVICE and GRAPH blocks: SERVICE [SILENT]? (IRI|?var) { P } -> P
		String out = q;
		while (true) {
			Match svc = findServiceLike(out);
			if (svc == null) {
				break;
			}
			String cand = out.substring(0, svc.start) + svc.inner + out.substring(svc.end);
			if (g.accept(cand)) {
				log.add("Flattened " + svc.kind + " block");
				out = cand;
			} else {
				break; // stop trying this pattern
			}
		}
		return out;
	}

	private static String removeOrSimplifyFilters(String q, Guard g, List<String> log) throws Exception {
		String out = q;
		while (true) {
			Match f = findFilter(out);
			if (f == null) {
				break;
			}
			// Try removing entire FILTER
			String cand = out.substring(0, f.start) + out.substring(f.end);
			if (g.accept(cand)) {
				log.add("Removed FILTER");
				out = cand;
				continue;
			}
			// If it's FILTER EXISTS { P } or FILTER NOT EXISTS { P }, try keeping just inner P
			if (f.inner != null && !f.inner.isEmpty()) {
				String cand2 = out.substring(0, f.start) + f.inner + out.substring(f.end);
				if (g.accept(cand2)) {
					log.add("Flattened FILTER EXISTS/NOT EXISTS");
					out = cand2;
					continue;
				}
			}
			break;
		}
		return out;
	}

	private static String removeBindClauses(String q, Guard g, List<String> log) throws Exception {
		String out = q;
		while (true) {
			Match b = findBind(out);
			if (b == null) {
				break;
			}
			String cand = out.substring(0, b.start) + out.substring(b.end);
			if (g.accept(cand)) {
				log.add("Removed BIND");
				out = cand;
				continue;
			}
			break;
		}
		return out;
	}

	private static String shrinkValues(String q, Guard g, Config cfg, List<String> log) throws Exception {
		String out = q;
		while (true) {
			ValuesBlock vb = findValues(out);
			if (vb == null) {
				break;
			}

			// Strategy: try removing entire VALUES; if not acceptable, reduce rows by halving batches.
			String remove = out.substring(0, vb.start) + out.substring(vb.end);
			if (g.accept(remove)) {
				log.add("Removed VALUES block");
				out = remove;
				continue;
			}

			if (vb.rows.size() <= 1) {
				break; // can't shrink rows further
			}

			int n = Math.max(cfg.valuesBatchStart, 2);
			List<List<String>> rows = new ArrayList<>(vb.rows);
			boolean did = false;
			while (rows.size() >= 2) {
				int chunk = Math.min(n, rows.size() / 2 + (rows.size() % 2));
				// build candidate with first chunk only
				List<List<String>> kept = rows.subList(0, chunk);
				String cand = out.substring(0, vb.start) +
						vb.renderWithRows(kept) +
						out.substring(vb.end);
				if (g.accept(cand)) {
					log.add("Reduced VALUES rows: " + rows.size() + " → " + kept.size());
					out = cand;
					did = true;
					break;
				} else {
					n = Math.min(rows.size(), n * 2);
				}
			}
			if (!did) {
				break;
			}
		}
		return out;
	}

	private static String shrinkUnionBranches(String q, Guard g, boolean preferRight, List<String> log)
			throws Exception {
		String out = q;
		while (true) {
			UnionMatch u = findUnion(out);
			if (u == null) {
				break;
			}

			// Try keeping left only (remove UNION + right)
			String keepLeft = out.substring(0, u.unionIdx) + out.substring(u.rightEnd + 1);
			// Try keeping right only (remove left + UNION)
			String keepRight = out.substring(0, u.leftStart) + out.substring(u.unionIdx + u.unionLen);

			if (preferRight) {
				if (g.accept(keepRight)) {
					log.add("Removed UNION left-branch");
					out = keepRight;
					continue;
				}
				if (g.accept(keepLeft)) {
					log.add("Removed UNION right-branch");
					out = keepLeft;
					continue;
				}
			} else {
				if (g.accept(keepLeft)) {
					log.add("Removed UNION right-branch");
					out = keepLeft;
					continue;
				}
				if (g.accept(keepRight)) {
					log.add("Removed UNION left-branch");
					out = keepRight;
					continue;
				}
			}
			break;
		}
		return out;
	}

	private static String shrinkOptionalBlocks(String q, Guard g, List<String> log) throws Exception {
		String out = q;
		while (true) {
			Match m = findKeywordBlock(out, "OPTIONAL");
			if (m == null) {
				break;
			}

			// Option A: remove entire OPTIONAL { ... }
			String remove = out.substring(0, m.start) + out.substring(m.end);
			if (g.accept(remove)) {
				log.add("Removed OPTIONAL block");
				out = remove;
				continue;
			}

			// Option B: flatten OPTIONAL { P } -> P
			String flat = out.substring(0, m.start) + m.inner + out.substring(m.end);
			if (g.accept(flat)) {
				log.add("Flattened OPTIONAL block");
				out = flat;
				continue;
			}

			break;
		}
		return out;
	}

	private static String removeGroupByHaving(String q, Guard g, List<String> log) throws Exception {
		String out = q;

		// HAVING: from HAVING ( ... ) possibly multiple, remove whole clause
		int hIdx = indexOfKeyword(out, "HAVING");
		if (hIdx >= 0) {
			int hend = endOfHaving(out, hIdx);
			String cand = out.substring(0, hIdx) + out.substring(hend);
			if (g.accept(cand)) {
				log.add("Removed HAVING");
				out = cand;
			}
		}

		// GROUP BY: remove entire clause
		int gIdx = indexOfKeyword(out, "GROUP", "BY");
		if (gIdx >= 0) {
			int gend = endOfGroupBy(out, gIdx);
			String cand = out.substring(0, gIdx) + out.substring(gend);
			if (g.accept(cand)) {
				log.add("Removed GROUP BY");
				out = cand;
			}
		}

		return out;
	}

	private static String simplifySelectProjection(String q, Guard g, List<String> log) throws Exception {
		// Try converting SELECT ... WHERE to SELECT * WHERE (preserve DISTINCT/REDUCED already removed earlier)
		int sIdx = indexOfKeyword(q, "SELECT");
		int wIdx = indexOfKeyword(q, "WHERE");
		if (sIdx >= 0 && wIdx > sIdx) {
			String head = q.substring(0, sIdx);
			String between = q.substring(sIdx, wIdx);
			String tail = q.substring(wIdx);
			// If already SELECT *, nothing to do
			if (between.matches("(?s).*\\b\\*\\b.*")) {
				return q;
			}

			String selStar = between.replaceAll("(?is)SELECT\\s+.+", "SELECT * ");
			String cand = head + selStar + tail;
			if (g.accept(cand)) {
				log.add("Simplified projection to SELECT *");
				return cand;
			}
		}
		return q;
	}

	private static String shrinkConstructTemplate(String q, Guard g, List<String> log) throws Exception {
		// For explicit CONSTRUCT { template } WHERE { ... } — drop extra template triples.
		// Strategy: inside the first top-level template block after CONSTRUCT, split by '.' and drop trailing parts.
		int cIdx = indexOfKeyword(q, "CONSTRUCT");
		if (cIdx < 0) {
			return q;
		}

		int tplOpen = nextChar(q, '{', cIdx);
		if (tplOpen < 0) {
			return q;
		}
		int tplClose = matchBrace(q, tplOpen);
		if (tplClose < 0) {
			return q;
		}

		String templateBody = q.substring(tplOpen + 1, tplClose);
		List<int[]> dotSegs = splitByDot(templateBody);

		// Try removing segments from the end
		for (int i = dotSegs.size() - 1; i >= 1; i--) { // keep at least one segment
			int[] seg = dotSegs.get(i);
			String newBody = templateBody.substring(0, seg[0]).trim();
			if (!newBody.endsWith(".")) {
				newBody = newBody + " .";
			}
			String cand = q.substring(0, tplOpen + 1) + "\n" + newBody + "\n" + q.substring(tplClose);
			if (g.accept(cand)) {
				log.add("Reduced CONSTRUCT template triples");
				return cand;
			}
		}
		return q;
	}

	private static String dropWhereStatements(String q, Guard g, List<String> log) throws Exception {
		// Find first WHERE { ... } and drop dot-separated top-level statements
		int wIdx = indexOfKeyword(q, "WHERE");
		if (wIdx < 0) {
			return q;
		}
		int open = nextChar(q, '{', wIdx);
		if (open < 0) {
			return q;
		}
		int close = matchBrace(q, open);
		if (close < 0) {
			return q;
		}

		String body = q.substring(open + 1, close);
		List<int[]> segs = splitByDot(body);
		if (segs.size() <= 1) {
			return q;
		}

		for (int i = segs.size() - 1; i >= 0; i--) {
			int[] seg = segs.get(i);
			String newBody = (body.substring(0, seg[0]) + body.substring(seg[1])).trim();
			if (!newBody.endsWith(".")) {
				newBody = newBody + " .";
			}
			String cand = q.substring(0, open + 1) + "\n" + newBody + "\n" + q.substring(close);
			if (g.accept(cand)) {
				log.add("Dropped WHERE statement segment");
				return cand;
			}
		}
		return q;
	}

	// ===========================
	// Token-level ddmin
	// ===========================

	private static String ddminTokens(String q, Guard g, boolean spaceyJoin, List<String> log) throws Exception {
		List<Token> toks = Tokenizer.lex(q);
		if (toks.isEmpty()) {
			return q;
		}

		// ddmin over tokens
		List<Token> minimized = ddmin(toks, cand -> {
			try {
				return g.accept(Tokenizer.join(cand, spaceyJoin));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		String res = Tokenizer.join(minimized, spaceyJoin);
		if (!res.equals(q)) {
			log.add("ddmin reduced tokens: " + toks.size() + " → " + minimized.size());
		}
		return res;
	}

	private static <T> List<T> ddmin(List<T> items, Predicate<List<T>> test) throws Exception {
		// Classic ddmin (Andreas Zeller)
		List<T> c = new ArrayList<>(items);
		int n = 2;
		while (c.size() >= 2) {
			boolean reduced = false;
			int chunkSize = (int) Math.ceil(c.size() / (double) n);

			for (int i = 0; i < c.size(); i += chunkSize) {
				int to = Math.min(c.size(), i + chunkSize);
				List<T> subset = c.subList(i, to);
				List<T> complement = new ArrayList<>(c.size() - subset.size());
				if (i > 0) {
					complement.addAll(c.subList(0, i));
				}
				if (to < c.size()) {
					complement.addAll(c.subList(to, c.size()));
				}

				if (test.test(complement)) {
					c = complement;
					n = Math.max(2, n - 1);
					reduced = true;
					break;
				}
			}
			if (!reduced) {
				if (n >= c.size()) {
					break;
				}
				n = Math.min(c.size(), n * 2);
			}
		}
		return c;
	}

	// ===========================
	// Low-level helpers & scanning
	// ===========================

	private static final class Guard {
		final FailureOracle failure;
		final ValidityOracle validity;
		final Config cfg;
		int attempts = 0;
		int accepted = 0;

		Guard(FailureOracle f, ValidityOracle v, Config cfg) {
			this.failure = f;
			this.validity = v;
			this.cfg = cfg;
		}

		boolean withinBudget() {
			return attempts < cfg.maxChecks;
		}

		boolean fails(String q) throws Exception {
			attempts++;
			return failure.fails(q);
		}

		boolean accept(String q) throws Exception {
			attempts++;
			boolean ok = failure.fails(q) && (!cfg.enforceValidity || (validity != null && validity.isValid(q)));
			if (ok) {
				accepted++;
			}
			return ok;
		}
	}

	// --- Minimal string search helpers (regex guarded) ---

	private static String replaceIf(String src, String regex, String repl) {
		return src.replaceAll(regex, repl);
	}

	private static int indexOfRegex(String src, String regex) {
		Matcher m = Pattern.compile(regex).matcher(src);
		return m.find() ? m.start() : -1;
	}

	private static int indexOfKeyword(String src, String... words) {
		int idx = 0;
		for (int i = 0; i < words.length; i++) {
			int j = indexOfWord(src, words[i], idx);
			if (j < 0) {
				return -1;
			}
			idx = j + words[i].length();
		}
		return idx - words[words.length - 1].length();
	}

	private static int indexOfWord(String src, String word, int fromIdx) {
		String re = "(?i)\\b" + Pattern.quote(word) + "\\b";
		Matcher m = Pattern.compile(re).matcher(src);
		return m.find(fromIdx) ? m.start() : -1;
	}

	private static int endOfLineOrClause(String src, int from) {
		int n = src.length();
		for (int i = from; i < n; i++) {
			char c = src.charAt(i);
			if (c == '\n' || c == '\r') {
				return i;
			}
		}
		return n;
	}

	private static int endOfOrderBy(String q, int orderIdx) {
		// Stop before LIMIT/OFFSET or end
		int end = q.length();
		for (String stop : new String[] { "LIMIT", "OFFSET", "GROUP", "HAVING" }) {
			int s = indexOfWord(q, stop, orderIdx + 1);
			if (s >= 0) {
				end = Math.min(end, s);
			}
		}
		return end;
	}

	private static String keepFirstOrderKey(String q, int start, int end) {
		String head = q.substring(0, start);
		String body = q.substring(start, end);
		String tail = q.substring(end);
		// Keep "ORDER BY <first key>"
		String first = body.replaceFirst(
				"(?is)ORDER\\s+BY\\s+(.+?)(,|\\)|\\s+ASC\\(|\\s+DESC\\(|\\s+LIMIT|\\s+OFFSET|$).*", "ORDER BY $1");
		if (!first.equals(body)) {
			return head + first + tail;
		}
		// last resort: remove everything after "ORDER BY" until next space
		int ob = indexOfWord(body, "BY", 0);
		if (ob >= 0) {
			int ks = ob + 2;
			int ke = body.indexOf(' ', ks + 1);
			if (ke > 0) {
				return head + body.substring(0, ke) + tail;
			}
		}
		return q;
	}

	private static int endOfHaving(String q, int havingIdx) {
		// Simple: from HAVING to next clause keyword or end
		int end = q.length();
		for (String stop : new String[] { "GROUP", "ORDER", "LIMIT", "OFFSET" }) {
			int s = indexOfWord(q, stop, havingIdx + 1);
			if (s >= 0) {
				end = Math.min(end, s);
			}
		}
		return end;
	}

	private static int endOfGroupBy(String q, int start) {
		int end = q.length();
		for (String stop : new String[] { "HAVING", "ORDER", "LIMIT", "OFFSET" }) {
			int s = indexOfWord(q, stop, start + 1);
			if (s >= 0) {
				end = Math.min(end, s);
			}
		}
		return end;
	}

	private static int nextChar(String s, char ch, int from) {
		int i = s.indexOf(ch, from);
		return i;
	}

	private static int matchBrace(String s, int openIdx) {
		char open = s.charAt(openIdx);
		char close = (open == '{') ? '}' : (open == '(') ? ')' : (open == '[' ? ']' : '\0');
		if (close == '\0') {
			return -1;
		}
		int depth = 0;
		boolean inStr = false;
		char strQ = 0;
		for (int i = openIdx; i < s.length(); i++) {
			char c = s.charAt(i);
			if (!inStr && (c == '"' || c == '\'')) {
				inStr = true;
				strQ = c;
				continue;
			}
			if (inStr) {
				if (c == strQ && s.charAt(i - 1) != '\\') {
					inStr = false;
				}
				continue;
			}
			if (c == open) {
				depth++;
			} else if (c == close) {
				depth--;
				if (depth == 0) {
					return i;
				}
			}
		}
		return -1;
	}

	private static List<int[]> splitByDot(String body) {
		List<int[]> segs = new ArrayList<>();
		int depth = 0;
		boolean inStr = false;
		char strQ = 0;
		int segStart = 0;
		for (int i = 0; i < body.length(); i++) {
			char c = body.charAt(i);
			if (!inStr && (c == '"' || c == '\'')) {
				inStr = true;
				strQ = c;
				continue;
			}
			if (inStr) {
				if (c == strQ && body.charAt(i - 1) != '\\') {
					inStr = false;
				}
				continue;
			}
			if (c == '{' || c == '(' || c == '[') {
				depth++;
			} else if (c == '}' || c == ')' || c == ']') {
				depth--;
			} else if (c == '.' && depth == 0) {
				segs.add(new int[] { segStart, i + 1 }); // include dot
				segStart = i + 1;
			}
		}
		if (segStart < body.length()) {
			segs.add(new int[] { segStart, body.length() });
		}
		return segs;
	}

	// --- Pattern matchers for blocks ---

	private static final class Match {
		final int start, end; // span to replace
		final String inner; // inner block (for flattening)
		final String kind;

		Match(int s, int e, String inner, String kind) {
			this.start = s;
			this.end = e;
			this.inner = inner;
			this.kind = kind;
		}
	}

	private static final class UnionMatch {
		final int leftStart, unionIdx, unionLen, rightEnd;

		UnionMatch(int ls, int ui, int ul, int re) {
			this.leftStart = ls;
			this.unionIdx = ui;
			this.unionLen = ul;
			this.rightEnd = re;
		}
	}

	private static final class ValuesBlock {
		final int start, end; // positions in source
		final boolean rowForm; // true if VALUES (vars) { rows }
		final List<List<String>> rows; // textual rows (already captured)
		final String header; // "VALUES ?v {" or "VALUES (?x ?y) {"

		ValuesBlock(int start, int end, boolean rowForm, List<List<String>> rows, String header) {
			this.start = start;
			this.end = end;
			this.rowForm = rowForm;
			this.rows = rows;
			this.header = header;
		}

		String renderWithRows(List<List<String>> keep) {
			StringBuilder sb = new StringBuilder();
			sb.append(header).append(' ');
			if (rowForm) {
				for (List<String> r : keep) {
					sb.append('(');
					for (int i = 0; i < r.size(); i++) {
						if (i > 0) {
							sb.append(' ');
						}
						sb.append(r.get(i));
					}
					sb.append(") ");
				}
			} else {
				// 1-col: header already "VALUES ?v {" form; keep rows as single terms
				for (List<String> r : keep) {
					if (!r.isEmpty()) {
						sb.append(r.get(0)).append(' ');
					}
				}
			}
			sb.append('}');
			return sb.toString();
		}
	}

	private static Match findServiceLike(String q) {
		// SERVICE [SILENT]? (IRI|?var) { P } or GRAPH (IRI|?var) { P }
		for (String kw : new String[] { "SERVICE", "GRAPH" }) {
			int idx = indexOfWord(q, kw, 0);
			while (idx >= 0) {
				int i = idx + kw.length();
				// Skip "SILENT" for SERVICE
				if (kw.equals("SERVICE")) {
					int s = indexOfWord(q, "SILENT", i);
					if (s == i || s == i + 1) {
						i = s + "SILENT".length();
					}
				}
				// Skip ws, then token (IRI or var)
				while (i < q.length() && Character.isWhitespace(q.charAt(i))) {
					i++;
				}
				if (i >= q.length()) {
					break;
				}

				// Accept <...> or ?var/$var or prefixed name token; we just skip one token charwise.
				if (q.charAt(i) == '<') {
					int gt = q.indexOf('>', i + 1);
					if (gt < 0) {
						break;
					}
					i = gt + 1;
				} else if (q.charAt(i) == '?' || q.charAt(i) == '$') {
					int j = i + 1;
					while (j < q.length() && isNameChar(q.charAt(j))) {
						j++;
					}
					i = j;
				} else {
					// prefixed name
					int j = i;
					while (j < q.length() && isNameCharOrColon(q.charAt(j))) {
						j++;
					}
					i = j;
				}

				// Now expect '{'
				while (i < q.length() && Character.isWhitespace(q.charAt(i))) {
					i++;
				}
				if (i >= q.length() || q.charAt(i) != '{') {
					idx = indexOfWord(q, kw, idx + 1);
					continue;
				}
				int close = matchBrace(q, i);
				if (close < 0) {
					idx = indexOfWord(q, kw, idx + 1);
					continue;
				}

				String inner = q.substring(i + 1, close);
				return new Match(idx, close + 1, inner, kw);
			}
		}
		return null;
	}

	private static Match findKeywordBlock(String q, String kw) {
		int idx = indexOfWord(q, kw, 0);
		while (idx >= 0) {
			int i = idx + kw.length();
			while (i < q.length() && Character.isWhitespace(q.charAt(i))) {
				i++;
			}
			if (i < q.length() && q.charAt(i) == '{') {
				int close = matchBrace(q, i);
				if (close > i) {
					String inner = q.substring(i + 1, close);
					return new Match(idx, close + 1, inner, kw);
				}
			}
			idx = indexOfWord(q, kw, idx + 1);
		}
		return null;
	}

	private static Match findFilter(String q) {
		int idx = indexOfWord(q, "FILTER", 0);
		while (idx >= 0) {
			int i = idx + "FILTER".length();
			while (i < q.length() && Character.isWhitespace(q.charAt(i))) {
				i++;
			}
			// FILTER EXISTS { ... } or NOT EXISTS { ... }
			int tmp = i;
			if (matchWord(q, tmp, "NOT")) {
				tmp = skipWord(q, tmp, "NOT");
				while (tmp < q.length() && Character.isWhitespace(q.charAt(tmp))) {
					tmp++;
				}
			}
			if (matchWord(q, tmp, "EXISTS")) {
				tmp = skipWord(q, tmp, "EXISTS");
				while (tmp < q.length() && Character.isWhitespace(q.charAt(tmp))) {
					tmp++;
				}
				if (tmp < q.length() && q.charAt(tmp) == '{') {
					int close = matchBrace(q, tmp);
					if (close > tmp) {
						String inner = q.substring(tmp + 1, close);
						return new Match(idx, close + 1, inner, "FILTER");
					}
				}
			}
			// Otherwise assume FILTER <parenthesized expression>, remove up to matching ')'
			if (i < q.length() && q.charAt(i) == '(') {
				int close = matchBrace(q, i);
				if (close > i) {
					return new Match(idx, close + 1, null, "FILTER");
				}
			}

			idx = indexOfWord(q, "FILTER", idx + 1);
		}
		return null;
	}

	private static Match findBind(String q) {
		int idx = indexOfWord(q, "BIND", 0);
		while (idx >= 0) {
			int i = idx + "BIND".length();
			while (i < q.length() && Character.isWhitespace(q.charAt(i))) {
				i++;
			}
			if (i < q.length() && q.charAt(i) == '(') {
				int close = matchBrace(q, i);
				if (close > i) {
					return new Match(idx, close + 1, null, "BIND");
				}
			}
			idx = indexOfWord(q, "BIND", idx + 1);
		}
		return null;
	}

	private static ValuesBlock findValues(String q) {
		int idx = indexOfWord(q, "VALUES", 0);
		while (idx >= 0) {
			int i = idx + "VALUES".length();
			while (i < q.length() && Character.isWhitespace(q.charAt(i))) {
				i++;
			}
			if (i >= q.length()) {
				break;
			}

			if (q.charAt(i) == '(') {
				// Row form: VALUES (?x ?y) { (..).. }
				int varClose = matchBrace(q, i);
				if (varClose < 0) {
					break;
				}
				int braceOpen = nextNonWs(q, varClose + 1);
				if (braceOpen < 0 || q.charAt(braceOpen) != '{') {
					break;
				}
				int braceClose = matchBrace(q, braceOpen);
				if (braceClose < 0) {
					break;
				}

				String header = q.substring(idx, braceOpen).trim() + " {";
				String rowsTxt = q.substring(braceOpen + 1, braceClose).trim();
				List<List<String>> rows = parseValuesRows(rowsTxt, true);
				return new ValuesBlock(idx, braceClose + 1, true, rows, header);
			} else if (q.charAt(i) == '?' || q.charAt(i) == '$') {
				// 1-col form: VALUES ?x { a b UNDEF }
				int afterVar = i + 1;
				while (afterVar < q.length() && isNameChar(q.charAt(afterVar))) {
					afterVar++;
				}
				int braceOpen = nextNonWs(q, afterVar);
				if (braceOpen < 0 || q.charAt(braceOpen) != '{') {
					break;
				}
				int braceClose = matchBrace(q, braceOpen);
				if (braceClose < 0) {
					break;
				}

				String header = q.substring(idx, braceOpen).trim() + " {";
				String rowsTxt = q.substring(braceOpen + 1, braceClose).trim();
				List<List<String>> rows = parseValuesRows(rowsTxt, false);
				return new ValuesBlock(idx, braceClose + 1, false, rows, header);
			} else {
				// Unknown VALUES form; skip
			}

			idx = indexOfWord(q, "VALUES", idx + 1);
		}
		return null;
	}

	private static List<List<String>> parseValuesRows(String txt, boolean rowForm) {
		List<List<String>> rows = new ArrayList<>();
		if (rowForm) {
			// Rows like: (ex:s1 1) (ex:s2 UNDEF) ...
			int i = 0;
			while (true) {
				i = skipWs(txt, i);
				if (i >= txt.length()) {
					break;
				}
				if (txt.charAt(i) != '(') {
					break;
				}
				int close = matchBrace(txt, i);
				if (close < 0) {
					break;
				}
				String row = txt.substring(i + 1, close).trim();
				if (!row.isEmpty()) {
					rows.add(Arrays.stream(row.split("\\s+")).collect(Collectors.toList()));
				}
				i = close + 1;
			}
		} else {
			// 1-col: tokens separated by whitespace
			String[] parts = txt.split("\\s+");
			for (String p : parts) {
				if (!p.isEmpty()) {
					rows.add(Collections.singletonList(p));
				}
			}
		}
		if (rows.isEmpty()) {
			rows.add(Collections.singletonList("UNDEF")); // guard, though not used if caller checks accept()
		}
		return rows;
	}

	private static UnionMatch findUnion(String q) {
		// Look for pattern: '}' UNION '{' at same nesting level
		int depth = 0;
		boolean inStr = false;
		char qch = 0;
		for (int i = 0; i < q.length(); i++) {
			char c = q.charAt(i);
			if (!inStr && (c == '"' || c == '\'')) {
				inStr = true;
				qch = c;
				continue;
			}
			if (inStr) {
				if (c == qch && q.charAt(i - 1) != '\\') {
					inStr = false;
				}
				continue;
			}
			if (c == '{') {
				depth++;
			} else if (c == '}') {
				depth--;
			} else if ((c == 'U' || c == 'u') && depth >= 1) {
				// Try match "UNION"
				if (matchWord(q, i, "UNION")) {
					// Nearest preceding '}' at same depth+1
					int leftClose = prevChar(q, '}', i - 1);
					if (leftClose < 0) {
						continue;
					}
					// Find its matching '{'
					int leftOpen = backwardsMatchBrace(q, leftClose);
					if (leftOpen < 0) {
						continue;
					}
					// Next '{' after UNION
					int rightOpen = nextChar(q, '{', i + "UNION".length());
					if (rightOpen < 0) {
						continue;
					}
					int rightClose = matchBrace(q, rightOpen);
					if (rightClose < 0) {
						continue;
					}

					return new UnionMatch(leftOpen, i, "UNION".length(), rightClose);
				}
			}
		}
		return null;
	}

	private static int prevChar(String s, char ch, int from) {
		for (int i = from; i >= 0; i--) {
			if (s.charAt(i) == ch) {
				return i;
			}
		}
		return -1;
	}

	private static int backwardsMatchBrace(String s, int closeIdx) {
		char close = s.charAt(closeIdx);
		char open = (close == '}') ? '{' : (close == ')') ? '(' : (close == ']') ? '[' : '\0';
		if (open == '\0') {
			return -1;
		}
		int depth = 0;
		boolean inStr = false;
		char qch = 0;
		for (int i = closeIdx; i >= 0; i--) {
			char c = s.charAt(i);
			if (!inStr && (c == '"' || c == '\'')) {
				inStr = true;
				qch = c;
				continue;
			}
			if (inStr) {
				if (c == qch && (i == 0 || s.charAt(i - 1) != '\\')) {
					inStr = false;
				}
				continue;
			}
			if (c == close) {
				depth++;
			} else if (c == open) {
				depth--;
				if (depth == 0) {
					return i;
				}
			}
		}
		return -1;
	}

	private static boolean matchWord(String s, int pos, String word) {
		if (pos < 0 || pos + word.length() > s.length()) {
			return false;
		}
		String sub = s.substring(pos, pos + word.length());
		boolean b = sub.equalsIgnoreCase(word);
		if (!b) {
			return false;
		}
		// Word boundary checks
		boolean leftOk = (pos == 0) || !Character.isLetterOrDigit(s.charAt(pos - 1));
		int end = pos + word.length();
		boolean rightOk = (end == s.length()) || !Character.isLetterOrDigit(s.charAt(end));
		return leftOk && rightOk;
	}

	private static int skipWord(String s, int pos, String word) {
		return pos + word.length();
	}

	private static int nextNonWs(String s, int pos) {
		int i = pos;
		while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
			i++;
		}
		return i < s.length() ? i : -1;
	}

	private static boolean isNameChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '-';
	}

	private static boolean isNameCharOrColon(char c) {
		return isNameChar(c) || c == ':' || c == '.';
	}

	// ===========================
	// Tokenizer & Joiner
	// ===========================

	private enum TKind {
		WORD,
		VAR,
		IRI,
		STRING,
		PUNCT
	}

	private static final class Token {
		final String text;
		final TKind kind;

		Token(String t, TKind k) {
			this.text = t;
			this.kind = k;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	private static final class Tokenizer {
		static List<Token> lex(String s) {
			List<Token> out = new ArrayList<>();
			int n = s.length();
			int i = 0;
			while (i < n) {
				char c = s.charAt(i);
				// Whitespace
				if (Character.isWhitespace(c)) {
					i++;
					continue;
				}
				// Comments: # ... EOL
				if (c == '#') {
					while (i < n && s.charAt(i) != '\n' && s.charAt(i) != '\r') {
						i++;
					}
					continue;
				}
				// IRI
				if (c == '<') {
					int j = s.indexOf('>', i + 1);
					if (j < 0) {
						out.add(new Token("<", TKind.PUNCT));
						i++;
						continue;
					}
					out.add(new Token(s.substring(i, j + 1), TKind.IRI));
					i = j + 1;
					continue;
				}
				// String (single or double)
				if (c == '"' || c == '\'') {
					int j = i + 1;
					while (j < n) {
						char d = s.charAt(j);
						if (d == c && s.charAt(j - 1) != '\\') {
							j++;
							break;
						}
						j++;
					}
					if (j > n) {
						j = n;
					}
					out.add(new Token(s.substring(i, j), TKind.STRING));
					i = j;
					continue;
				}
				// Variable
				if (c == '?' || c == '$') {
					int j = i + 1;
					while (j < n && isNameChar(s.charAt(j))) {
						j++;
					}
					out.add(new Token(s.substring(i, j), TKind.VAR));
					i = j;
					continue;
				}
				// Punctuation single chars we care about
				if ("{}[]().,;|/^*!+=<>?-".indexOf(c) >= 0) {
					out.add(new Token(String.valueOf(c), TKind.PUNCT));
					i++;
					continue;
				}
				// Word / prefixed name token (include colon and dot parts)
				if (Character.isLetter(c) || c == '_') {
					int j = i + 1;
					while (j < n && isNameCharOrColon(s.charAt(j))) {
						j++;
					}
					out.add(new Token(s.substring(i, j), TKind.WORD));
					i = j;
					continue;
				}
				// Numbers
				if (Character.isDigit(c)) {
					int j = i + 1;
					while (j < n && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.' || s.charAt(j) == 'e'
							|| s.charAt(j) == 'E' || s.charAt(j) == '+' || s.charAt(j) == '-')) {
						j++;
					}
					out.add(new Token(s.substring(i, j), TKind.WORD));
					i = j;
					continue;
				}
				// Fallback: single char as punct
				out.add(new Token(String.valueOf(c), TKind.PUNCT));
				i++;
			}
			return out;
		}

		static String join(List<Token> toks, boolean spacey) {
			if (toks.isEmpty()) {
				return "";
			}
			StringBuilder sb = new StringBuilder(toks.size() * 4);
			Token prev = null;
			for (Token t : toks) {
				if (prev != null && spaceNeeded(prev, t, spacey)) {
					sb.append(' ');
				}
				sb.append(t.text);
				prev = t;
			}
			return sb.toString().trim();
		}

		private static boolean spaceNeeded(Token a, Token b, boolean spacey) {
			if (!spacey) {
				return false;
			}
			// Separate word-ish tokens
			if ((a.kind == TKind.WORD || a.kind == TKind.VAR || a.kind == TKind.STRING || a.kind == TKind.IRI)
					&& (b.kind == TKind.WORD || b.kind == TKind.VAR || b.kind == TKind.STRING || b.kind == TKind.IRI)) {
				return true;
			}

			// Around punctuation we can usually omit, but keep for safety around operators
			String bt = b.text;
			if ("|/^*!+=<>?".contains(bt)) {
				return true;
			}
			// Opening punctuation
			if ("({[".contains(bt)) {
				return true;
			}
			// Closing punctuation doesn't need leading space
			if (")}]".contains(bt)) {
				return false;
			}

			// Dots/semis/commas: ensure separation from words
			if (".,;".contains(bt) && (a.kind == TKind.WORD || a.kind == TKind.VAR)) {
				return false;
			}

			return false;
		}
	}

	// Remove the last matching tail clause (e.g., LIMIT 10, OFFSET 20) from the query text.
	private static String stripTailClause(String src, String regex) {
		Matcher m = Pattern.compile(regex).matcher(src);
		int lastStart = -1, lastEnd = -1;
		while (m.find()) {
			lastStart = m.start();
			lastEnd = m.end();
		}
		if (lastStart >= 0) {
			return src.substring(0, lastStart) + src.substring(lastEnd);
		}
		return src;
	}

	// Skip ASCII whitespace starting at pos; returns first non-ws index (or src.length()).
	private static int skipWs(String s, int pos) {
		int i = pos;
		while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
			i++;
		}
		return i;
	}

}
