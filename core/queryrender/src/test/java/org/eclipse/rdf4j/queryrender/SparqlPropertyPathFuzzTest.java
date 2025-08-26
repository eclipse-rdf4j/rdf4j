package org.eclipse.rdf4j.queryrender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.DynamicTest;

/**
 * Combinatorial SPARQL property-path test generator (Java 11, JUnit 5).
 *
 * HOW TO INTEGRATE: 1) Implement assertRoundTrip(String sparql) to call your parser + canonicalizer, e.g.
 * assertSameSparqlQuery(sparql, cfg()) or equivalent. 2) Implement assertRejects(String sparql) to assert parse
 * failure. 3) Remove @Disabled from the @TestFactory methods.
 */
public class SparqlPropertyPathFuzzTest {

	// =========================
	// CONFIGURATION KNOBS
	// =========================

	/** Max AST depth (atoms at depth 0). Depth 3–4 already finds lots of bugs. */
	private static final int MAX_DEPTH = 1;

	/** Upper bound on total positive tests (across skeletons). Keep sane for CI. */
	private static final int MAX_TESTS = 1;

	/** Generate whitespace variants around operators (if your printer canonicalizes WS). */
	private static final boolean GENERATE_WHITESPACE_VARIANTS = false;

	/** Include "a" (rdf:type) as an atom in path position (legal in SPARQL). */
	private static final boolean INCLUDE_A_SHORTCUT = true;

	/** Make negation of a single inverse compact as !^ex:p instead of !(^ex:p). */
	private static final boolean COMPACT_SINGLE_NEGATION = true;

	/** Deterministic random seed for subsampling when counts exceed caps. */
	private static final long SEED = 0xBADC0FFEE0DDF00DL;

	// Prefixes used in generated queries
	private static final String PREFIXES = "PREFIX ex:   <http://example.org/>\n" +
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n";

	// A small, diverse IRI/prefixed-name set for atoms
	private static final List<String> ATOMS = Collections.unmodifiableList(Arrays.asList(
			"ex:pA", "ex:pB", "ex:pC", "ex:pD",
			"ex:pE", "ex:pF", "ex:pG", "ex:pH",
			"foaf:knows", "foaf:name",
			"<http://example.org/p/I0>",
			"<http://example.org/p/I1>",
			"<http://example.org/p/I2>"
	));

	// =========================
	// PUBLIC TEST FACTORIES
	// =========================

	@TestFactory
	@Disabled
	Stream<DynamicTest> propertyPathPositiveCases() {
		Generator gen = new Generator(MAX_DEPTH);
		Set<PathNode> paths = gen.generateAllPaths();

		// create SELECT skeletons
		List<Function<String, String>> skeletons = Arrays.asList(
				SparqlPropertyPathFuzzTest::skelBasic,
				SparqlPropertyPathFuzzTest::skelChainName,
				SparqlPropertyPathFuzzTest::skelOptional,
				SparqlPropertyPathFuzzTest::skelUnionTwoTriples,
				SparqlPropertyPathFuzzTest::skelFilterExists,
				SparqlPropertyPathFuzzTest::skelValuesSubjects
		);

		// render all, add whitespace variants if enabled, then cap to MAX_TESTS deterministically
		List<String> queries = new ArrayList<>();
		for (PathNode p : paths) {
			String path = Renderer.render(p, COMPACT_SINGLE_NEGATION);
			for (Function<String, String> skel : skeletons) {
				String q = PREFIXES + skel.apply(path);
				queries.add(q);
				if (GENERATE_WHITESPACE_VARIANTS) {
					for (String wq : Whitespace.variants(q)) {
						queries.add(wq);
					}
				}
			}
		}
		// dedupe & cap
		queries = new ArrayList<>(new LinkedHashSet<>(queries));
		queries = Sampler.capDeterministic(queries, MAX_TESTS, SEED);

		return queries.stream()
				.map(q -> DynamicTest.dynamicTest("OK: " + summarize(q),
						() -> assertSameSparqlQuery(q, cfg()))
				);
	}

	private static final String EX = "http://ex/";

	private static final String SPARQL_PREFIX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
			"PREFIX ex: <http://ex/>\n" +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

