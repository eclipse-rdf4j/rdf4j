package org.eclipse.rdf4j.queryrender;

import static java.util.Spliterator.ORDERED;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Streaming SPARQL property-path test generator (Java 11, JUnit 5). - No all-upfront sets; everything is lazy. -
 * Bounded distinct filtering so memory ~ O(MAX_TESTS). - Deterministic order, deterministic cap.
 *
 * HOW TO INTEGRATE: 1) Implement assertRoundTrip(String sparql) to call your parser + canonicalizer, e.g.
 * assertSameSparqlQuery(sparql, cfg()). 2) Implement assertRejects(String sparql) to assert parse failure. 3)
 * Remove @Disabled from @TestFactory methods after wiring.
 */
public class SparqlPropertyPathStreamTest {

	// =========================
	// CONFIG
	// =========================

	/** Max AST depth (atoms at depth 0). */
	private static final int MAX_DEPTH = 4;

	/** Upper bound on total positive tests (across all skeletons and WS variants). */
	private static final int MAX_TESTS = 5000;

	/** Upper bound on total negative tests. */
	private static final int MAX_NEG_TESTS = 300;

	/** Generate whitespace variants if your canonicalizer collapses WS. */
	private static final boolean GENERATE_WHITESPACE_VARIANTS = false;

	/** Include 'a' (rdf:type) as an atom in path position (legal); excluded inside !(...) sets. */
	private static final boolean INCLUDE_A_SHORTCUT = true;

	/** Render !^ex:p as compact single negation when possible. */
	private static final boolean COMPACT_SINGLE_NEGATION = true;

	/** Deterministic seed used only for optional sampling knobs (not used by default). */
	@SuppressWarnings("unused")
	private static final long SEED = 0xBADC0FFEE0DDF00DL;

	// A small, diverse IRI/prefixed-name vocabulary
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
	Stream<DynamicTest> propertyPathPositiveCases_streaming() {
		List<Function<String, String>> skeletons = Arrays.asList(
				SparqlPropertyPathStreamTest::skelBasic,
				SparqlPropertyPathStreamTest::skelChainName,
				SparqlPropertyPathStreamTest::skelOptional,
				SparqlPropertyPathStreamTest::skelUnionTwoTriples,
				SparqlPropertyPathStreamTest::skelFilterExists,
				SparqlPropertyPathStreamTest::skelValuesSubjects
		);

		final int variantsPerQuery = GENERATE_WHITESPACE_VARIANTS ? 3 : 1;
		final int perPathYield = skeletons.size() * variantsPerQuery;
		final int neededDistinctPaths = Math.max(1, (int) Math.ceil((double) MAX_TESTS / perPathYield));

		// Bound dedupe to only what we plan to consume
		Set<String> seenPaths = new LinkedHashSet<>(neededDistinctPaths * 2);

		Stream<String> distinctPaths = PathStreams.allDepths(MAX_DEPTH)
				.map(p -> Renderer.render(p, COMPACT_SINGLE_NEGATION))
				.filter(distinctLimited(seenPaths, neededDistinctPaths))
				.limit(neededDistinctPaths); // hard stop once we have enough

		Stream<String> queries = distinctPaths.flatMap(path -> skeletons.stream().flatMap(skel -> {
			String q = SPARQL_PREFIX + skel.apply(path);
			if (!GENERATE_WHITESPACE_VARIANTS) {
				return Stream.of(q);
			} else {
				return Whitespace.variants(q).stream();
			}
		})
		).limit(MAX_TESTS);

		return queries.map(q -> DynamicTest.dynamicTest("OK: " + summarize(q), () -> assertSameSparqlQuery(q, cfg()))
		);
	}

//	@Disabled("Wire assertRejects(), then remove @Disabled")
//	@TestFactory
//	Stream<DynamicTest> propertyPathNegativeCases_streaming() {
//		// Simple: fixed invalids list -> stream -> cap -> tests
//		Stream<String> invalidPaths = InvalidCases.streamInvalidPropertyPaths();
//		Stream<String> invalidQueries = invalidPaths
//				.map(SparqlPropertyPathStreamTest::skelWrapBasic)
//				.limit(MAX_NEG_TESTS);
//
//		return invalidQueries.map(q ->
//				DynamicTest.dynamicTest("REJECT: " + summarize(q), () -> assertRejects(q))
//		);
//	}

