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

package org.eclipse.rdf4j.queryrender.sparql;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.algebra.BindingSetAssignment;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.ValueConstant;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.queryrender.VarNameNormalizer;
import org.eclipse.rdf4j.queryrender.sparql.ir.IRTextPrinter;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrBGP;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrGraph;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrNode;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrPathTriple;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrStatementPattern;
import org.eclipse.rdf4j.queryrender.sparql.ir.IrSubSelect;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.IrDebug;
import org.eclipse.rdf4j.queryrender.sparql.ir.util.IrTransforms;
import org.eclipse.rdf4j.queryrender.sparql.util.TermRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TupleExprIRRenderer: user-facing façade to convert RDF4J algebra back into SPARQL text.
 *
 * <p>
 * Conversion of {@link TupleExpr} into a textual IR and expression rendering is delegated to
 * {@link TupleExprToIrConverter}. This class orchestrates IR transforms and printing, and provides a small
 * configuration surface and convenience entrypoints.
 * </p>
 *
 * Features:
 *
 * <ul>
 * <li>SELECT / ASK / DESCRIBE / CONSTRUCT forms</li>
 * <li>BGPs, OPTIONALs, UNIONs, MINUS, GRAPH, SERVICE, VALUES</li>
 * <li>Property paths, plus safe best-effort reassembly for simple cases</li>
 * <li>Aggregates, GROUP BY, HAVING (with _anon_having_* substitution)</li>
 * <li>Subselects in WHERE</li>
 * <li>ORDER BY, LIMIT, OFFSET</li>
 * <li>Prefix compaction and nice formatting</li>
 * </ul>
 *
 * How it works (big picture):
 * <ul>
 * <li>Normalize the TupleExpr (peel Order/Slice/Distinct/etc., detect HAVING) into a lightweight {@code Normalized}
 * carrier.</li>
 * <li>Build a textual Intermediate Representation (IR) that mirrors SPARQL’s shape: a header (projection), a list-like
 * WHERE block ({@link IrBGP}), and trailing modifiers. The IR tries to be a straightforward, low-logic mirror of the
 * TupleExpr tree.</li>
 * <li>Run a small, ordered pipeline of IR transforms ({@link IrTransforms}) that are deliberately side‑effect‑free and
 * compositional. Each transform is narrowly scoped (e.g., property path fusions, negated property sets, collections)
 * and uses simple heuristics like only fusing across parser‑generated bridge variables named with the
 * {@code _anon_path_} prefix.</li>
 * <li>Print the transformed IR using a tiny printer interface ({@link IrPrinter}) that centralizes indentation, IRI
 * compaction, and child printing.</li>
 * </ul>
 *
 * Policy/decisions:
 * <ul>
 * <li>Do <b>not</b> rewrite a single inequality {@code ?p != <iri>} into {@code ?p NOT IN (<iri>)}. Only reconstruct
 * NOT IN when multiple {@code !=} terms share the same variable.</li>
 * <li>Do <b>not</b> fuse {@code ?s ?p ?o . FILTER (?p != <iri>)} into a negated path {@code ?s !(<iri>) ?o}.</li>
 * <li>Use {@code a} for {@code rdf:type} consistently, incl. inside property lists.</li>
 * </ul>
 *
 * Naming hints from the RDF4J parser:
 * <ul>
 * <li>{@code _anon_path_*}: anonymous intermediate variables introduced when parsing property paths. Transforms only
 * compose chains across these bridge variables to avoid altering user bindings.</li>
 * <li>{@code _anon_having_*}: marks variables synthesized for HAVING extraction.</li>
 * <li>{@code _anon_bnode_*}: placeholder variables for [] that should render as an empty blank node.</li>
 * </ul>
 */
@Experimental
public class TupleExprIRRenderer {
	private static final Logger log = LoggerFactory.getLogger(TupleExprIRRenderer.class);

	// ---------------- Public API helpers ----------------

	// ---------------- Configuration ----------------
	/** Anonymous blank node variables (originating from [] in the original query). */

	private final Config cfg;
	private final PrefixIndex prefixIndex;
	private final Map<String, String> userBnodeLabels = new LinkedHashMap<>();
	private final Map<String, String> anonBnodeLabels = new LinkedHashMap<>();
	private int bnodeCounter = 1;
	private static final String USER_BNODE_PREFIX = "_anon_user_bnode_";
	private static final String ANON_BNODE_PREFIX = "_anon_bnode_";