	// Shared renderer config with canonical whitespace and useful prefixes.
	private static TupleExprIRRenderer.Config cfg() {
		TupleExprIRRenderer.Config style = new TupleExprIRRenderer.Config();
		style.prefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		style.prefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		style.prefixes.put("foaf", "http://xmlns.com/foaf/0.1/");
		style.prefixes.put("ex", "http://ex/");
		style.prefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
		style.valuesPreserveOrder = true;
		return style;
	}

	// ---------- Helpers ----------

	private TupleExpr parseAlgebra(String sparql) {
		try {
			ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, sparql, null);
			return pq.getTupleExpr();
		} catch (MalformedQueryException e) {
			throw new MalformedQueryException(
					"Failed to parse SPARQL query.\n###### QUERY ######\n" + sparql + "\n\n######################",
					e);
		}

	}

	private String render(String sparql, TupleExprIRRenderer.Config cfg) {
		TupleExpr algebra = parseAlgebra(sparql);
		if (sparql.contains("ASK")) {
			return new TupleExprIRRenderer(cfg).renderAsk(algebra, null).trim();
		}

		if (sparql.contains("DESCRIBE")) {
			return new TupleExprIRRenderer(cfg).renderAsk(algebra, null).trim();
		}

		return new TupleExprIRRenderer(cfg).render(algebra, null).trim();
	}

	/** Round-trip twice and assert the renderer is a fixed point (idempotent). */
	private String assertFixedPoint(String sparql, TupleExprIRRenderer.Config cfg) {
//		System.out.println("# Original SPARQL query\n" + sparql + "\n");
		TupleExpr tupleExpr = parseAlgebra(SPARQL_PREFIX + sparql);
//		System.out.println("# Original TupleExpr\n" + tupleExpr + "\n");
		String r1 = render(SPARQL_PREFIX + sparql, cfg);
		String r2;
		try {
			r2 = render(r1, cfg);
		} catch (MalformedQueryException e) {
			throw new RuntimeException("Failed to parse SPARQL query after rendering.\n### Original query ###\n"
					+ sparql + "\n\n### Rendered query ###\n" + r1 + "\n", e);
		}
		assertEquals(r1, r2, "Renderer must be idempotent after one round-trip");
		String r3 = render(r2, cfg);
		assertEquals(r2, r3, "Renderer must be idempotent after two round-trips");
		return r2;
	}

	/** Assert semantic equivalence by comparing result rows (order-insensitive). */
	private void assertSameSparqlQuery(String sparql, TupleExprIRRenderer.Config cfg) {
//		String rendered = assertFixedPoint(original, cfg);
		sparql = sparql.trim();

		try {
			TupleExpr expected = parseAlgebra(SPARQL_PREFIX + sparql);
			String rendered = render(SPARQL_PREFIX + sparql, cfg);
//			System.out.println(rendered + "\n\n\n");
			TupleExpr actual = parseAlgebra(rendered);
			assertThat(VarNameNormalizer.normalizeVars(actual.toString()))
					.as("Algebra after rendering must be identical to original")
					.isEqualTo(VarNameNormalizer.normalizeVars(expected.toString()));
//			assertThat(rendered).isEqualToNormalizingNewlines(SPARQL_PREFIX + sparql);

		} catch (Throwable t) {
			String rendered;
			TupleExpr expected = parseAlgebra(SPARQL_PREFIX + sparql);
			System.out.println("\n\n\n");
			System.out.println("# Original SPARQL query\n" + sparql + "\n");
			System.out.println("# Original TupleExpr\n" + expected + "\n");

			try {
				cfg.debugIR = true;
				System.out.println("\n# Re-rendering with IR debug enabled for this failing test\n");
				// Trigger debug prints from the renderer
				rendered = render(SPARQL_PREFIX + sparql, cfg);
				System.out.println("\n# Rendered SPARQL query\n" + rendered + "\n");
			} finally {
				cfg.debugIR = false;
			}

			TupleExpr actual = parseAlgebra(rendered);

//			assertThat(VarNameNormalizer.normalizeVars(actual.toString()))
//					.as("Algebra after rendering must be identical to original")
//					.isEqualTo(VarNameNormalizer.normalizeVars(expected.toString()));

			// Fail (again) with the original comparison so the test result is correct
			assertThat(rendered).isEqualToNormalizingNewlines(SPARQL_PREFIX + sparql);

		}
	}

	/** Replace with your parse-failure assertion. */
	private static void assertRejects(String sparql) {
		// TODO: integrate with your parser test harness:
		// assertThrows(ParseException.class, () -> parse(sparql));
		throw new UnsupportedOperationException("Wire assertRejects(sparql) to your parser tests");
	}

	// =========================
	// SKELETONS
	// =========================

	private static String skelBasic(String path) {
		return "SELECT ?s ?o\nWHERE {\n  ?s " + path + " ?o .\n}";
	}

	private static String skelChainName(String path) {
		return "SELECT ?s ?n\nWHERE {\n  ?s " + path + "/foaf:name ?n .\n}";
	}

	private static String skelOptional(String path) {
		return "SELECT ?s ?o\nWHERE {\n  OPTIONAL { ?s " + path + " ?o . }\n}";
	}

	private static String skelUnionTwoTriples(String path) {
		return "SELECT ?s ?o\nWHERE {\n  { ?s " + path + " ?o . }\n  UNION\n  { ?o " + path + " ?s . }\n}";
	}

	private static String skelFilterExists(String path) {
		return "SELECT ?s ?o\nWHERE {\n" +
				"  ?s foaf:knows ?o .\n" +
				"  FILTER EXISTS { ?s " + path + " ?o . }\n" +
				"}";
	}

	private static String skelValuesSubjects(String path) {
		return "SELECT ?s ?o\nWHERE {\n" +
				"  VALUES ?s { ex:s1 ex:s2 }\n" +
				"  ?s " + path + " ?o .\n" +
				"}";
	}

	// =========================
	// PATH AST + RENDERER
	// =========================

	/** Precedence: ALT < SEQ < PREFIX < POSTFIX < ATOM/GROUP */
	private enum Prec {
		ALT,
		SEQ,
		PREFIX,
		POSTFIX,
		ATOM
	}

	private interface PathNode {
		Prec prec();

		boolean prohibitsExtraQuantifier(); // to avoid generating a+*
	}

	private static final class Atom implements PathNode {
		final String iri; // prefixed or <IRI> or "a"

		Atom(String iri) {
			this.iri = iri;
		}

		public Prec prec() {
			return Prec.ATOM;
		}

		public boolean prohibitsExtraQuantifier() {
			return false;
		}

		public String toString() {
			return iri;
		}

		public int hashCode() {
			return Objects.hash(iri);
		}

		public boolean equals(Object o) {
			return (o instanceof Atom) && ((Atom) o).iri.equals(iri);
		}
	}

	private static final class Inverse implements PathNode {
		final PathNode inner;

		Inverse(PathNode inner) {
			this.inner = inner;
		}

		public Prec prec() {
			return Prec.PREFIX;
		}

		public boolean prohibitsExtraQuantifier() {
			return inner.prohibitsExtraQuantifier();
		}

		public int hashCode() {
			return Objects.hash("^", inner);
		}

		public boolean equals(Object o) {
			return (o instanceof Inverse) && ((Inverse) o).inner.equals(inner);
		}
	}

	/** SPARQL PathNegatedPropertySet: either !IRI | !^IRI | !(IRI|^IRI|...) */
	private static final class NegatedSet implements PathNode {
		final List<PathNode> elems; // each elem must be Atom or Inverse(Atom)

		NegatedSet(List<PathNode> elems) {
			this.elems = elems;
		}

		public Prec prec() {
			return Prec.PREFIX;
		}

		public boolean prohibitsExtraQuantifier() {
			return false;
		}

		public int hashCode() {
			return Objects.hash("!", elems);
		}

		public boolean equals(Object o) {
			return (o instanceof NegatedSet) && ((NegatedSet) o).elems.equals(elems);
		}
	}

	private static final class Sequence implements PathNode {
		final PathNode left, right;

		Sequence(PathNode left, PathNode right) {
			this.left = left;
			this.right = right;
		}

		public Prec prec() {
			return Prec.SEQ;
		}

		public boolean prohibitsExtraQuantifier() {
			return false;
		}

		public int hashCode() {
			return Objects.hash("/", left, right);
		}

		public boolean equals(Object o) {
			return (o instanceof Sequence) && ((Sequence) o).left.equals(left) && ((Sequence) o).right.equals(right);
		}
	}

	private static final class Alternative implements PathNode {
		final PathNode left, right;

		Alternative(PathNode left, PathNode right) {
			this.left = left;
			this.right = right;
		}

		public Prec prec() {
			return Prec.ALT;
		}

		public boolean prohibitsExtraQuantifier() {
			return false;
		}

		public int hashCode() {
			return Objects.hash("|", left, right);
		}

		public boolean equals(Object o) {
			return (o instanceof Alternative) && ((Alternative) o).left.equals(left)
					&& ((Alternative) o).right.equals(right);
		}
	}

	private enum Quant {
		STAR("*"),
		PLUS("+"),
		QMARK("?");

		final String s;

		Quant(String s) {
			this.s = s;
		}
	}

	private static final class Quantified implements PathNode {
		final PathNode inner;
		final Quant q;

		Quantified(PathNode inner, Quant q) {
			this.inner = inner;
			this.q = q;
		}

		public Prec prec() {
			return Prec.POSTFIX;
		}

		public boolean prohibitsExtraQuantifier() {
			return true;
		} // prevent a+?

		public int hashCode() {
			return Objects.hash("Q", inner, q);
		}

		public boolean equals(Object o) {
			return (o instanceof Quantified)
					&& ((Quantified) o).inner.equals(inner)
					&& ((Quantified) o).q == q;
		}
	}

	private static final class Group implements PathNode {
		final PathNode inner;

		Group(PathNode inner) {
			this.inner = inner;
		}

		public Prec prec() {
			return Prec.ATOM;
		} // parentheses force atom-level

		public boolean prohibitsExtraQuantifier() {
			return inner.prohibitsExtraQuantifier();
		}

		public int hashCode() {
			return Objects.hash("()", inner);
		}

		public boolean equals(Object o) {
			return (o instanceof Group) && ((Group) o).inner.equals(inner);
		}
	}

	private static final class Renderer {
		static String render(PathNode n, boolean compactSingleNeg) {
			StringBuilder sb = new StringBuilder();
			render(n, sb, n.prec(), compactSingleNeg);
			return sb.toString();
		}

		private static void render(PathNode n, StringBuilder sb, Prec ctx, boolean compactSingleNeg) {
			if (n instanceof Atom) {
				sb.append(((Atom) n).iri);
			} else if (n instanceof Inverse) {
				sb.append("^");
				PathNode inner = ((Inverse) n).inner;
				maybeParen(inner, sb, Prec.PREFIX, compactSingleNeg);
			} else if (n instanceof NegatedSet) {
				NegatedSet ns = (NegatedSet) n;
				if (compactSingleNeg && ns.elems.size() == 1
						&& (ns.elems.get(0) instanceof Atom || ns.elems.get(0) instanceof Inverse)) {
					sb.append("!");
					PathNode e = ns.elems.get(0);
					if (e instanceof Inverse || e instanceof Atom) {
						// !^ex:p or !ex:p
						render(e, sb, Prec.PREFIX, compactSingleNeg);
					} else {
						sb.append("(");
						render(e, sb, Prec.ALT, compactSingleNeg);
						sb.append(")");
					}
				} else {
					sb.append("!(");
					for (int i = 0; i < ns.elems.size(); i++) {
						if (i > 0)
							sb.append("|");
						render(ns.elems.get(i), sb, Prec.ALT, compactSingleNeg);
					}
					sb.append(")");
				}
			} else if (n instanceof Sequence) {
				Sequence s = (Sequence) n;
				boolean need = ctx.ordinal() > Prec.SEQ.ordinal(); // parent is tighter than seq? No; we need parens if
																	// parent tighter than us
				if (need)
					sb.append("(");
				render(s.left, sb, Prec.SEQ, compactSingleNeg);
				sb.append("/");
				render(s.right, sb, Prec.SEQ, compactSingleNeg);
				if (need)
					sb.append(")");
			} else if (n instanceof Alternative) {
				Alternative a = (Alternative) n;
				boolean need = ctx.ordinal() > Prec.ALT.ordinal();
				if (need)
					sb.append("(");
				render(a.left, sb, Prec.ALT, compactSingleNeg);
				sb.append("|");
				render(a.right, sb, Prec.ALT, compactSingleNeg);
				if (need)
					sb.append(")");
			} else if (n instanceof Quantified) {
				Quantified q = (Quantified) n;
				maybeParen(q.inner, sb, Prec.POSTFIX, compactSingleNeg);
				sb.append(q.q.s);
			} else if (n instanceof Group) {
				sb.append("(");
				render(((Group) n).inner, sb, Prec.ALT, compactSingleNeg);
				sb.append(")");
			} else {
				throw new IllegalStateException("Unknown node: " + n);
			}
		}

		private static void maybeParen(PathNode child, StringBuilder sb, Prec parentPrec, boolean compactSingleNeg) {
			boolean need = child.prec().ordinal() < parentPrec.ordinal();
			if (need)
				sb.append("(");
			render(child, sb, child.prec(), compactSingleNeg);
			if (need)
				sb.append(")");
		}
	}

	// =========================
	// GENERATOR
	// =========================

	private static final class Generator {

		private final int maxDepth;

		Generator(int maxDepth) {
			this.maxDepth = maxDepth;
		}

		Set<PathNode> generateAllPaths() {
			Map<Integer, Set<PathNode>> byDepth = new HashMap<>();
			// depth 0: atoms + negated-single + inverse(atom) + optional 'a'
			Set<PathNode> d0 = new LinkedHashSet<>();
			for (String a : ATOMS)
				d0.add(new Atom(a));
			if (INCLUDE_A_SHORTCUT)
				d0.add(new Atom("a"));
			// inverse(atom)
			List<PathNode> baseAtoms = new ArrayList<>(d0);
			for (PathNode a : baseAtoms)
				d0.add(new Inverse(a));
			// simple negations: !atom, !^atom
			for (PathNode a : baseAtoms) {
				d0.add(new NegatedSet(Collections.singletonList(a)));
				d0.add(new NegatedSet(Collections.singletonList(new Inverse(a))));
			}
			// small negated sets size 2..3
			for (int k = 2; k <= 3; k++) {
				for (List<PathNode> comb : Combinator.kSubsets(limitInverseAtoms(baseAtoms), k)) {
					d0.add(new NegatedSet(comb));
				}
			}

			byDepth.put(0, d0);

			for (int depth = 1; depth <= maxDepth; depth++) {
				Set<PathNode> acc = new LinkedHashSet<>();

				// Unary: inverse and quantifiers on any smaller-depth node
				for (int d = 0; d < depth; d++) {
					for (PathNode n : byDepth.get(d)) {
						// Avoid ^^p; still legal but redundant—skip to reduce duplicates
						if (!(n instanceof Inverse))
							acc.add(new Inverse(n));

						// Quantifiers, but don't stack them (e.g., a+*). Allow quantifiers on negated sets and groups.
						if (!n.prohibitsExtraQuantifier()) {
							acc.add(new Quantified(n, Quant.STAR));
							acc.add(new Quantified(n, Quant.PLUS));
							acc.add(new Quantified(n, Quant.QMARK));
						}

						// Grouping variants
						acc.add(new Group(n));
					}
				}

				// Binary: sequences and alternatives combining partitions dL + dR = depth-1
				for (int dL = 0; dL < depth; dL++) {
					int dR = depth - 1 - dL;
					for (PathNode L : byDepth.get(dL)) {
						for (PathNode R : byDepth.get(dR)) {
							acc.add(new Sequence(L, R));
							acc.add(new Alternative(L, R));
						}
					}
				}

				byDepth.put(depth, acc);
			}

			// Union of all depths up to max
			Set<PathNode> all = new LinkedHashSet<>();
			for (int d = 0; d <= maxDepth; d++)
				all.addAll(byDepth.get(d));
			// Deduplicate by rendering canonical string (stable set)
			Map<String, PathNode> canonical = new LinkedHashMap<>();
			for (PathNode p : all) {
				canonical.put(Renderer.render(p, COMPACT_SINGLE_NEGATION), p);
			}
			return new LinkedHashSet<>(canonical.values());
		}

		private static List<PathNode> limitInverseAtoms(List<PathNode> atoms) {
			// Only allow Atom or Inverse(Atom) inside negated sets
			List<PathNode> rs = new ArrayList<>();
			for (PathNode n : atoms) {
				if (n instanceof Atom)
					rs.add(n);
				else if (n instanceof Inverse && ((Inverse) n).inner instanceof Atom)
					rs.add(n);
			}
			return rs;
		}
	}

	// =========================
	// INVALID CASES
	// =========================

	private static final class InvalidCases {

		static List<String> generateInvalidPropertyPaths() {
			List<String> bad = new ArrayList<>();

			// Lonely operators
			bad.add("/");
			bad.add("|");
			bad.add("^");
			bad.add("!");
			bad.add("*");
			bad.add("+");
			bad.add("?");

			// Empty groups / sets
			bad.add("()");
			bad.add("!()");
			bad.add("(| ex:pA)");
			bad.add("!(ex:pA|)");
			bad.add("!(|)");

			// Double quantifiers or illegal postfix stacking
			bad.add("ex:pA+*");
			bad.add("ex:pB??");
			bad.add("(ex:pC|ex:pD)+?");

			// Missing operands
			bad.add("/ex:pA");
			bad.add("ex:pA/");
			bad.add("|ex:pA");
			bad.add("ex:pA|");
			bad.add("^/ex:pA");
			bad.add("!/ex:pA");

			// Illegal content in negated set (non-atom path like ex:a/ex:b)
			bad.add("!(ex:pA/ex:pB)");
			bad.add("!(^ex:pA/ex:pB)");
			bad.add("!(ex:pA|ex:pB/ex:pC)");

			// Unbalanced parentheses
			bad.add("(ex:pA|ex:pB");
			bad.add("ex:pA|ex:pB)");
			bad.add("!(^ex:pA|ex:pB");

			// Weird whitespace splits that should still be illegal
			bad.add("ex:pA |  | ex:pB");
			bad.add("ex:pA /  / ex:pB");

			// Quantifier before prefix (nonsense)
			bad.add("*^ex:pA");

			// Inverse of nothing
			bad.add("^()");
			bad.add("^|ex:pA");
			bad.add("^!");
			return bad;
		}
	}

	// =========================
	// HELPERS
	// =========================

	private static final class Combinator {
		static <T> List<List<T>> kSubsets(List<T> arr, int k) {
			List<List<T>> res = new ArrayList<>();
			backtrack(arr, k, 0, new ArrayDeque<>(), res);
			return res;
		}

		private static <T> void backtrack(List<T> arr, int k, int idx, Deque<T> cur, List<List<T>> res) {
			if (cur.size() == k) {
				res.add(new ArrayList<>(cur));
				return;
			}
			for (int i = idx; i < arr.size(); i++) {
				cur.addLast(arr.get(i));
				backtrack(arr, k, i + 1, cur, res);
				cur.removeLast();
			}
		}
	}

	private static final class Sampler {
		static <T> List<T> capDeterministic(List<T> items, int max, long seed) {
			if (items.size() <= max)
				return items;
			Random rnd = new Random(seed);
			List<Integer> idx = new ArrayList<>();
			for (int i = 0; i < items.size(); i++)
				idx.add(i);
			Collections.shuffle(idx, rnd);
			idx = idx.subList(0, max);
			Collections.sort(idx);
			List<T> out = new ArrayList<>(max);
			for (int i : idx)
				out.add(items.get(i));
			return out;
		}
	}

	private static final class Whitespace {
		static List<String> variants(String q) {
			// Very conservative variants: tight vs spaced operators in property paths
			// You can extend this as needed.
			String spaced = q.replaceAll("\\|", " | ")
					.replaceAll("/", " / ")
					.replaceAll("\\^", "^ ")
					.replaceAll("!\\(", "! (")
					.replaceAll("!\\^", "! ^")
					.replaceAll("\\+", " + ")
					.replaceAll("\\*", " * ")
					.replaceAll("\\?", " ? ");
			String compact = q.replaceAll("\\s+", " ")
					.replace(" (", "(")
					.replace("( ", "(")
					.replace(" )", ")")
					.replace(" .", ".")
					.trim();
			LinkedHashSet<String> set = new LinkedHashSet<>();
			set.add(q);
			set.add(spaced);
			set.add(compact);
			return new ArrayList<>(set);
		}
	}

	private static String summarize(String q) {
		String one = q.replace("\n", "\\n");
		if (one.length() <= 140)
			return one;
		return one.substring(0, 137) + "...";
	}
}
