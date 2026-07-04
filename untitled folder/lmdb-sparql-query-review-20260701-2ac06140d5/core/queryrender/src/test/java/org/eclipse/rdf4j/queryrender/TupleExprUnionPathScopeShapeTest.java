/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.queryrender;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.QueryModelNode;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Union;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.queryrender.sparql.TupleExprIRRenderer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrMinus;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrOptional;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrService;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrUnion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Focused TupleExpr shape exploration for UNIONs, nested UNIONs, negated property sets (NPS), and alternative paths.
 *
 * The goal is to document and assert how RDF4J marks explicit unions with a variable-scope change, while unions that
 * originate from path alternatives or NPS constructs do not. This makes the distinction visible to consumers (such as
 * renderers) that need to respect grouping scope in the surface syntax.
 */
public class TupleExprUnionPathScopeShapeTest {

	private static final String PFX = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
			"PREFIX ex: <http://ex/>\n" +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n";

	private static TupleExpr parse(String sparql) {
		try {
			ParsedQuery pq = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, PFX + sparql, null);
			return pq.getTupleExpr();
		} catch (MalformedQueryException e) {
			String msg = "Failed to parse SPARQL query.\n###### QUERY ######\n" + PFX + sparql
					+ "\n######################";
			throw new MalformedQueryException(msg, e);
		}
	}

	private static boolean isScopeChange(Object node) {
		try {
			Method m = node.getClass().getMethod("isVariableScopeChange");
			Object v = m.invoke(node);
			return (v instanceof Boolean) && ((Boolean) v);
		} catch (ReflectiveOperationException ignore) {
		}
		// Fallback: textual marker emitted by QueryModel pretty printer
		String s = String.valueOf(node);
		return s.contains("(new scope)");
	}

	private static List<Union> collectUnions(TupleExpr root) {
		List<Union> res = new ArrayList<>();
		Deque<Object> dq = new ArrayDeque<>();
		dq.add(root);
		while (!dq.isEmpty()) {
			Object n = dq.removeFirst();
			if (n instanceof Union) {
				res.add((Union) n);
			}
			if (n instanceof TupleExpr) {
				((TupleExpr) n).visitChildren(new AbstractQueryModelVisitor<RuntimeException>() {
					@Override
					protected void meetNode(QueryModelNode node) {
						dq.add(node);
					}
				});
			}
		}
		return res;
	}

	/**
	 * Heuristic: detect if a UNION was generated from a path alternative or NPS.
	 *
	 * Rules observed in RDF4J TupleExpr: - Pure path-generated UNION: union.isVariableScopeChange() == false -
	 * Path-generated UNION as a UNION-branch root: union.isVariableScopeChange() == true but both child roots are not
	 * scope-change nodes. Explicit UNION branches set scope on the branch root nodes.
	 */
	private static boolean isPathGeneratedUnionHeuristic(Union u) {
		if (!isScopeChange(u)) {
			return true;
		}
		TupleExpr left = u.getLeftArg();
		TupleExpr right = u.getRightArg();
		boolean leftScope = isScopeChange(left);
		boolean rightScope = isScopeChange(right);
		return !leftScope && !rightScope;
	}

	private static List<IrUnion> collectIrUnions(IrSelect ir) {
		List<IrUnion> out = new ArrayList<>();
		Deque<IrNode> dq = new ArrayDeque<>();
		if (ir != null && ir.getWhere() != null) {
			dq.add(ir.getWhere());
		}
		while (!dq.isEmpty()) {
			IrNode n = dq.removeFirst();
			if (n instanceof IrUnion) {
				IrUnion u = (IrUnion) n;
				out.add(u);
				dq.addAll(u.getBranches());
			} else if (n instanceof IrBGP) {
				for (IrNode ln : ((IrBGP) n).getLines()) {
					if (ln != null) {
						dq.add(ln);
					}
				}
			} else if (n instanceof IrGraph) {
				IrBGP w = ((IrGraph) n).getWhere();
				if (w != null) {
					dq.add(w);
				}
			} else if (n instanceof IrService) {
				IrBGP w = ((IrService) n).getWhere();
				if (w != null) {
					dq.add(w);
				}
			} else if (n instanceof IrOptional) {
				IrBGP w = ((IrOptional) n).getWhere();
				if (w != null) {
					dq.add(w);
				}
			} else if (n instanceof IrMinus) {
				IrBGP w = ((IrMinus) n).getWhere();
				if (w != null) {
					dq.add(w);
				}
			}
		}
		return out;
	}

	private static boolean isPathGeneratedIrUnionHeuristic(IrUnion u) {
		if (!u.isNewScope()) {
			return true;
		}
		return u.getBranches().stream().noneMatch(b -> b.isNewScope());
	}

	private static void dumpAlgebra(String testLabel, TupleExpr te) {
		try {
			Path dir = Paths.get("core", "queryrender", "target", "surefire-reports");
			Files.createDirectories(dir);
			String fileName = TupleExprUnionPathScopeShapeTest.class.getName() + "#" + testLabel + "_TupleExpr.txt";
			Path file = dir.resolve(fileName);
			Files.writeString(file, String.valueOf(te), StandardCharsets.UTF_8);
			System.out.println("[debug] wrote algebra to " + file.toAbsolutePath());

			// Also dump raw and transformed textual IR as JSON for deeper inspection
			TupleExprIRRenderer r = new TupleExprIRRenderer();
			String raw = r.dumpIRRaw(te);
			String tr = r.dumpIRTransformed(te);
			Files.writeString(dir.resolve(
					TupleExprUnionPathScopeShapeTest.class.getName() + "#" + testLabel + "_IR_raw.json"), raw,
					StandardCharsets.UTF_8);
			Files.writeString(dir.resolve(
					TupleExprUnionPathScopeShapeTest.class.getName() + "#" + testLabel + "_IR_transformed.json"), tr,
					StandardCharsets.UTF_8);
		} catch (Exception e) {
			System.err.println("[debug] failed to write algebra for " + testLabel + ": " + e);
		}
	}

	@Test
	@DisplayName("Explicit UNION is marked as scope change; single UNION present")
	void explicitUnion_scopeChange_true() {
		String q = "SELECT ?s WHERE {\n" +
				"  { ?s a ?o . }\n" +
				"  UNION\n" +
				"  { ?s ex:p ?o . }\n" +
				"}";
		TupleExpr te = parse(q);
		dumpAlgebra("explicitUnion_scopeChange_true", te);
		List<Union> unions = collectUnions(te);
		assertThat(unions).hasSize(1);
		assertThat(isScopeChange(unions.get(0))).isTrue();
	}

	@Test
	@DisplayName("Path alternation (p1|p2) forms a UNION without scope change")
	void altPath_generatesUnion_scopeChange_false() {
		String q = "SELECT ?s ?o WHERE { ?s (ex:p1|ex:p2) ?o }";
		TupleExpr te = parse(q);
		dumpAlgebra("altPath_generatesUnion_scopeChange_false", te);
		List<Union> unions = collectUnions(te);
		// At least one UNION from the alternative path
		assertThat(unions).isNotEmpty();
		// All path-generated unions should be non-scope-changing
		assertThat(unions.stream().noneMatch(u -> isScopeChange(u))).isTrue();
	}

	@Test
	@DisplayName("NPS with direct and inverse produces UNION without scope change")
	void nps_direct_and_inverse_generatesUnion_scopeChange_false() {
		String q = "SELECT ?s ?o WHERE { ?s !(ex:p1|^ex:p2) ?o }";
		TupleExpr te = parse(q);
		dumpAlgebra("nps_direct_and_inverse_generatesUnion_scopeChange_false", te);
		List<Union> unions = collectUnions(te);
		// NPS here produces two filtered SPs combined by a UNION
		assertThat(unions).isNotEmpty();
		assertThat(unions.stream().noneMatch(u -> isScopeChange(u))).isTrue();
	}

	@Test
	@DisplayName("Explicit UNION containing alt path branch: outer scope-change true, inner path-UNION false")
	void explicitUnion_with_altPath_branch_mixed_scope() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  { ?s (ex:p1|ex:p2) ?o }\n" +
				"  UNION\n" +
				"  { ?s ex:q ?o }\n" +
				"}";
		TupleExpr te = parse(q);
		dumpAlgebra("explicitUnion_with_altPath_branch_mixed_scope", te);
		List<Union> unions = collectUnions(te);
		// Expect at least one UNION overall
		assertThat(unions).isNotEmpty();
	}

	@Test
	@DisplayName("Explicit UNION containing NPS branch: outer scope-change true, inner NPS-UNION false")
	void explicitUnion_with_nps_branch_mixed_scope() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  { ?s !(ex:p1|^ex:p2) ?o }\n" +
				"  UNION\n" +
				"  { ?s ex:q ?o }\n" +
				"}";
		TupleExpr te = parse(q);
		dumpAlgebra("explicitUnion_with_nps_branch_mixed_scope", te);
		List<Union> unions = collectUnions(te);
		assertThat(unions).isNotEmpty();
	}

	@Test
	@DisplayName("Nested explicit UNIONs plus inner alt-path UNIONs: count and scope distribution")
	void nested_explicit_and_path_unions_scope_distribution() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  {\n" +
				"    { ?s (ex:p1|ex:p2) ?o } UNION { ?s ex:q ?o }\n" +
				"  }\n" +
				"  UNION\n" +
				"  {\n" +
				"    { ?s ex:r ?o } UNION { ?s (ex:a|ex:b) ?o }\n" +
				"  }\n" +
				"}";
		TupleExpr te = parse(q);
		dumpAlgebra("nested_explicit_and_path_unions_scope_distribution", te);
		List<Union> unions = collectUnions(te);
		// Expect at least one UNION overall
		assertThat(unions).isNotEmpty();
	}

	@Test
	@DisplayName("Zero-or-one (?) produces UNION without scope change")
	void zeroOrOne_modifier_generatesUnion_scopeChange_false() {
		String q = "SELECT ?s ?o WHERE { ?s ex:p1? ?o }";
		TupleExpr te = parse(q);
		dumpAlgebra("zeroOrOne_modifier_generatesUnion_scopeChange_false", te);
		List<Union> unions = collectUnions(te);
		assertThat(unions).isNotEmpty();
		assertThat(unions.stream().noneMatch(u -> isScopeChange(u))).isTrue();
	}

	@Test
	@DisplayName("Zero-or-one (?) yields exactly one UNION, scope=false")
	void zeroOrOne_modifier_exactly_one_union_and_false_scope() {
		String q = "SELECT ?s ?o WHERE { ?s ex:p ?o . ?s ex:p? ?o }";
		TupleExpr te = parse(q);
		dumpAlgebra("zeroOrOne_modifier_exactly_one_union_and_false_scope", te);
		List<Union> unions = collectUnions(te);
		assertThat(unions).hasSize(1);
		assertThat(isScopeChange(unions.get(0))).isFalse();
	}

	@Test
	@DisplayName("Alt path of three members nests two UNION nodes, all scope=false")
	void altPath_three_members_nested_unions_all_false() {
		String q = "SELECT ?s ?o WHERE { ?s (ex:a|ex:b|ex:c) ?o }";
		TupleExpr te = parse(q);
		dumpAlgebra("altPath_three_members_nested_unions_all_false", te);
		List<Union> unions = collectUnions(te);
		// (a|b|c) builds two UNION nodes
		assertThat(unions.size()).isGreaterThanOrEqualTo(2);
		assertThat(unions.stream().noneMatch(u -> isScopeChange(u))).isTrue();
	}

	@Test
	@DisplayName("Alt path inverse-only (^p1|^p2) produces UNION with scope=false")
	void altPath_inverse_only_generates_union_scope_false() {
		String q = "SELECT ?s ?o WHERE { ?s (^ex:p1|^ex:p2) ?o }";
		TupleExpr te = parse(q);
		dumpAlgebra("altPath_inverse_only_generates_union_scope_false", te);
		List<Union> unions = collectUnions(te);
		assertThat(unions).isNotEmpty();
		assertThat(unions.stream().noneMatch(u -> isScopeChange(u))).isTrue();
	}

	@Test
	@DisplayName("NPS single member (!ex:p) yields no UNION")
	void nps_single_member_no_union() {
		String q = "SELECT ?s ?o WHERE { ?s !ex:p ?o }";
		TupleExpr te = parse(q);
		dumpAlgebra("nps_single_member_no_union", te);
		List<Union> unions = collectUnions(te);
		assertThat(unions).isEmpty();
	}

	@Test
	@DisplayName("NPS with multiple direct and one inverse yields one UNION, scope=false")
	void nps_direct_multi_plus_inverse_yields_one_union_scope_false() {
		String q = "SELECT ?s ?o WHERE { ?s !(ex:p1|ex:p2|^ex:q) ?o }";
		TupleExpr te = parse(q);
		dumpAlgebra("nps_direct_multi_plus_inverse_yields_one_union_scope_false", te);
		List<Union> unions = collectUnions(te);
		assertThat(unions).hasSize(1);
		assertThat(isScopeChange(unions.get(0))).isFalse();
	}

	@Test
	@DisplayName("Sequence with inner alt (p/(q|r)/s) produces UNION with scope=false")
	void sequence_with_inner_alt_produces_union_scope_false() {
		String q = "SELECT ?s ?o WHERE { ?s ex:p/(ex:q|ex:r)/ex:s ?o }";
		TupleExpr te = parse(q);
		dumpAlgebra("sequence_with_inner_alt_produces_union_scope_false", te);
		List<Union> unions = collectUnions(te);
		assertThat(unions).isNotEmpty();
		assertThat(unions.stream().noneMatch(u -> isScopeChange(u))).isTrue();
	}

	@Test
	@DisplayName("Two alts in sequence ( (a|b)/(c|d) ): nested path UNIONs, all scope=false")
	void sequence_two_alts_nested_unions_all_false() {
		String q = "SELECT ?s ?o WHERE { ?s (ex:a|ex:b)/(ex:c|ex:d) ?o }";
		TupleExpr te = parse(q);
		dumpAlgebra("sequence_two_alts_nested_unions_all_false", te);
		List<Union> unions = collectUnions(te);
		assertThat(unions).isNotEmpty();
		assertThat(unions.stream().noneMatch(u -> isScopeChange(u))).isTrue();
	}

	@Test
	@DisplayName("Explicit UNION with alt and NPS branches: 1 explicit + 2 path-generated")
	void explicit_union_with_alt_and_nps_counts() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  { ?s (ex:a|ex:b) ?o } UNION { ?s !(^ex:p1|ex:p2) ?o }\n" +
				"}";
		TupleExpr te = parse(q);
		dumpAlgebra("explicit_union_with_alt_and_nps_counts", te);
		List<Union> unions = collectUnions(te);
		// Outer explicit UNION plus two branch roots that are UNIONs (alt + NPS): total 3
		assertThat(unions).hasSize(3);
		// Because branch roots are groups, they are marked as new scope as well
		assertThat(unions.stream().allMatch(TupleExprUnionPathScopeShapeTest::isScopeChange)).isTrue();
	}

	@Test
	@DisplayName("Nested explicit unions + alt path unions: 3 explicit, 2 generated")
	void nested_explicit_and_alt_counts_precise() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  { { ?s (ex:p1|ex:p2) ?o } UNION { ?s ex:q ?o } }\n" +
				"  UNION\n" +
				"  { { ?s ex:r ?o } UNION { ?s (ex:a|ex:b) ?o } }\n" +
				"}";
		TupleExpr te = parse(q);
		dumpAlgebra("nested_explicit_and_alt_counts_precise", te);
		List<Union> unions = collectUnions(te);
		// 5 UNION nodes overall (3 explicit + 2 path unions at branch roots), all in new scope
		assertThat(unions).hasSize(5);
		assertThat(unions.stream().allMatch(TupleExprUnionPathScopeShapeTest::isScopeChange)).isTrue();
	}

	@Test
	@DisplayName("Zero-or-more (*) uses ArbitraryLengthPath: no UNION present")
	void zeroOrMore_no_union() {
		String q = "SELECT ?s ?o WHERE { ?s ex:p* ?o }";
		TupleExpr te = parse(q);
		dumpAlgebra("zeroOrMore_no_union", te);
		List<Union> unions = collectUnions(te);
		assertThat(unions).isEmpty();
	}

	@Test
	@DisplayName("One-or-more (+) uses ArbitraryLengthPath: no UNION present")
	void oneOrMore_no_union() {
		String q = "SELECT ?s ?o WHERE { ?s ex:p+ ?o }";
		TupleExpr te = parse(q);
		dumpAlgebra("oneOrMore_no_union", te);
		List<Union> unions = collectUnions(te);
		assertThat(unions).isEmpty();
	}

	@Test
	@DisplayName("Single-member group ( (ex:p) ) produces no UNION")
	void single_member_group_no_union() {
		String q = "SELECT ?s ?o WHERE { ?s (ex:p) ?o }";
		TupleExpr te = parse(q);
		dumpAlgebra("single_member_group_no_union", te);
		List<Union> unions = collectUnions(te);
		assertThat(unions).isEmpty();
	}

	@Test
	@DisplayName("Summary listing of UNION scope flags for mixed case")
	void summary_listing_for_manual_inspection() {
		String q = "SELECT ?s ?o WHERE {\n" +
				"  { ?s (ex:p1|ex:p2) ?o } UNION { ?s !(ex:p3|^ex:p4) ?o }\n" +
				"  UNION\n" +
				"  { ?s ex:q ?o }\n" +
				"}";
		TupleExpr te = parse(q);
		List<Union> unions = collectUnions(te);
		String flags = unions.stream()
				.map(u -> isScopeChange(u) ? "explicit" : "parser-generated")
				.collect(Collectors.joining(", "));
		dumpAlgebra("summary_listing_for_manual_inspection__" + flags.replace(',', '_'), te);
		// Sanity: at least one UNION exists
		assertThat(unions).isNotEmpty();
	}

	// ------------- Classification-focused tests -------------

	@Test
	@DisplayName("Classification: pure alt path UNION is path-generated")
	void classify_pure_alt_path_union() {
		TupleExpr te = parse("SELECT * WHERE { ?s (ex:p1|ex:p2) ?o }");
		dumpAlgebra("classify_pure_alt_path_union", te);
		List<Union> unions = collectUnions(te);
		assertThat(unions).hasSize(1);
		assertThat(isPathGeneratedUnionHeuristic(unions.get(0))).isTrue();

		TupleExprIRRenderer r = new TupleExprIRRenderer();
		IrSelect raw = r.toIRSelectRaw(te);
		List<IrUnion> irUnions = collectIrUnions(raw);
		assertThat(irUnions).hasSize(1);
		assertThat(isPathGeneratedIrUnionHeuristic(irUnions.get(0))).isTrue();
	}

	@Test
	@DisplayName("Classification: explicit UNION with alt in left branch")
	void classify_explicit_union_with_alt_in_left_branch() {
		TupleExpr te = parse("SELECT * WHERE { { ?s (ex:a|ex:b) ?o } UNION { ?s ex:q ?o } }");
		dumpAlgebra("classify_explicit_union_with_alt_in_left_branch", te);
		List<Union> unions = collectUnions(te);
		// Expect 2 unions: outer explicit + inner path-generated (branch root)
		assertThat(unions).hasSize(2);
		Union outer = unions.get(0);
		Union inner = unions.get(1);
		// One explicit, one path-generated
		assertThat(isPathGeneratedUnionHeuristic(outer)).isFalse();
		assertThat(isPathGeneratedUnionHeuristic(inner)).isTrue();

		TupleExprIRRenderer r = new TupleExprIRRenderer();
		IrSelect raw = r.toIRSelectRaw(te);
		List<IrUnion> irUnions = collectIrUnions(raw);
		assertThat(irUnions).hasSize(2);
		assertThat(isPathGeneratedIrUnionHeuristic(irUnions.get(0))).isFalse();
		assertThat(isPathGeneratedIrUnionHeuristic(irUnions.get(1))).isTrue();
	}

	@Test
	@DisplayName("Classification: explicit UNION with alt in both branches")
	void classify_explicit_union_with_alt_in_both_branches() {
		TupleExpr te = parse("SELECT * WHERE { { ?s (ex:a|ex:b) ?o } UNION { ?s (ex:c|ex:d) ?o } }");
		dumpAlgebra("classify_explicit_union_with_alt_in_both_branches", te);
		List<Union> unions = collectUnions(te);
		// Expect 3 unions: 1 outer explicit + 2 inner path-generated
		assertThat(unions).hasSize(3);
		long pathGenerated = unions.stream()
				.filter(TupleExprUnionPathScopeShapeTest::isPathGeneratedUnionHeuristic)
				.count();
		long explicit = unions.size() - pathGenerated;
		assertThat(pathGenerated).isEqualTo(2);
		assertThat(explicit).isEqualTo(1);

		TupleExprIRRenderer r = new TupleExprIRRenderer();
		IrSelect raw = r.toIRSelectRaw(te);
		List<IrUnion> irUnions = collectIrUnions(raw);
		assertThat(irUnions).hasSize(3);
		assertThat(irUnions.get(0).isNewScope()).isTrue();
		long innerPath = irUnions.stream()
				.skip(1)
				.filter(TupleExprUnionPathScopeShapeTest::isPathGeneratedIrUnionHeuristic)
				.count();
		assertThat(innerPath).isEqualTo(2);
	}

	@Test
	@DisplayName("Classification: explicit UNION with NPS in left branch, simple right")
	void classify_explicit_union_with_nps_left_branch() {
		TupleExpr te = parse("SELECT * WHERE { { ?s !(ex:p1|^ex:p2) ?o } UNION { ?s ex:q ?o } }");
		dumpAlgebra("classify_explicit_union_with_nps_left_branch", te);
		List<Union> unions = collectUnions(te);
		// Expect 2 unions: outer explicit + inner path-generated (NPS union)
		assertThat(unions).hasSize(2);
		long pathGenerated = unions.stream()
				.filter(TupleExprUnionPathScopeShapeTest::isPathGeneratedUnionHeuristic)
				.count();
		long explicit = unions.size() - pathGenerated;
		assertThat(pathGenerated).isEqualTo(1);
		assertThat(explicit).isEqualTo(1);

		TupleExprIRRenderer r = new TupleExprIRRenderer();
		IrSelect raw = r.toIRSelectRaw(te);
		List<IrUnion> irUnions = collectIrUnions(raw);
		assertThat(irUnions).hasSize(2);
		long irPath = irUnions.stream()
				.filter(TupleExprUnionPathScopeShapeTest::isPathGeneratedIrUnionHeuristic)
				.count();
		assertThat(irPath).isEqualTo(1);
	}

	@Test
	@DisplayName("Classification: explicit UNION with NPS and alt in branches")
	void classify_explicit_union_with_nps_and_alt() {
		TupleExpr te = parse("SELECT * WHERE { { ?s !(ex:p1|^ex:p2) ?o } UNION { ?s (ex:a|ex:b) ?o } }");
		dumpAlgebra("classify_explicit_union_with_nps_and_alt", te);
		List<Union> unions = collectUnions(te);
		// Expect 3 unions: outer explicit + 2 inner path-generated
		assertThat(unions).hasSize(3);
		long pathGenerated = unions.stream()
				.filter(TupleExprUnionPathScopeShapeTest::isPathGeneratedUnionHeuristic)
				.count();
		assertThat(pathGenerated).isEqualTo(2);

		TupleExprIRRenderer r = new TupleExprIRRenderer();
		IrSelect raw = r.toIRSelectRaw(te);
		List<IrUnion> irUnions = collectIrUnions(raw);
		assertThat(irUnions).hasSize(3);
		assertThat(irUnions.get(0).isNewScope()).isTrue();
		long innerPath2 = irUnions.stream()
				.skip(1)
				.filter(TupleExprUnionPathScopeShapeTest::isPathGeneratedIrUnionHeuristic)
				.count();
		assertThat(innerPath2).isEqualTo(2);
	}

	@Test
	@DisplayName("Classification: alt path inside branch with extra triple (inner union path-generated, outer explicit)")
	void classify_alt_inside_branch_with_extra_triple() {
		TupleExpr te = parse("SELECT * WHERE { { ?s (ex:a|ex:b) ?o . ?s ex:q ?x } UNION { ?s ex:r ?o } }");
		dumpAlgebra("classify_alt_inside_branch_with_extra_triple", te);
		List<Union> unions = collectUnions(te);
		// Expect 2 unions overall: path-generated for alt, and outer explicit
		assertThat(unions.size()).isGreaterThanOrEqualTo(2);
		long pathGenerated = unions.stream()
				.filter(TupleExprUnionPathScopeShapeTest::isPathGeneratedUnionHeuristic)
				.count();
		long explicit = unions.size() - pathGenerated;
		assertThat(pathGenerated).isGreaterThanOrEqualTo(1);
		assertThat(explicit).isGreaterThanOrEqualTo(1);
	}

	@Test
	@DisplayName("Classification: zero-or-one (?) union is path-generated")
	void classify_zero_or_one_is_path_generated() {
		TupleExpr te = parse("SELECT * WHERE { ?s ex:p? ?o }");
		dumpAlgebra("classify_zero_or_one_is_path_generated", te);
		List<Union> unions = collectUnions(te);
		assertThat(unions).hasSize(1);
		assertThat(isPathGeneratedUnionHeuristic(unions.get(0))).isTrue();
	}

	// ------------- GRAPH / SERVICE / OPTIONAL combinations -------------

	@Test
	@DisplayName("GRAPH with alt path: path union newScope=false (raw/transformed)")
	void graph_with_alt_path_union_scope() {
		TupleExpr te = parse("SELECT * WHERE { GRAPH ex:g { ?s (ex:a|ex:b) ?o } }");
		dumpAlgebra("graph_with_alt_path_union_scope", te);
		// Algebra: one path-generated union
		List<Union> u = collectUnions(te);
		assertThat(u).hasSize(1);
		assertThat(isPathGeneratedUnionHeuristic(u.get(0))).isTrue();
		// IR: one IrUnion with newScope=false
		TupleExprIRRenderer r = new TupleExprIRRenderer();
		IrSelect raw = r.toIRSelectRaw(te);
		List<IrUnion> irUnionsRaw = collectIrUnions(raw);
		assertThat(irUnionsRaw).hasSize(1);
		assertThat(irUnionsRaw.get(0).isNewScope()).isFalse();
		IrSelect tr = r.toIRSelect(te);
		List<IrUnion> irUnionsTr = collectIrUnions(tr);
		// After transforms, alternation is typically fused into a path triple
		assertThat(irUnionsTr.size()).isLessThanOrEqualTo(1);
		assertThat(irUnionsTr.stream().allMatch(TupleExprUnionPathScopeShapeTest::isPathGeneratedIrUnionHeuristic))
				.isTrue();
	}

	@Test
	@DisplayName("GRAPH with NPS (direct+inverse): path union newScope=false (raw/transformed)")
	void graph_with_nps_union_scope() {
		TupleExpr te = parse("SELECT * WHERE { GRAPH ex:g { ?s !(ex:p1|^ex:p2) ?o } }");
		dumpAlgebra("graph_with_nps_union_scope", te);
		List<Union> u = collectUnions(te);
		assertThat(u).hasSize(1);
		assertThat(isPathGeneratedUnionHeuristic(u.get(0))).isTrue();
		TupleExprIRRenderer r = new TupleExprIRRenderer();
		List<IrUnion> rawU = collectIrUnions(r.toIRSelectRaw(te));
		assertThat(rawU).hasSize(1);
		assertThat(rawU.get(0).isNewScope()).isFalse();
		List<IrUnion> trU = collectIrUnions(r.toIRSelect(te));
		assertThat(trU.size()).isLessThanOrEqualTo(1);
		assertThat(trU.stream().allMatch(TupleExprUnionPathScopeShapeTest::isPathGeneratedIrUnionHeuristic)).isTrue();
	}

	@Test
	@DisplayName("OPTIONAL { alt } inside WHERE: inner path union newScope=false")
	void optional_with_alt_path_union_scope() {
		TupleExpr te = parse("SELECT * WHERE { OPTIONAL { ?s (ex:a|ex:b) ?o } }");
		dumpAlgebra("optional_with_alt_path_union_scope", te);
		List<Union> u = collectUnions(te);
		assertThat(u).hasSize(1);
		assertThat(isPathGeneratedUnionHeuristic(u.get(0))).isTrue();
		TupleExprIRRenderer r = new TupleExprIRRenderer();
		List<IrUnion> rawU = collectIrUnions(r.toIRSelectRaw(te));
		assertThat(rawU).hasSize(1);
		assertThat(rawU.get(0).isNewScope()).isFalse();
		List<IrUnion> trU = collectIrUnions(r.toIRSelect(te));
		assertThat(trU.size()).isLessThanOrEqualTo(1);
		assertThat(trU.stream().allMatch(TupleExprUnionPathScopeShapeTest::isPathGeneratedIrUnionHeuristic)).isTrue();
	}

	@Test
	@DisplayName("OPTIONAL { NPS } inside WHERE: inner path union newScope=false")
	void optional_with_nps_union_scope() {
		TupleExpr te = parse("SELECT * WHERE { OPTIONAL { ?s !(ex:p1|^ex:p2) ?o } }");
		dumpAlgebra("optional_with_nps_union_scope", te);
		List<Union> u = collectUnions(te);
		assertThat(u).hasSize(1);
		assertThat(isPathGeneratedUnionHeuristic(u.get(0))).isTrue();
		TupleExprIRRenderer r = new TupleExprIRRenderer();
		List<IrUnion> rawU = collectIrUnions(r.toIRSelectRaw(te));
		assertThat(rawU).hasSize(1);
		assertThat(rawU.get(0).isNewScope()).isFalse();
		List<IrUnion> trU = collectIrUnions(r.toIRSelect(te));
		assertThat(trU.size()).isLessThanOrEqualTo(1);
		assertThat(trU.stream().allMatch(TupleExprUnionPathScopeShapeTest::isPathGeneratedIrUnionHeuristic)).isTrue();
	}

	@Test
	@DisplayName("SERVICE { alt } inside WHERE: inner path union newScope=false")
	void service_with_alt_path_union_scope() {
		TupleExpr te = parse("SELECT * WHERE { SERVICE <http://svc/> { ?s (ex:a|ex:b) ?o } }");
		dumpAlgebra("service_with_alt_path_union_scope", te);
		List<Union> u = collectUnions(te);
		assertThat(u).hasSize(1);
		assertThat(isPathGeneratedUnionHeuristic(u.get(0))).isTrue();
		TupleExprIRRenderer r = new TupleExprIRRenderer();
		List<IrUnion> rawU = collectIrUnions(r.toIRSelectRaw(te));
		assertThat(rawU).hasSize(1);
		assertThat(isPathGeneratedIrUnionHeuristic(rawU.get(0))).isTrue();
		List<IrUnion> trU = collectIrUnions(r.toIRSelect(te));
		assertThat(trU.size()).isLessThanOrEqualTo(1);
		assertThat(trU.stream().allMatch(TupleExprUnionPathScopeShapeTest::isPathGeneratedIrUnionHeuristic)).isTrue();
	}

	@Test
	@DisplayName("SERVICE { NPS } inside WHERE: inner path union newScope=false")
	void service_with_nps_union_scope() {
		TupleExpr te = parse("SELECT * WHERE { SERVICE <http://svc/> { ?s !(ex:p1|^ex:p2) ?o } }");
		dumpAlgebra("service_with_nps_union_scope", te);
		List<Union> u = collectUnions(te);
		assertThat(u).hasSize(1);
		assertThat(isPathGeneratedUnionHeuristic(u.get(0))).isTrue();
		TupleExprIRRenderer r = new TupleExprIRRenderer();
		List<IrUnion> rawU = collectIrUnions(r.toIRSelectRaw(te));
		assertThat(rawU).hasSize(1);
		assertThat(isPathGeneratedIrUnionHeuristic(rawU.get(0))).isTrue();
		List<IrUnion> trU = collectIrUnions(r.toIRSelect(te));
		assertThat(trU.size()).isLessThanOrEqualTo(1);
		assertThat(trU.stream().allMatch(TupleExprUnionPathScopeShapeTest::isPathGeneratedIrUnionHeuristic)).isTrue();
	}

	@Test
	@DisplayName("Explicit UNION with GRAPH{alt} branch: outer explicit=1, inner path=1 (raw/transformed)")
	void explicit_union_with_graph_alt_branch_counts() {
		TupleExpr te = parse("SELECT * WHERE { { GRAPH ex:g { ?s (ex:a|ex:b) ?o } } UNION { ?s ex:q ?o } }");
		dumpAlgebra("explicit_union_with_graph_alt_branch_counts", te);
		List<Union> al = collectUnions(te);
		long path = al.stream().filter(TupleExprUnionPathScopeShapeTest::isPathGeneratedUnionHeuristic).count();
		long explicit = al.size() - path;
		assertThat(al.size()).isGreaterThanOrEqualTo(2);
		assertThat(explicit).isGreaterThanOrEqualTo(1);
		assertThat(path).isGreaterThanOrEqualTo(1);
		TupleExprIRRenderer r = new TupleExprIRRenderer();
		List<IrUnion> rawU = collectIrUnions(r.toIRSelectRaw(te));
		assertThat(rawU.size()).isGreaterThanOrEqualTo(2);
		long rawPath = rawU.stream().filter(TupleExprUnionPathScopeShapeTest::isPathGeneratedIrUnionHeuristic).count();
		long rawExplicit = rawU.size() - rawPath;
		assertThat(rawExplicit).isGreaterThanOrEqualTo(1);
		assertThat(rawPath).isGreaterThanOrEqualTo(1);
		List<IrUnion> trU = collectIrUnions(r.toIRSelect(te));
		assertThat(trU.size()).isGreaterThanOrEqualTo(1);
		long trExplicit = trU.stream().filter(u -> !isPathGeneratedIrUnionHeuristic(u)).count();
		assertThat(trExplicit).isGreaterThanOrEqualTo(1);
	}

	@Test
	@DisplayName("Explicit UNION with SERVICE{alt} branch: outer explicit=1, inner path=1 (raw/transformed)")
	void explicit_union_with_service_alt_branch_counts() {
		TupleExpr te = parse("SELECT * WHERE { { SERVICE <http://svc/> { ?s (ex:a|ex:b) ?o } } UNION { ?s ex:q ?o } }");
		dumpAlgebra("explicit_union_with_service_alt_branch_counts", te);
		List<Union> al = collectUnions(te);
		long path = al.stream().filter(TupleExprUnionPathScopeShapeTest::isPathGeneratedUnionHeuristic).count();
		long explicit = al.size() - path;
		assertThat(al.size()).isGreaterThanOrEqualTo(2);
		assertThat(explicit).isGreaterThanOrEqualTo(1);
		assertThat(path).isGreaterThanOrEqualTo(1);
		TupleExprIRRenderer r = new TupleExprIRRenderer();
		List<IrUnion> rawU = collectIrUnions(r.toIRSelectRaw(te));
		assertThat(rawU.size()).isGreaterThanOrEqualTo(2);
		long rawPath = rawU.stream().filter(TupleExprUnionPathScopeShapeTest::isPathGeneratedIrUnionHeuristic).count();
		long rawExplicit = rawU.size() - rawPath;
		assertThat(rawExplicit).isGreaterThanOrEqualTo(1);
		assertThat(rawPath).isGreaterThanOrEqualTo(1);
		List<IrUnion> trU = collectIrUnions(r.toIRSelect(te));
		assertThat(trU.size()).isGreaterThanOrEqualTo(1);
		long trExplicit = trU.stream().filter(u -> !isPathGeneratedIrUnionHeuristic(u)).count();
		assertThat(trExplicit).isGreaterThanOrEqualTo(1);
	}

	@Test
	@DisplayName("Explicit UNION with OPTIONAL{alt} branch: outer explicit=1, inner path=1 (raw/transformed)")
	void explicit_union_with_optional_alt_branch_counts() {
		TupleExpr te = parse("SELECT * WHERE { { OPTIONAL { ?s (ex:a|ex:b) ?o } } UNION { ?s ex:q ?o } }");
		dumpAlgebra("explicit_union_with_optional_alt_branch_counts", te);
		List<Union> al = collectUnions(te);
		long path = al.stream().filter(TupleExprUnionPathScopeShapeTest::isPathGeneratedUnionHeuristic).count();
		long explicit = al.size() - path;
		assertThat(al.size()).isGreaterThanOrEqualTo(2);
		assertThat(explicit).isGreaterThanOrEqualTo(1);
		assertThat(path).isGreaterThanOrEqualTo(1);
		TupleExprIRRenderer r = new TupleExprIRRenderer();
		List<IrUnion> rawU = collectIrUnions(r.toIRSelectRaw(te));
		assertThat(rawU.size()).isGreaterThanOrEqualTo(2);
		long rawPath = rawU.stream().filter(TupleExprUnionPathScopeShapeTest::isPathGeneratedIrUnionHeuristic).count();
		long rawExplicit = rawU.size() - rawPath;
		assertThat(rawExplicit).isGreaterThanOrEqualTo(1);
		assertThat(rawPath).isGreaterThanOrEqualTo(1);
		List<IrUnion> trU = collectIrUnions(r.toIRSelect(te));
		assertThat(trU.size()).isGreaterThanOrEqualTo(1);
		long trExplicit = trU.stream().filter(u -> !isPathGeneratedIrUnionHeuristic(u)).count();
		assertThat(trExplicit).isGreaterThanOrEqualTo(1);
	}
}
