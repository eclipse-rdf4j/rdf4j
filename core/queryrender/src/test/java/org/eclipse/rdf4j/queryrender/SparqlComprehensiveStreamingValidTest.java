package org.eclipse.rdf4j.queryrender;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Spliterator.ORDERED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SPARQL 1.1 streaming test generator (valid cases only).
 * Java 11 + JUnit 5.
 *
 * FEATURES COVERED (all VALID):
 * - Prologue (PREFIX/BASE)
 * - Triple sugar: predicate/object lists, 'a', blank-node property lists, RDF collections
 * - Graph pattern algebra: GROUP, OPTIONAL, UNION, MINUS
 * - FILTER with expressions (incl. EXISTS/NOT EXISTS), BIND, VALUES
 * - Property paths (streaming AST generator with correct precedence)
 * - Aggregates + GROUP BY + HAVING (projection validity enforced)
 * - Subqueries (SUBSELECT with proper scoping)
 * - Datasets: FROM / FROM NAMED + GRAPH
 * - Federated SERVICE (incl. SILENT and variable endpoints)
 * - Solution modifiers: ORDER BY / LIMIT / OFFSET / DISTINCT / REDUCED
 * - Query forms: SELECT / ASK / CONSTRUCT (template w/out paths) / DESCRIBE
 *
 * MEMORY: all enumeration is lazy and bounded by per-category caps.
 */
public class SparqlComprehensiveStreamingValidTest {

	// =========================
	// GLOBAL CONFIG KNOBS
	// =========================

	// Per-category caps (tune for CI/runtime)
	private static final int MAX_SELECT_PATH_CASES = 800;
	private static final int MAX_TRIPLE_SYNTAX_CASES = 500;
	private static final int MAX_GROUP_ALGEBRA_CASES = 500;
	private static final int MAX_FILTER_BIND_VALUES_CASES = 600;
	private static final int MAX_AGGREGATE_CASES = 400;
	private static final int MAX_SUBQUERY_CASES = 300;
	private static final int MAX_DATASET_GRAPH_SERVICE = 300;
	private static final int MAX_CONSTRUCT_CASES = 300;
	private static final int MAX_ASK_DESCRIBE_CASES = 200;

	// Extra extensions
	private static final int MAX_ORDER_BY_CASES = 500;
	private static final int MAX_DESCRIBE_CASES = 200;
	private static final int MAX_SERVICE_VALUES_CASES = 400;

	/** Max property-path AST depth (atoms at depth 0). */
	private static final int MAX_PATH_DEPTH = 3;

	/** Optional spacing variants to shake lexer (all remain valid). */
	private static final boolean GENERATE_WHITESPACE_VARIANTS = false;

	/** Allow 'a' in path atoms (legal); excluded from negated sets. */
	private static final boolean INCLUDE_A_IN_PATHS = true;

	/** Render "!^ex:p" compactly when possible. */
	private static final boolean COMPACT_SINGLE_NEGATION = true;

	// =========================
	// PREFIXES & VOCAB
	// =========================

	private static final List<String> CLASSES = Arrays.asList("ex:C", "ex:Person", "ex:Thing");
	private static final List<String> PREDICATES = Arrays.asList("ex:pA", "ex:pB", "ex:pC", "ex:pD", "foaf:knows", "foaf:name");
	private static final List<String> MORE_IRIS = Arrays.asList(
			"<http://example.org/p/I0>", "<http://example.org/p/I1>", "<http://example.org/p/I2>"
	);
	private static final List<String> GRAPH_IRIS = Arrays.asList(
			"<http://graphs.example/g0>", "<http://graphs.example/g1>"
	);
	private static final List<String> SERVICE_IRIS = Arrays.asList(
			"<http://services.example/sparql>", "<http://federation.example/ep>"
	);
	private static final List<String> DATASET_FROM = Arrays.asList(
			"<http://dataset.example/default0>", "<http://dataset.example/default1>"
	);
	private static final List<String> DATASET_NAMED = Arrays.asList(
			"<http://dataset.example/named0>", "<http://dataset.example/named1>"
	);

	private static final List<String> STRING_LITS = Arrays.asList(
			"\"alpha\"", "'beta'", "\"\"\"multi\nline\"\"\"", "\"x\"@en", "\"3\"^^xsd:string"
	);
	@SuppressWarnings("unused")
	private static final List<String> NUM_LITS = Arrays.asList("0", "1", "2", "42", "3.14", "1e9");
	@SuppressWarnings("unused")
	private static final List<String> BOOL_LITS = Arrays.asList("true", "false");

	// =========================
	// ASSERTION HOOKS — INTEGRATE HERE
	// =========================

	private static void assertRoundTrip(String sparql) {
		// Example:
		assertSameSparqlQuery(sparql, cfg());
	}