	public TupleExprIRRenderer() {
		this(new Config());
	}

	public TupleExprIRRenderer(final Config cfg) {
		this.cfg = cfg == null ? new Config() : cfg;
		this.prefixIndex = new PrefixIndex(this.cfg.prefixes);
	}

	public void reset() {
		userBnodeLabels.clear();
		anonBnodeLabels.clear();
		bnodeCounter = 1;
	}

	// ---------------- Experimental textual IR API ----------------

	// Package-private accessors for the converter
	Config getConfig() {
		return cfg;
	}

	/**
	 * Build a best‑effort textual IR for a SELECT‑form query.
	 *
	 * Steps:
	 * <ol>
	 * <li>Normalize the TupleExpr (gather LIMIT/OFFSET/ORDER, peel wrappers, detect HAVING candidates).</li>
	 * <li>Translate the remaining WHERE tree into an IR block ({@link IrBGP}) with simple, explicit nodes (statement
	 * patterns, path triples, filters, graphs, unions, etc.).</li>
	 * <li>Apply the ordered IR transform pipeline ({@link IrTransforms#transformUsingChildren}) to perform
	 * purely-textual best‑effort fusions (paths, NPS, collections, property lists) while preserving user variable
	 * bindings.</li>
	 * <li>Populate IR header sections (projection, group by, having, order by) from normalized metadata.</li>
	 * </ol>
	 *
	 * The method intentionally keeps TupleExpr → IR logic simple; most nontrivial decisions live in transform passes
	 * for clarity and testability.
	 */
	public IrSelect toIRSelect(final TupleExpr tupleExpr) {
		// Build raw IR (no transforms) via the converter
		IrSelect ir = new TupleExprToIrConverter(this).toIRSelect(tupleExpr);
		if (cfg.debugIR) {
			System.out.println("# IR (raw)\n" + IrDebug.dump(ir));
		}
		// Transform IR, including nested subselects, then apply top-level grouping preservation
		IrSelect transformed = transformIrRecursively(ir);
		// Preserve explicit grouping braces around a single‑element WHERE when the original algebra
		// indicated a variable scope change at the root of the query.
		if (transformed != null && transformed.getWhere() != null
				&& transformed.getWhere().getLines() != null
				&& transformed.getWhere().getLines().size() == 1
				&& TupleExprToIrConverter.hasExplicitRootScope(tupleExpr)) {
			final IrNode only = transformed.getWhere().getLines().get(0);
			if (only instanceof IrStatementPattern || only instanceof IrPathTriple || only instanceof IrGraph
					|| only instanceof IrSubSelect) {
				transformed.getWhere().setNewScope(true);
			}
		}
		if (cfg.debugIR) {
			System.out.println("# IR (transformed)\n" + IrDebug.dump(transformed));
		}
		return transformed;
	}

	/** Build IR without applying IR transforms (raw). Useful for tests and debugging. */
	public IrSelect toIRSelectRaw(final TupleExpr tupleExpr) {
		return TupleExprToIrConverter.toIRSelectRaw(tupleExpr, this, false);
	}

	/** Dump raw IR (JSON) for debugging/tests. */
	public String dumpIRRaw(final TupleExpr tupleExpr) {
		return IrDebug.dump(toIRSelectRaw(tupleExpr));
	}

	/** Dump transformed IR (JSON) for debugging/tests. */
	public String dumpIRTransformed(final TupleExpr tupleExpr) {
		return IrDebug.dump(toIRSelect(tupleExpr));
	}

	/** Render a textual SELECT query from an {@code IrSelect} model. */

	// ---------------- Rendering helpers (prefix-aware) ----------------
	public String render(final IrSelect ir,
			final DatasetView dataset, final boolean subselect) {
		final StringBuilder out = new StringBuilder(256);
		if (!subselect) {
			printPrologueAndDataset(out, dataset);
		}
		IRTextPrinter printer = new IRTextPrinter(out, this::convertVarToString, cfg);
		ir.print(printer);
		return out.toString().trim();
	}