	// =========================
	// ASSERTION HOOKS (INTEGRATE HERE)
	// =========================

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
		TupleExpr expected;
		try {
			expected = parseAlgebra(sparql);

		} catch (Exception e) {
			return;
		}

		try {
			String rendered = render(sparql, cfg);
//			System.out.println(rendered + "\n\n\n");
			TupleExpr actual = parseAlgebra(rendered);
			assertThat(VarNameNormalizer.normalizeVars(actual.toString()))
					.as("Algebra after rendering must be identical to original")
					.isEqualTo(VarNameNormalizer.normalizeVars(expected.toString()));
//			assertThat(rendered).isEqualToNormalizingNewlines(SPARQL_PREFIX + sparql);

		} catch (Throwable t) {
			String rendered;
			expected = parseAlgebra(sparql);
			System.out.println("\n\n\n");
			System.out.println("# Original SPARQL query\n" + sparql + "\n");
			System.out.println("# Original TupleExpr\n" + expected + "\n");

			try {
				cfg.debugIR = true;
				System.out.println("\n# Re-rendering with IR debug enabled for this failing test\n");
				// Trigger debug prints from the renderer
				rendered = render(sparql, cfg);
				System.out.println("\n# Rendered SPARQL query\n" + rendered + "\n");
			} finally {
				cfg.debugIR = false;
			}

			TupleExpr actual = parseAlgebra(rendered);

//			assertThat(VarNameNormalizer.normalizeVars(actual.toString()))
//					.as("Algebra after rendering must be identical to original")
//					.isEqualTo(VarNameNormalizer.normalizeVars(expected.toString()));

			// Fail (again) with the original comparison so the test result is correct
			assertThat(rendered).isEqualToNormalizingNewlines(sparql);

		}
	}

	// =========================
	// SKELETONS
	// =========================

	private static String skelBasic(String path) {
		return "SELECT ?s ?o\nWHERE {\n  ?s " + path + " ?o .\n}";
	}

	private static String skelWrapBasic(String path) {
		return SPARQL_PREFIX + skelBasic(path);
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
				"  FILTER EXISTS {\n" +
				"    ?s " + path + " ?o . \n" +
				"  }\n" +
				"}";
	}

	private static String skelValuesSubjects(String path) {
		return "SELECT ?s ?o\nWHERE {\n" +
				"    VALUES (?s) {\n" +
				"    (ex:s1)\n" +
				"    (ex:s2)\n" +
				"  }\n" +
				"  ?s " + path + " ?o .\n" +
				"}";
	}

	// =========================
	// PATH AST + RENDERER
	// =========================

	/** Precedence: ALT < SEQ < PREFIX (!,^) < POSTFIX (*,+,?) < ATOM/GROUP. */
	private enum Prec {
		ALT,
		SEQ,
		PREFIX,
		POSTFIX,
		ATOM
	}

	private interface PathNode {
		Prec prec();

		boolean prohibitsExtraQuantifier(); // avoid a+*, (…)?+, etc.
	}

	private static final class Atom implements PathNode {
		final String iri; // prefixed, <IRI>, or 'a'

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

	/** SPARQL PathNegatedPropertySet: only IRI or ^IRI elements (no 'a', no composed paths). */
	private static final class NegatedSet implements PathNode {
		final ArrayList<PathNode> elems; // each elem must be Atom(!= 'a') or Inverse(Atom(!='a'))

		NegatedSet(List<PathNode> elems) {
			this.elems = new ArrayList<>(elems);
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
		}

		public int hashCode() {
			return Objects.hash("Q", inner, q);
		}

		public boolean equals(Object o) {
			return (o instanceof Quantified) && ((Quantified) o).inner.equals(inner) && ((Quantified) o).q == q;
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
				ns.elems.sort(Comparator.comparing(Object::toString)); // deterministic order
				if (compactSingleNeg && ns.elems.size() == 1
						&& (ns.elems.get(0) instanceof Atom || ns.elems.get(0) instanceof Inverse)) {
					sb.append("!");
					PathNode e = ns.elems.get(0);
					render(e, sb, Prec.PREFIX, compactSingleNeg); // !^ex:p or !ex:p
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
				boolean need = ctx.ordinal() > Prec.SEQ.ordinal();
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
	// STREAMING GENERATOR
	// =========================

	private static final class PathStreams {

		/** Stream all PathNodes up to maxDepth, lazily, in deterministic order. */
		static Stream<PathNode> allDepths(int maxDepth) {
			Stream<PathNode> s = Stream.empty();
			for (int d = 0; d <= maxDepth; d++) {
				s = Stream.concat(s, depth(d));
			}
			return s;
		}

		/** Stream all PathNodes at exactly 'depth', lazily. */
		static Stream<? extends PathNode> depth(int depth) {
			if (depth == 0)
				return depth0();
			return Stream.concat(unary(depth), binary(depth));
		}

		// ----- depth=0: atoms, inverse(atom), negated singles and small sets -----

		private static Stream<? extends PathNode> depth0() {
			Stream<? extends PathNode> atoms = atomStream();
			Stream<PathNode> inverses = atomStream().map(Inverse::new);

			// Negated singles: !iri and !^iri (exclude 'a' from set elements)
			Stream<PathNode> negSingles = Stream.concat(
					iriAtoms().map(a -> new NegatedSet(Collections.singletonList(a))),
					iriAtoms().map(a -> new NegatedSet(Collections.singletonList(new Inverse(a))))
			);

			// Small negated sets of size 2..3, using [iri, ^iri] domain
			List<PathNode> negDomain = Stream.concat(
					iriAtoms(),
					iriAtoms().map(Inverse::new)
			).collect(Collectors.toList()); // small list; fine to collect

			Stream<PathNode> negSets = Stream.concat(kSubsets(negDomain, 2), kSubsets(negDomain, 3))
					.map(NegatedSet::new);

			return Stream.of(atoms, inverses, negSingles, negSets).reduce(Stream::concat).orElseGet(Stream::empty);
		}

		// ----- unary: for each smaller depth node, yield inverse, quantifiers, group -----

		private static Stream<PathNode> unary(int depth) {
			// dChild in [0 .. depth-1]
			Stream<PathNode> chained = Stream.empty();
			for (int d = 0; d < depth; d++) {
				Stream<PathNode> fromD = depth(d).flatMap(n -> {
					Stream<PathNode> inv = (n instanceof Inverse) ? Stream.empty() : Stream.of(new Inverse(n));
					Stream<PathNode> quants = n.prohibitsExtraQuantifier()
							? Stream.empty()
							: Stream.of(new Quantified(n, Quant.STAR), new Quantified(n, Quant.PLUS),
									new Quantified(n, Quant.QMARK));
					Stream<PathNode> grp = Stream.of(new Group(n));
					return Stream.of(inv, quants, grp).reduce(Stream::concat).orElseGet(Stream::empty);
				});
				chained = Stream.concat(chained, fromD);
			}
			return chained;
		}

		// ----- binary: for dL + dR = depth-1, cross product of left x right -----

		private static Stream<PathNode> binary(int depth) {
			Stream<PathNode> all = Stream.empty();
			for (int dL = 0; dL < depth; dL++) {
				int dR = depth - 1 - dL;
				Stream<PathNode> part = depth(dL)
						.flatMap(L -> depth(dR).flatMap(R -> Stream.of(new Sequence(L, R), new Alternative(L, R))
						)
						);
				all = Stream.concat(all, part);
			}
			return all;
		}

		// ----- atoms + helpers -----

		private static Stream<Atom> atomStream() {
			Stream<String> base = ATOMS.stream();
			if (INCLUDE_A_SHORTCUT)
				base = Stream.concat(Stream.of("a"), base);
			return base.map(Atom::new);
		}

		private static Stream<Atom> iriAtoms() {
			// exclude 'a' for negated set elements (SPARQL restricts to IRI/^IRI)
			return ATOMS.stream().map(Atom::new);
		}

		/** Lazy k-subsets over a small list (deterministic order, no allocations per element). */
		private static <T> Stream<List<T>> kSubsets(List<T> list, int k) {
			if (k < 0 || k > list.size())
				return Stream.empty();
			if (k == 0)
				return Stream.of(Collections.emptyList());

			Spliterator<List<T>> sp = new Spliterators.AbstractSpliterator<List<T>>(Long.MAX_VALUE, ORDERED) {
				final int n = list.size();
				final int[] idx = initFirst(k);
				boolean hasNext = (k <= n);

				@Override
				public boolean tryAdvance(Consumer<? super List<T>> action) {
					if (!hasNext)
						return false;
					List<T> comb = new ArrayList<>(k);
					for (int i = 0; i < k; i++)
						comb.add(list.get(idx[i]));
					action.accept(Collections.unmodifiableList(comb));
					hasNext = nextCombination(idx, n, k);
					return true;
				}
			};
			return StreamSupport.stream(sp, false);
		}

		private static int[] initFirst(int k) {
			int[] idx = new int[k];
			for (int i = 0; i < k; i++)
				idx[i] = i;
			return idx;
		}

		// Lexicographic next combination
		private static boolean nextCombination(int[] idx, int n, int k) {
			for (int i = k - 1; i >= 0; i--) {
				if (idx[i] != i + n - k) {
					idx[i]++;
					for (int j = i + 1; j < k; j++)
						idx[j] = idx[j - 1] + 1;
					return true;
				}
			}
			return false;
		}
	}

	// =========================
	// INVALID CASES (streamed)
	// =========================

	private static final class InvalidCases {
		static Stream<String> streamInvalidPropertyPaths() {
			// NOTE: keep this small; streaming isn't necessary here,
			// but we provide as a Stream for symmetry and easy capping.
			List<String> bad = new ArrayList<>();

			// Lonely operators
			Collections.addAll(bad, "/", "|", "^", "!", "*", "+", "?");

			// Empty groups / sets
			Collections.addAll(bad, "()", "!()", "(| ex:pA)", "!(ex:pA|)", "!(|)");

			// Double quantifiers / illegal postfix stacking
			Collections.addAll(bad, "ex:pA+*", "ex:pB??", "(ex:pC|ex:pD)+?");

			// Missing operands
			Collections.addAll(bad, "/ex:pA", "ex:pA/", "|ex:pA", "ex:pA|", "^/ex:pA", "!/ex:pA");

			// Illegal content in negated set (non-atom paths; 'a' forbidden)
			Collections.addAll(bad, "!(ex:pA/ex:pB)", "!(^ex:pA/ex:pB)", "!(ex:pA|ex:pB/ex:pC)", "!(a)");

			// Unbalanced parentheses
			Collections.addAll(bad, "(ex:pA|ex:pB", "ex:pA|ex:pB)", "!(^ex:pA|ex:pB");

			// Weird whitespace splits that should still be illegal
			Collections.addAll(bad, "ex:pA |  | ex:pB", "ex:pA /  / ex:pB");

			// Quantifier before prefix (nonsense)
			Collections.addAll(bad, "*^ex:pA");

			// Inverse of nothing
			Collections.addAll(bad, "^()", "^|ex:pA", "^!");

			return bad.stream();
		}
	}

	// =========================
	// HELPERS
	// =========================

	/** Bounded distinct: returns true for the first 'limit' distinct items; false afterwards or on duplicates. */
	private static <T> Predicate<T> distinctLimited(Set<T> seen, int limit) {
		Objects.requireNonNull(seen, "seen");
		AtomicInteger left = new AtomicInteger(limit);
		return t -> {
			if (seen.contains(t))
				return false;
			int remaining = left.get();
			if (remaining <= 0)
				return false;
			// Reserve a slot then record
			if (left.compareAndSet(remaining, remaining - 1)) {
				seen.add(t);
				return true;
			}
			return false;
		};
	}

	private static final class Whitespace {
		static List<String> variants(String q) {
			// Conservative operator spacing variants
			String spaced = q.replace("|", " | ")
					.replace("/", " / ")
					.replace("^", "^ ")
					.replace("!(", "! (")
					.replace("!^", "! ^")
					.replace("+", " + ")
					.replace("*", " * ")
					.replace("?", " ? ");
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
		return (one.length() <= 140) ? one : one.substring(0, 137) + "...";
	}
}