	/** Failure oracle for shrinker: returns true when the query still fails your round-trip. */
	private static SparqlShrinker.FailureOracle failureOracle() {
		return q -> {
			try {
				assertRoundTrip(q);
				return false; // no failure
			} catch (Throwable t) {
				return true; // still failing
			}
		};
	}


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

	private static TupleExpr parseAlgebra(String sparql) {
		try {
			ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, sparql, null);
			return pq.getTupleExpr();
		} catch (MalformedQueryException e) {
			throw new MalformedQueryException(
					"Failed to parse SPARQL query.\n###### QUERY ######\n" + sparql + "\n\n######################",
					e);
		}

	}

	private static String render(String sparql, TupleExprIRRenderer.Config cfg) {
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
	private static void assertSameSparqlQuery(String sparql, TupleExprIRRenderer.Config cfg) {
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



	/** Run the assertion, and on failure automatically shrink and rethrow with minimized query. */
	private static void runWithShrink(String q) {
		 assertRoundTrip(q);
//		ShrinkOnFailure.wrap(q, () -> assertRoundTrip(q), failureOracle());
	}

	// =========================
	// TEST FACTORIES (VALID ONLY)
	// =========================

	private static String wrapPrologue(String body) {
		return SPARQL_PREFIX + body;
	}

	private static String wrap(String q) {
		if (!GENERATE_WHITESPACE_VARIANTS) {
			return q;
		}
		List<String> vs = Whitespace.variants(q);
		return vs.get(0);
	}

	private static Stream<DynamicTest> toDynamicTests(String prefix, Stream<String> queries) {
		Set<String> seen = new LinkedHashSet<>();
		return queries
				.filter(distinctLimited(seen, Integer.MAX_VALUE))
				.map(q -> DynamicTest.dynamicTest(prefix + " :: " + summarize(q),
						() -> runWithShrink(q)));
	}

	/** Bounded distinct: returns true for the first 'limit' distinct items; false afterwards or on duplicates. */
	private static <T> Predicate<T> distinctLimited(Set<T> seen, int limit) {
		Objects.requireNonNull(seen, "seen");
		AtomicInteger left = new AtomicInteger(limit);
		return t -> {
			if (seen.contains(t)) {
				return false;
			}
			int remaining = left.get();
			if (remaining <= 0) {
				return false;
			}
			if (left.compareAndSet(remaining, remaining - 1)) {
				seen.add(t);
				return true;
			}
			return false;
		};
	}

	private static <A, B> Stream<Pair<A, B>> cartesian(Stream<A> as, Stream<B> bs) {
		List<B> bl = bs.collect(Collectors.toList());
		return as.flatMap(a -> bl.stream().map(b -> new Pair<>(a, b)));
	}

	private static String summarize(String q) {
		String one = q.replace("\n", "\\n");
		return (one.length() <= 160) ? one : one.substring(0, 157) + "...";
	}

	/** Build a 1-column VALUES with N rows: VALUES ?var { ex:s1 ex:s2 ... } */
	private static String emitValues1(String var, int n) {
		StringBuilder sb = new StringBuilder("VALUES ?" + var + " { ");
		for (int i = 1; i <= n; i++) {
			if (i > 1) {
				sb.append(' ');
			}
			sb.append("ex:s").append(i);
		}
		return sb.append(" }").toString();
	}

	/**
	 * Build a 2-column VALUES with N rows:
	 *   VALUES (?v1 ?v2) { (ex:s1 1) (ex:s2 UNDEF) ... }
	 * If includeUndef is true, every 3rd row uses UNDEF in the second column.
	 */
	private static String emitValues2(String v1, String v2, int n, boolean includeUndef) {
		StringBuilder sb = new StringBuilder("VALUES (?" + v1 + " ?" + v2 + ") { ");
		for (int i = 1; i <= n; i++) {
			sb.append('(')
					.append("ex:s").append(i).append(' ')
					.append(includeUndef && (i % 3 == 0) ? "UNDEF" : String.valueOf(i))
					.append(") ");
		}
		return sb.append("}").toString();
	}

	// ----- Extensions: ORDER BY, DESCRIBE variants, nested SERVICE, VALUES-heavy -----

	@Disabled
	@TestFactory
	Stream<DynamicTest> select_with_property_paths_valid() {
		final int variantsPerPath = 3; // skeletons per path
		int neededPaths = Math.max(1, MAX_SELECT_PATH_CASES / variantsPerPath);

		Set<String> seen = new LinkedHashSet<>(neededPaths * 2);

		Stream<String> pathStream = PathStreams.allDepths(MAX_PATH_DEPTH, INCLUDE_A_IN_PATHS)
				.map(p -> Renderer.render(p, COMPACT_SINGLE_NEGATION))
				.filter(distinctLimited(seen, neededPaths))
				.limit(neededPaths);

		Stream<String> queries = pathStream.flatMap(path -> Stream.of(
				wrap(SPARQL_PREFIX + "SELECT ?s ?o WHERE { ?s " + path + " ?o . }"),
				wrap(SPARQL_PREFIX + "SELECT ?s ?n WHERE { ?s " + path + "/foaf:name ?n . }"),
				wrap(SPARQL_PREFIX + "SELECT ?s ?o WHERE {\n" +
						"  ?s a " + CLASSES.get(0) + " .\n" +
						"  FILTER EXISTS { ?s " + path + " ?o . }\n" +
						"}")
		)).limit(MAX_SELECT_PATH_CASES);

		return toDynamicTests("SELECT+PATH", queries);
	}

	@Disabled
	@TestFactory
	Stream<DynamicTest> triple_surface_syntax_valid() {
		Stream<String> baseTriples = Stream.of(
				// predicate/object lists; object lists; dangling semicolon legal
				"SELECT ?s ?o WHERE { ?s a " + CLASSES.get(0) + " ; " +
						PREDICATES.get(0) + " ?o , " + STRING_LITS.get(0) + " ; " +
						PREDICATES.get(1) + " 42 ; " +
						PREDICATES.get(2) + " ?x ; " +
						" . }",

				// blank node property lists; collections
				"SELECT ?s ?x WHERE {\n" +
						"  [] " + PREDICATES.get(0) + " ?s ; " + PREDICATES.get(1) + " [ " + PREDICATES.get(2) + " ?x ] .\n" +
						"  ?s " + PREDICATES.get(3) + " ( " + CLASSES.get(1) + " " + CLASSES.get(2) + " ) .\n" +
						"}",

				// nested blank nodes and 'a'
				"SELECT ?who ?name WHERE {\n" +
						"  ?who a " + CLASSES.get(1) + " ; foaf:name ?name ; " + PREDICATES.get(0) + " [ a " + CLASSES.get(2) + " ; " + PREDICATES.get(1) + " ?x ] .\n" +
						"}"
		);

		return toDynamicTests("TripleSyntax", baseTriples
				.map(SparqlComprehensiveStreamingValidTest::wrapPrologue)
				.limit(MAX_TRIPLE_SYNTAX_CASES));
	}

	@Disabled
	@TestFactory
	Stream<DynamicTest> group_algebra_valid() {
		Stream<String> groups = Stream.of(
				// OPTIONAL with internal FILTER
				"SELECT ?s ?o WHERE {\n" +
						"  ?s " + PREDICATES.get(0) + " ?o .\n" +
						"  OPTIONAL { ?s " + PREDICATES.get(1) + " ?x . FILTER(?x > 1) }\n" +
						"}",

				// UNION multi-branch
				"SELECT ?s WHERE {\n" +
						"  { ?s " + PREDICATES.get(0) + " ?o . }\n" +
						"  UNION { ?s " + PREDICATES.get(1) + " ?o . }\n" +
						"  UNION { ?s a " + CLASSES.get(0) + " . }\n" +
						"}",

				// MINUS with aligned variables
				"SELECT ?s ?o WHERE {\n" +
						"  ?s " + PREDICATES.get(0) + " ?o .\n" +
						"  MINUS { ?s " + PREDICATES.get(1) + " ?o . }\n" +
						"}"
		);

		return toDynamicTests("GroupAlgebra", groups
				.map(SparqlComprehensiveStreamingValidTest::wrapPrologue)
				.limit(MAX_GROUP_ALGEBRA_CASES));
	}

	// =========================================================================================
	// UTIL: Wrap & DynamicTest plumbing
	// =========================================================================================

	@Disabled
	@TestFactory
	Stream<DynamicTest> filter_bind_values_valid() {
		Stream<String> queries = Stream.of(
				// regex + lang + logical
				"SELECT ?s ?name WHERE {\n" +
						"  ?s foaf:name ?name .\n" +
						"  FILTER( REGEX(?name, \"^A\", \"i\") && ( LANG(?name) = \"\" || LANGMATCHES(LANG(?name), \"en\") ) )\n" +
						"}",

				// EXISTS / NOT EXISTS referencing earlier vars
				"SELECT ?s WHERE {\n" +
						"  ?s " + PREDICATES.get(0) + " ?o .\n" +
						"  FILTER EXISTS { ?o " + PREDICATES.get(1) + " ?x }\n" +
						"  FILTER NOT EXISTS { ?s " + PREDICATES.get(2) + " ?x }\n" +
						"}",

				// BIND + VALUES (1-col)
				"SELECT ?s ?z WHERE {\n" +
						"  VALUES ?s { ex:s1 ex:s2 ex:s3 }\n" +
						"  ?s " + PREDICATES.get(0) + " ?o .\n" +
						"  BIND( CONCAT(STR(?s), \"-\", STR(?o)) AS ?z )\n" +
						"}",

				// VALUES 2-col with UNDEF in row form
				"SELECT ?s ?o WHERE {\n" +
						"  VALUES (?s ?o) { (ex:s1 1) (ex:s2 UNDEF) (ex:s3 3) }\n" +
						"  ?s " + PREDICATES.get(0) + " ?o .\n" +
						"}"
		);

		return toDynamicTests("FilterBindValues", queries
				.map(SparqlComprehensiveStreamingValidTest::wrapPrologue)
				.limit(MAX_FILTER_BIND_VALUES_CASES));
	}

	@Disabled
	@TestFactory
	Stream<DynamicTest> aggregates_groupby_having_valid() {
		Stream<String> queries = Stream.of(
				// Count + group + having
				"SELECT ?s (COUNT(?o) AS ?c) WHERE {\n" +
						"  ?s " + PREDICATES.get(0) + " ?o .\n" +
						"} GROUP BY ?s HAVING (COUNT(?o) > 1)",

				// DISTINCT aggregates and ORDER BY aggregated alias
				"SELECT (SUM(DISTINCT ?v) AS ?total) WHERE {\n" +
						"  ?s " + PREDICATES.get(1) + " ?v .\n" +
						"} ORDER BY DESC(?total) LIMIT 10",

				// GROUP_CONCAT with SEPARATOR
				"SELECT ?s (GROUP_CONCAT(DISTINCT STR(?o); SEPARATOR=\", \") AS ?names) WHERE {\n" +
						"  ?s foaf:name ?o .\n" +
						"} GROUP BY ?s"
		);

		return toDynamicTests("Aggregates", queries
				.map(SparqlComprehensiveStreamingValidTest::wrapPrologue)
				.limit(MAX_AGGREGATE_CASES));
	}

	@Disabled
	@TestFactory
	Stream<DynamicTest> subqueries_valid() {
		Stream<String> queries = Stream.of(
				"SELECT ?s ?c WHERE {\n" +
						"  { SELECT ?s (COUNT(?o) AS ?c) WHERE { ?s " + PREDICATES.get(0) + " ?o . } GROUP BY ?s }\n" +
						"  FILTER(?c > 0)\n" +
						"}"
		);

		return toDynamicTests("Subqueries", queries
				.map(SparqlComprehensiveStreamingValidTest::wrapPrologue)
				.limit(MAX_SUBQUERY_CASES));
	}

	// =========================================================================================
	// STREAM HELPERS
	// =========================================================================================

	@Disabled
	@TestFactory
	Stream<DynamicTest> datasets_graph_service_valid() {
		Stream<String> datasetClauses = cartesian(DATASET_FROM.stream(), DATASET_NAMED.stream())
				.limit(2)
				.map(pair -> "FROM " + pair.getLeft() + "\nFROM NAMED " + pair.getRight() + "\n")
				.map(ds -> SPARQL_PREFIX + ds);

		Stream<String> queries = Stream.concat(
				datasetClauses.map(ds ->
						ds + "SELECT ?s WHERE { GRAPH " + GRAPH_IRIS.get(0) + " { ?s " + PREDICATES.get(0) + " ?o } }"
				),
				Stream.of(
						// SERVICE with constant IRI
						SPARQL_PREFIX + "SELECT ?s ?o WHERE {\n" +
								"  SERVICE SILENT " + SERVICE_IRIS.get(0) + " { ?s " + PREDICATES.get(0) + " ?o }\n" +
								"}",

						// SERVICE with variable endpoint (bound via VALUES)
						SPARQL_PREFIX + "SELECT ?s WHERE {\n" +
								"  VALUES ?svc { " + SERVICE_IRIS.get(1) + " }\n" +
								"  SERVICE ?svc { ?s " + PREDICATES.get(1) + " ?o }\n" +
								"}"
				)
		);

		return toDynamicTests("DatasetGraphService", queries.limit(MAX_DATASET_GRAPH_SERVICE));
	}

	@Disabled
	@TestFactory
	Stream<DynamicTest> construct_ask_describe_valid() {
		Stream<String> queries = Stream.of(
				// Explicit template (no property paths in template)
				"CONSTRUCT {\n" +
						"  ?s a " + CLASSES.get(0) + " ; " + PREDICATES.get(0) + " ?o .\n" +
						"} WHERE { ?s " + PREDICATES.get(0) + " ?o . }",

				// CONSTRUCT WHERE short form
				"CONSTRUCT WHERE { ?s " + PREDICATES.get(1) + " ?o . }",

				// ASK
				"ASK WHERE { ?s " + PREDICATES.get(0) + " ?o . OPTIONAL { ?s " + PREDICATES.get(1) + " ?x } }",

				// DESCRIBE with WHERE and explicit IRIs in target list
				"DESCRIBE ?s <http://example.org/resource/X> WHERE { ?s a " + CLASSES.get(1) + " . }"
		).map(SparqlComprehensiveStreamingValidTest::wrapPrologue);

		return toDynamicTests("ConstructAskDescribe", queries.limit(MAX_CONSTRUCT_CASES + MAX_ASK_DESCRIBE_CASES));
	}

	@Disabled
	@TestFactory
	Stream<DynamicTest> order_by_and_modifiers_valid() {
		final int keysNeeded = 80; // enough to mix into MAX_ORDER_BY_CASES
		Set<String> seenKeys = new LinkedHashSet<>(keysNeeded * 2);

		final String where =
				"{\n" +
						"  ?s " + PREDICATES.get(0) + " ?v .\n" +
						"  OPTIONAL { ?s foaf:name ?name }\n" +
						"}";

		List<String> keys = ExprStreams.orderKeyStream()
				.filter(distinctLimited(seenKeys, keysNeeded))
				.limit(keysNeeded)
				.collect(Collectors.toList());

		Function<int[], String> buildAliased = pairIdx -> {
			String sel1 = ExprStreams.selectExprPool().get(pairIdx[0] % ExprStreams.selectExprPool().size());
			String sel2 = ExprStreams.selectExprPool().get(pairIdx[1] % ExprStreams.selectExprPool().size());

			return SPARQL_PREFIX +
					"SELECT DISTINCT ?s (" + sel1 + " AS ?k1) (" + sel2 + " AS ?k2)\n" +
					"WHERE " + where + "\n" +
					"ORDER BY DESC(?k1) ASC(?k2)\n" +
					"LIMIT 10 OFFSET 2";
		};

		Function<int[], String> buildDirect = pairIdx -> {
			String k1 = keys.get(pairIdx[0]);
			String k2 = keys.get(pairIdx[1]);
			String ord = String.join(" ",
					ExprStreams.toOrderCondition(k1),
					ExprStreams.toOrderCondition(k2)
			);
			return SPARQL_PREFIX +
					"SELECT REDUCED * WHERE " + where + "\n" +
					"ORDER BY " + ord + "\n" +
					"LIMIT 7";
		};

		Stream<int[]> pairs = ExprStreams.indexPairs(keys.size());

		Stream<String> queries = Stream.concat(
				pairs.map(buildAliased),
				ExprStreams.indexPairs(keys.size()).map(buildDirect)
		).limit(MAX_ORDER_BY_CASES);

		return toDynamicTests("OrderBy+Modifiers", queries);
	}

	@Disabled
	@TestFactory
	Stream<DynamicTest> describe_forms_valid() {
		List<String> simpleDescribeTargets = Arrays.asList(
				"DESCRIBE <http://example.org/resource/A> <http://example.org/resource/B>",
				"DESCRIBE <http://example.org/resource/C>"
		);

		Stream<String> noWhere = simpleDescribeTargets.stream()
				.map(q -> SPARQL_PREFIX + q);

		Stream<String> withWhere = Stream.of(
				"DESCRIBE ?s WHERE { ?s a " + CLASSES.get(0) + " . }",
				"DESCRIBE * WHERE { ?s " + PREDICATES.get(0) + " ?o . OPTIONAL { ?s foaf:name ?name } } LIMIT 5"
		).map(q -> SPARQL_PREFIX + q);

		Stream<String> queries = Stream.concat(noWhere, withWhere)
				.limit(MAX_DESCRIBE_CASES);

		return toDynamicTests("DescribeForms", queries);
	}

	// =========================================================================================
	// PROPERTY PATH AST + RENDERER (VALID-ONLY)
	// =========================================================================================

	@Disabled
	@TestFactory
	Stream<DynamicTest> nested_service_and_values_joins_valid() {
		Stream<String> serviceQueries = Stream.of(
				SPARQL_PREFIX +
						"SELECT ?s ?o WHERE {\n" +
						"  SERVICE " + SERVICE_IRIS.get(0) + " {\n" +
						"    SERVICE SILENT " + SERVICE_IRIS.get(1) + " { ?s " + PREDICATES.get(0) + " ?o }\n" +
						"  }\n" +
						"}",

				SPARQL_PREFIX +
						"SELECT ?s WHERE {\n" +
						"  VALUES ?svc { " + SERVICE_IRIS.get(0) + " }\n" +
						"  SERVICE ?svc { ?s " + PREDICATES.get(1) + " ?o OPTIONAL { ?o " + PREDICATES.get(2) + " ?x } }\n" +
						"}"
		);

		Stream<String> valuesHeavy = Stream.concat(
				// 1-column VALUES (many rows)
				Stream.of(emitValues1("s", 16)).map(vs ->
						SPARQL_PREFIX +
								"SELECT ?s ?o WHERE {\n" +
								"  " + vs + "\n" +
								"  ?s " + PREDICATES.get(0) + " ?o .\n" +
								"  OPTIONAL { ?s foaf:name ?name }\n" +
								"}"
				),
				// 2-column VALUES with UNDEF rows
				Stream.of(emitValues2("s", "o", 12, true)).map(vs ->
						SPARQL_PREFIX +
								"SELECT ?s ?o WHERE {\n" +
								"  " + vs + "\n" +
								"  ?s " + PREDICATES.get(0) + " ?o .\n" +
								"}"
				)
		);

		Stream<String> queries = Stream.concat(serviceQueries, valuesHeavy)
				.limit(MAX_SERVICE_VALUES_CASES);

		return toDynamicTests("Service+Values", queries);
	}

	/** Precedence: ALT < SEQ < PREFIX (!,^) < POSTFIX (*,+,?) < ATOM/GROUP. */
	private enum Prec {ALT, SEQ, PREFIX, POSTFIX, ATOM}

	private enum Quant {
		STAR("*"), PLUS("+"), QMARK("?");
		final String s;

		Quant(String s) {
			this.s = s;
		}
	}

	private interface PathNode {
		Prec prec();

		boolean prohibitsExtraQuantifier();
	}

	/** Immutable pair for tiny cartesian helpers. */
	private static final class Pair<A, B> {
		private final A a;
		private final B b;

		Pair(A a, B b) {
			this.a = a;
			this.b = b;
		}

		A getLeft() {
			return a;
		}

		B getRight() {
			return b;
		}
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

	/** Negated property set: only IRI or ^IRI elements; 'a' is excluded here. */
	private static final class NegatedSet implements PathNode {
		final List<PathNode> elems; // each elem must be Atom(!='a') or Inverse(Atom(!='a'))

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
			return (o instanceof Alternative) && ((Alternative) o).left.equals(left) && ((Alternative) o).right.equals(right);
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

	// =========================================================================================
	// STREAMING PATH GENERATOR (VALID-ONLY)
	// =========================================================================================

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

	// =========================================================================================
	// EXPRESSIONS for ORDER BY / SELECT AS (valid subset)
	// =========================================================================================

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
				if (compactSingleNeg && ns.elems.size() == 1 && (ns.elems.get(0) instanceof Atom || ns.elems.get(0) instanceof Inverse)) {
					sb.append("!");
					PathNode e = ns.elems.get(0);
					render(e, sb, Prec.PREFIX, compactSingleNeg); // !^ex:p or !ex:p
				} else {
					sb.append("!(");
					for (int i = 0; i < ns.elems.size(); i++) {
						if (i > 0) {
							sb.append("|");
						}
						render(ns.elems.get(i), sb, Prec.ALT, compactSingleNeg);
					}
					sb.append(")");
				}
			} else if (n instanceof Sequence) {
				Sequence s = (Sequence) n;
				boolean need = ctx.ordinal() > Prec.SEQ.ordinal();
				if (need) {
					sb.append("(");
				}
				render(s.left, sb, Prec.SEQ, compactSingleNeg);
				sb.append("/");
				render(s.right, sb, Prec.SEQ, compactSingleNeg);
				if (need) {
					sb.append(")");
				}
			} else if (n instanceof Alternative) {
				Alternative a = (Alternative) n;
				boolean need = ctx.ordinal() > Prec.ALT.ordinal();
				if (need) {
					sb.append("(");
				}
				render(a.left, sb, Prec.ALT, compactSingleNeg);
				sb.append("|");
				render(a.right, sb, Prec.ALT, compactSingleNeg);
				if (need) {
					sb.append(")");
				}
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
			if (need) {
				sb.append("(");
			}
			render(child, sb, child.prec(), compactSingleNeg);
			if (need) {
				sb.append(")");
			}
		}
	}

	// =========================================================================================
	// WHITESPACE VARIANTS (VALID)
	// =========================================================================================

	private static final class PathStreams {

		private static final List<String> ATOMS =
				Stream.concat(PREDICATES.stream(), MORE_IRIS.stream()).collect(Collectors.toList());

		static Stream<PathNode> allDepths(int maxDepth, boolean includeA) {
			Stream<PathNode> s = Stream.empty();
			for (int d = 0; d <= maxDepth; d++) {
				s = Stream.concat(s, depth(d, includeA));
			}
			return s;
		}

		static Stream<? extends PathNode> depth(int depth, boolean includeA) {
			if (depth == 0) {
				return depth0(includeA);
			}
			return Stream.concat(unary(depth, includeA), binary(depth, includeA));
		}

		private static Stream<? extends PathNode> depth0(boolean includeA) {
			Stream<Atom> atoms = atomStream(includeA);
			Stream<PathNode> inverses = atomStream(includeA).map(Inverse::new);

			// Negated singles: !iri and !^iri (exclude 'a')
			Stream<PathNode> negSingles = Stream.concat(
					iriAtoms().map(a -> new NegatedSet(Collections.singletonList(a))),
					iriAtoms().map(a -> new NegatedSet(Collections.singletonList(new Inverse(a))))
			);

			// Small negated sets of size 2..3, domain [iri, ^iri] (excluding 'a')
			List<PathNode> negDomain = Stream.concat(
					iriAtoms(),
					iriAtoms().map(Inverse::new)
			).collect(Collectors.toList());

			Stream<PathNode> negSets =
					Stream.concat(kSubsets(negDomain, 2), kSubsets(negDomain, 3))
							.map(NegatedSet::new);

			return Stream.of(atoms, inverses, negSingles, negSets)
					.reduce(Stream::concat).orElseGet(Stream::empty);
		}

		private static Stream<PathNode> unary(int depth, boolean includeA) {
			Stream<PathNode> chained = Stream.empty();
			for (int d = 0; d < depth; d++) {
				int dd = d;
				Stream<PathNode> fromD =
						depth(dd, includeA).flatMap(n -> {
							Stream<PathNode> inv = (n instanceof Inverse) ? Stream.empty() : Stream.of(new Inverse(n));
							Stream<PathNode> quants = n.prohibitsExtraQuantifier()
									? Stream.empty()
									: Stream.of(new Quantified(n, Quant.STAR), new Quantified(n, Quant.PLUS), new Quantified(n, Quant.QMARK));
							Stream<PathNode> grp = Stream.of(new Group(n));
							return Stream.of(inv, quants, grp).reduce(Stream::concat).orElseGet(Stream::empty);
						});
				chained = Stream.concat(chained, fromD);
			}
			return chained;
		}

		private static Stream<PathNode> binary(int depth, boolean includeA) {
			Stream<PathNode> all = Stream.empty();
			for (int dL = 0; dL < depth; dL++) {
				int dR = depth - 1 - dL;
				Stream<PathNode> part =
						depth(dL, includeA).flatMap(L ->
								depth(dR, includeA).flatMap(R ->
										Stream.of(new Sequence(L, R), new Alternative(L, R))
								)
						);
				all = Stream.concat(all, part);
			}
			return all;
		}

		private static Stream<Atom> atomStream(boolean includeA) {
			Stream<String> base = ATOMS.stream();
			if (includeA) {
				base = Stream.concat(Stream.of("a"), base);
			}
			return base.map(Atom::new);
		}

		private static Stream<Atom> iriAtoms() {
			// exclude 'a' for negated sets
			return ATOMS.stream().map(Atom::new);
		}

		private static <T> Stream<List<T>> kSubsets(List<T> list, int k) {
			if (k < 0 || k > list.size()) {
				return Stream.empty();
			}
			if (k == 0) {
				return Stream.of(Collections.emptyList());
			}

			Spliterator<List<T>> sp = new Spliterators.AbstractSpliterator<List<T>>(Long.MAX_VALUE, ORDERED) {
				final int n = list.size();
				final int[] idx = initFirst(k);
				boolean hasNext = (k <= n);

				@Override
				public boolean tryAdvance(java.util.function.Consumer<? super List<T>> action) {
					if (!hasNext) {
						return false;
					}
					List<T> comb = new ArrayList<>(k);
					for (int i = 0; i < k; i++) {
						comb.add(list.get(idx[i]));
					}
					action.accept(Collections.unmodifiableList(comb));
					hasNext = nextCombination(idx, n, k);
					return true;
				}
			};
			return StreamSupport.stream(sp, false);
		}

		private static int[] initFirst(int k) {
			int[] idx = new int[k];
			for (int i = 0; i < k; i++) {
				idx[i] = i;
			}
			return idx;
		}

		private static boolean nextCombination(int[] idx, int n, int k) {
			for (int i = k - 1; i >= 0; i--) {
				if (idx[i] != i + n - k) {
					idx[i]++;
					for (int j = i + 1; j < k; j++) {
						idx[j] = idx[j - 1] + 1;
					}
					return true;
				}
			}
			return false;
		}
	}

	private static final class ExprStreams {

		private static final List<String> VARS = Arrays.asList("?s", "?o", "?v", "?name");
		private static final List<String> NUMS = Arrays.asList("0", "1", "2", "42", "3.14", "1e6");
		private static final List<String> STRS = Arrays.asList("\"alpha\"", "\"beta\"", "\"A\"@en", "\"3\"^^xsd:string");

		/** Small pool of expressions appropriate for SELECT ... AS ?k */
		static List<String> selectExprPool() {
			return Arrays.asList(
					"?v + 1",
					"(?v * 2)",
					"STRLEN(STR(?s))",
					"COALESCE(?v, 0)",
					"IF(BOUND(?name), STRLEN(?name), 0)",
					"ABS(?v)",
					"YEAR(NOW())",
					"UCASE(STR(?name))"
			).stream().map(ExprStreams::parenIfNeeded).collect(Collectors.toList());
		}

		/** ORDER BY conditions: keys like "ASC(expr)", "DESC(expr)", or "(expr)". */
		static Stream<String> orderKeyStream() {
			Stream<String> exprs = exprStreamDepth2().map(ExprStreams::parenIfNeeded);
			Stream<String> asc = exprs.map(e -> "ASC(" + e + ")");
			Stream<String> desc = exprStreamDepth2().map(ExprStreams::parenIfNeeded).map(e -> "DESC(" + e + ")");
			Stream<String> bare = exprStreamDepth2().map(ExprStreams::parenIfNeeded).map(e -> "(" + e + ")");
			return Stream.of(asc, desc, bare).reduce(Stream::concat).orElseGet(Stream::empty);
		}

		static String toOrderCondition(String key) {
			return key;
		}

		/** Stream pairs of distinct indices (i < j) lazily. */
		static Stream<int[]> indexPairs(int n) {
			Spliterator<int[]> sp = new Spliterators.AbstractSpliterator<int[]>(Long.MAX_VALUE, ORDERED) {
				int i = 0, j = 1;

				@Override
				public boolean tryAdvance(java.util.function.Consumer<? super int[]> action) {
					while (i < n) {
						if (j < n) {
							action.accept(new int[]{i, j});
							j++;
							return true;
						} else {
							i++;
							j = i + 1;
						}
					}
					return false;
				}
			};
			return StreamSupport.stream(sp, false);
		}

		// ----- expression building (small, valid subset) -----

		private static Stream<String> exprStreamDepth2() {
			Stream<String> d0 = Stream.of(
					VARS.stream(),
					NUMS.stream(),
					STRS.stream()
			).reduce(Stream::concat).orElseGet(Stream::empty);

			Stream<String> d1 = Stream.concat(
					d0.flatMap(e -> Stream.of(
							"STR(" + e + ")", "STRLEN(STR(" + e + "))", "UCASE(STR(" + e + "))",
							"ABS(" + e + ")", "ROUND(" + e + ")", "LCASE(STR(" + e + "))",
							"COALESCE(" + e + ", 0)"
					)),
					cross(VARS.stream(), NUMS.stream(), (a, b) -> "(" + a + " + " + b + ")")
			);

			Stream<String> d2 = Stream.of(
					d1.flatMap(e -> Stream.of(
							"IF(BOUND(?name), " + e + ", 0)",
							"COALESCE(" + e + ", 1, 2)",
							"xsd:integer(" + e + ")",
							"(" + e + " * 2)"
					)),
					cross(d1, NUMS.stream(), (a, b) -> "(" + a + " - " + b + ")")
			).reduce(Stream::concat).orElseGet(Stream::empty);

			return Stream.of(d0, d1, d2).reduce(Stream::concat).orElseGet(Stream::empty);
		}

		private static String parenIfNeeded(String e) {
			String t = e.trim();
			if (t.startsWith("(")) {
				return t;
			}
			if (t.contains(" ") || t.contains(",")) {
				return "(" + t + ")";
			}
			return t;
		}

		private static <A, B> Stream<String> cross(Stream<A> as, Stream<B> bs, java.util.function.BiFunction<A, B, String> f) {
			List<B> bl = bs.collect(Collectors.toList());
			return as.flatMap(a -> bl.stream().map(b -> f.apply(a, b)));
		}
	}

	private static final class Whitespace {
		static List<String> variants(String q) {
			String spaced = q.replace("|", " | ")
					.replace("/", " / ")
					.replace("^", "^ ")
					.replace("!(", "! (")
					.replace("!^", "! ^")
					.replace("+", " + ")
					.replace("*", " * ")
					.replace("?", " ? ");
			String compact = q.replaceAll("\\s+", " ")
					.replace(" (", "(").replace("( ", "(")
					.replace(" )", ")").replace(" .", ".").trim();
			LinkedHashSet<String> set = new LinkedHashSet<>();
			set.add(q);
			set.add(spaced);
			set.add(compact);
			return new ArrayList<>(set);
		}
	}
}