	// Recursively apply the transformer pipeline to a select and any nested subselects.
	private IrSelect transformIrRecursively(final IrSelect select) {
		if (select == null) {
			return null;
		}
		// First, transform the WHERE using standard pipeline
		IrSelect top = IrTransforms.transformUsingChildren(select, this);
		// Then, transform nested subselects via a child-mapping pass
		IrNode mapped = top.transformChildren(child -> {
			if (child instanceof IrBGP) {
				// descend into BGP lines to replace IrSubSelects
				IrBGP bgp = (IrBGP) child;
				IrBGP nb = new IrBGP(!bgp.getLines().isEmpty() && bgp.isNewScope());
				nb.setNewScope(bgp.isNewScope());
				for (IrNode ln : bgp.getLines()) {
					if (ln instanceof IrSubSelect) {
						IrSubSelect ss = (IrSubSelect) ln;
						IrSelect subSel = ss.getSelect();
						IrSelect subTx = transformIrRecursively(subSel);
						nb.add(new IrSubSelect(subTx, ss.isNewScope()));
					} else {
						nb.add(ln);
					}
				}
				return nb;
			}
			return child;
		});
		return (IrSelect) mapped;
	}

	/** Backward-compatible: render as SELECT query (no dataset). */
	public String render(final TupleExpr tupleExpr) {
		return renderSelectInternal(tupleExpr, RenderMode.TOP_LEVEL_SELECT, null);
	}

	/** SELECT with dataset (FROM/FROM NAMED). */
	public String render(final TupleExpr tupleExpr, final DatasetView dataset) {
		return renderSelectInternal(tupleExpr, RenderMode.TOP_LEVEL_SELECT, dataset);
	}

	/** ASK query (top-level). */
	public String renderAsk(final TupleExpr tupleExpr, final DatasetView dataset) {
		// Build IR (including transforms) and then print only the WHERE block using the IR printer.
		reset();
		BNodeValidator.validate(tupleExpr, cfg);
		final StringBuilder out = new StringBuilder(256);
		final IrSelect ir = toIRSelect(tupleExpr);
		// Prologue
		printPrologueAndDataset(out, dataset);
		out.append("ASK");
		// WHERE (from IR)
		out.append(cfg.canonicalWhitespace ? "\nWHERE " : " WHERE ");
		new IRTextPrinter(out, this::convertVarToString, cfg).printWhere(ir.getWhere());
		String rendered = out.toString().trim();
		verifyRoundTrip(tupleExpr, rendered);
		return rendered;
	}

	private String renderSelectInternal(final TupleExpr tupleExpr,
			final RenderMode mode,
			final DatasetView dataset) {
		reset();
		BNodeValidator.validate(tupleExpr, cfg);
		final IrSelect ir = toIRSelect(tupleExpr);
		final boolean asSub = mode == RenderMode.SUBSELECT;
		String rendered = render(ir, dataset, asSub);
//		verifyRoundTrip(tupleExpr, rendered);
		return rendered;
	}

	private void verifyRoundTrip(final TupleExpr original, final String rendered) {
		if (!cfg.verifyRoundTrip || original == null || rendered == null || rendered.isEmpty()) {
			return;
		}

		try {
			ParsedQuery parsed = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, rendered, null);
			String expected = VarNameNormalizer.normalizeVars(original.toString());
			String actual = VarNameNormalizer.normalizeVars(parsed.getTupleExpr().toString());
			if (!expected.equals(actual)) {
				String message = "Rendered SPARQL does not round-trip to the original TupleExpr."
						+ "\n# Rendered query\n" + rendered
						+ "\n# Original TupleExpr (normalized)\n" + expected
						+ "\n# Round-tripped TupleExpr (normalized)\n" + actual
						+ "\n# Diff (original -> round-tripped)\n" + diffText(expected, actual);
				throw new IllegalStateException(message);
			}
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			log.error("Unexpected error while round-tripping TupleExpr. original={}, rendered={}",
					original, rendered, e);
			throw new IllegalStateException("Failed to verify rendered SPARQL against the original TupleExpr", e);
		}
	}

	// diff the two strings to help debugging
	private String diffText(String expected, String actual) {
		List<String> expLines = List.of(expected.split("\\R", -1));
		List<String> actLines = List.of(actual.split("\\R", -1));

		int max = Math.max(expLines.size(), actLines.size());
		StringBuilder sb = new StringBuilder(256);
		for (int i = 0; i < max; i++) {
			String el = i < expLines.size() ? expLines.get(i) : "<missing>";
			String al = i < actLines.size() ? actLines.get(i) : "<missing>";
			if (!el.trim().equals(al.trim())) {
				sb.append("line ").append(i + 1).append(":\n");
				sb.append("- ").append(el).append('\n');
				sb.append("+ ").append(al).append('\n');
				int common = commonPrefixLength(el, al);
				if (common < Math.min(el.length(), al.length())) {
					sb.append("  ").append(" ".repeat(common)).append("^\n");
				}
			}
			if (sb.length() > 1024) {
				sb.append("... diff truncated ...");
				break;
			}
		}
		return sb.length() == 0 ? "<no visible diff>" : sb.toString();
	}

	private int commonPrefixLength(String a, String b) {
		int limit = Math.min(a.length(), b.length());
		int i = 0;
		while (i < limit && a.charAt(i) == b.charAt(i)) {
			i++;
		}
		return i;
	}

	// ---- Validation: reject illegal blank node placements before rendering ----
	private static final class BNodeValidator extends AbstractQueryModelVisitor<RuntimeException> {
		private final Config cfg;

		private BNodeValidator(Config cfg) {
			this.cfg = cfg == null ? new Config() : cfg;
		}

		static void validate(TupleExpr expr, Config cfg) {
			if (expr == null || cfg == null || !cfg.failOnIllegalBNodes) {
				return;
			}
			expr.visit(new BNodeValidator(cfg));
		}

		@Override
		public void meet(BindingSetAssignment node) {
			if (cfg.allowBNodesInValues) {
				return;
			}
			for (BindingSet bs : node.getBindingSets()) {
				for (String name : bs.getBindingNames()) {
					Value v = bs.getValue(name);
					if (v instanceof BNode) {
						throw new IllegalArgumentException("Blank nodes in VALUES are not supported: binding '" + name
								+ "' -> " + v);
					}
				}
			}
		}

		@Override
		public void meet(StatementPattern sp) {
			// StatementPattern positions allow anonymous bnodes (subject/object). Predicate bnodes are illegal but
			// should not occur after parsing; keep tolerant to avoid overblocking.
		}

		@Override
		public void meet(Var var) {
			if (!var.isAnonymous()) {
				return;
			}
			String name = var.getName();
			if (name == null) {
				return;
			}

			assert !name.startsWith("anon_");

			if (name.startsWith("_anon_bnode_") || name.startsWith("_anon_user_bnode_")) {
				throw new IllegalArgumentException("Anonymous blank node used in expression context: " + name);
			}
		}

		@Override
		public void meet(ValueConstant node) {
			if (node.getValue() instanceof BNode) {
				throw new IllegalArgumentException("Blank node literal in expression context is not supported: "
						+ node.getValue());
			}
		}
	}

	private void printPrologueAndDataset(final StringBuilder out, final DatasetView dataset) {
		if (cfg.printPrefixes && !cfg.prefixes.isEmpty()) {
			cfg.prefixes.forEach((pfx, ns) -> out.append("PREFIX ").append(pfx).append(": <").append(ns).append(">\n"));
		}
		// FROM / FROM NAMED (top-level only)
		final List<IRI> dgs = dataset != null ? dataset.defaultGraphs : cfg.defaultGraphs;
		final List<IRI> ngs = dataset != null ? dataset.namedGraphs : cfg.namedGraphs;
		for (IRI iri : dgs) {
			out.append("FROM ").append(convertIRIToString(iri)).append("\n");
		}
		for (IRI iri : ngs) {
			out.append("FROM NAMED ").append(convertIRIToString(iri)).append("\n");
		}
	}

	String convertVarToString(final Var v) {
		if (v == null) {
			return "?_";
		}
		if (v.hasValue()) {
			return convertValueToString(v.getValue());
		}

		// Anonymous blank node placeholder variables originating from [] should render as [].
		if (v.isAnonymous() && v.getName() != null && v.getName().startsWith(ANON_BNODE_PREFIX)) {

			if (cfg.preserveAnonBNodeIdentity) {
				return "_:" + anonBnodeLabels.computeIfAbsent(v.getName(),
						TupleExprIRRenderer::deriveStableLabelFromName);
			}
			return "[]";
		}
		// User-specified blank nodes (_:bnode1) are encoded with the _anon_user_bnode_ prefix; restore the label.
		if (v.isAnonymous() && v.getName() != null && v.getName().startsWith(USER_BNODE_PREFIX)) {

			String existing = userBnodeLabels.get(v.getName());
			if (existing == null) {
				if (cfg.preserveUserBNodeLabels || cfg.deterministicBNodeLabels) {
					existing = deriveStableLabelFromName(v.getName());
				} else {
					existing = "bnode" + bnodeCounter++;
				}
				userBnodeLabels.put(v.getName(), existing);
			}
			return "_:" + existing;
		}
		// Path bridge variables (_anon_path_*) must render as regular variables so they can be
		// shared across UNION branches without violating blank-node scoping rules during parsing.
		if (v.isAnonymous() && v.getName() != null && v.getName().startsWith("_anon_path_")) {
			return "?" + v.getName();
		}

		if (v.isAnonymous() && !v.isConstant()) {
			return "_:" + v.getName();
		}
		return "?" + v.getName();
	}

	public String convertValueToString(final Value val) {
		return TermRenderer.convertValueToString(val, prefixIndex, cfg.usePrefixCompaction);
	}

	private static String deriveStableLabelFromName(String name) {
		if (name == null) {
			return "bnode";
		}
		String trimmed = name;

		assert !trimmed.startsWith("anon_");

		if (trimmed.startsWith(USER_BNODE_PREFIX)) {
			trimmed = trimmed.substring(USER_BNODE_PREFIX.length());
		} else if (trimmed.startsWith(ANON_BNODE_PREFIX)) {
			trimmed = trimmed.substring(ANON_BNODE_PREFIX.length());
		}

		if (trimmed.isEmpty()) {
			return "bnode";
		}

		if (trimmed.matches("[A-Za-z0-9_-]+")) {
			return trimmed.startsWith("bnode") ? trimmed : "bnode" + trimmed;
		}

		return "bnode" + Integer.toHexString(trimmed.hashCode());
	}

	// ---- Aggregates ----

	public String convertIRIToString(final IRI iri) {
		return TermRenderer.convertIRIToString(iri, prefixIndex, cfg.usePrefixCompaction);
	}

	/**
	 * Convert a Var to a compact IRI string when it is bound to a constant IRI; otherwise return null. Centralizes a
	 * common pattern used by IR nodes and helpers to avoid duplicate null/instance checks.
	 */
	public String convertVarIriToString(final Var v) {
		if (v != null && v.hasValue() && v.getValue() instanceof IRI) {
			return convertIRIToString((IRI) v.getValue());
		}
		return null;
	}

	// NOTE: NOT IN reconstruction moved into NormalizeFilterNotInTransform.

	/** Rendering context: top-level query vs nested subselect. */
	private enum RenderMode {
		TOP_LEVEL_SELECT,
		SUBSELECT
	}

	/** Optional dataset input for FROM/FROM NAMED lines. */
	public static final class DatasetView {
		public final List<IRI> defaultGraphs = new ArrayList<>();
		public final List<IRI> namedGraphs = new ArrayList<>();

		public DatasetView addDefault(IRI iri) {
			if (iri != null) {
				defaultGraphs.add(iri);
			}
			return this;
		}

		public DatasetView addNamed(IRI iri) {
			if (iri != null) {
				namedGraphs.add(iri);
			}
			return this;
		}
	}

	public static final class Config {
		public final String indent = "  ";
		public final boolean printPrefixes = true;
		public final boolean usePrefixCompaction = true;
		public final boolean canonicalWhitespace = true;
		public boolean verifyRoundTrip = true; // parse rendered SPARQL and compare to original TupleExpr
		public final LinkedHashMap<String, String> prefixes = new LinkedHashMap<>();
		// Flags
		// Optional dataset (top-level only) if you never pass a DatasetView at render().
		// These are rarely used, but offered for completeness.
		public final List<IRI> defaultGraphs = new ArrayList<>();
		public final List<IRI> namedGraphs = new ArrayList<>();
		public boolean debugIR = false; // print IR before and after transforms
		public boolean valuesPreserveOrder = false; // keep VALUES column order as given by BSA iteration
		public boolean preserveUserBNodeLabels = false; // derive stable labels from parser placeholder
		public boolean deterministicBNodeLabels = false; // stable mapping independent of traversal order
		public boolean preserveAnonBNodeIdentity = false; // render repeated [] as the same _:label
		public boolean failOnIllegalBNodes = true; // reject bnodes in VALUES or expression contexts
		public boolean allowBNodesInValues = false; // override to allow (non-standard) bnodes in VALUES
	}

}
