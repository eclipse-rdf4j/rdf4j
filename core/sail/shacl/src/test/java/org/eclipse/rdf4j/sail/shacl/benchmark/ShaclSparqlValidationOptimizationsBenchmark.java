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

package org.eclipse.rdf4j.sail.shacl.benchmark;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.transaction.QueryEvaluationMode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.results.lazy.LazyValidationReport;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * Benchmarks the SPARQL-based SHACL validation path affected by:
 *
 * <ul>
 * <li>NodeKind SPARQL filter simplification</li>
 * <li>xsd:string datatype validation shortcut</li>
 * <li>UNION splitting into independent validation queries</li>
 * </ul>
 *
 * <p>
 * The benchmark deliberately contains separate workloads for each optimization. This makes it possible to distinguish
 * the effect of an individual change from the noise introduced by a combined validation workload.
 *
 * <p>
 * All validation is performed through the Bulk validation approach because that is the path that generates and executes
 * the SPARQL validation queries.
 *
 * <p>
 * <b>What is measured:</b> only the data load + Bulk SPARQL validation. Everything else - creating the repository and
 * loading the ontology and the SHACL shapes - is done in the fixture setup, <b>outside</b> the measured method. Because
 * a Bulk commit leaves the loaded data in the store, each measured invocation needs a fresh repository; the load +
 * validation fixtures use {@link Level#Invocation} setup/teardown to rebuild the store around every measured op. That
 * per-invocation overhead is fine here because a single data load + validation takes far longer than the JMH timestamp
 * granularity that makes {@code Level.Invocation} risky for sub-millisecond work.
 *
 * @author Sava Savov
 */
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, jvmArgs = { "-Xms16G", "-Xmx16G" })
@Measurement(iterations = 15)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ShaclSparqlValidationOptimizationsBenchmark {
	private static final String EX = "http://example.com/";
	private static final SimpleValueFactory VF = SimpleValueFactory.getInstance();

	// Number of leaf subclasses per target class. Every workload's targets are typed as these leaves, so the shapes'
	// sh:targetClass (NodeKindTarget / StringTarget / UnionTarget) only selects them through rdfs:subClassOf inference
	// -
	// mirroring BuildingX's Room -> OfficeRoom/MeetingRoom/... hierarchy.
	private static final int TARGET_KINDS = 5;

	/*
	 * --------------- * Benchmark parameters * --------------- *
	 */
	/**
	 * Number of resources used by the NodeKind benchmark.
	 */
	public int nodeKindTargets = 100000;
	/**
	 * Number of additional {@code ex:blankValue_0..N-1} properties (each {@code sh:nodeKind sh:BlankNode}) added on top
	 * of the single {@code iriValue}/{@code literalValue}/{@code blankValue} properties, so that most of the per-target
	 * nodeKind checks are the {@code BlankNode} kind - deliberately, not incidentally.
	 * <p>
	 * The pre-optimization resolved-filter expression is an OR-ladder tried in a fixed order - {@code isIRI(?value)},
	 * then {@code isLiteral(?value)}, then {@code isBlank(?value)} - so a value satisfying the <em>first</em> disjunct
	 * (an IRI checked against {@code sh:nodeKind sh:IRI}) costs one {@code isX()} call on the old path, while a value
	 * satisfying the <em>last</em> one (a blank node checked against {@code sh:nodeKind sh:BlankNode}) costs three:
	 * {@code isIRI} and {@code isLiteral} both have to fail before {@code isBlank} is even tried. The single
	 * {@code iriValue}/{@code literalValue}/{@code blankValue} properties alone average that out (1+2+3 calls across
	 * the three, vs. 1+1+1 on the resolved path - a 2x reduction, diluted by the cheap {@code iriValue} case).
	 * Weighting the workload towards the {@code BlankNode} case instead exercises the old path's actual worst case
	 * rather than an average across all three, closer to the 3x per-value reduction the resolved single check buys
	 * there.
	 */
	public int nodeKindBlankProperties = 8;
	/**
	 * Number of xsd:string literals.
	 */
	public int stringTargets = 10000;
	/**
	 * Size of each xsd:string literal. The idea behind the optimization was that skipping {@code literal.stringValue()}
	 * for xsd:string avoids materializing the lexical form, which would matter for out-of-line/large literals.
	 * <p>
	 * <b>Caveat - this optimization is only meaningful for stores that lazily load the lexical form:</b> on all of
	 * RDF4J's own stores the label and the datatype live in a single value record and are read together, so touching
	 * the datatype already materializes the label. See the note on {@link #xsdString(StringFixture)}. Larger literals
	 * therefore do not make this optimization pay off on RDF4J - they only make the (already unavoidable) value read
	 * bigger.
	 */
	public int stringLiteralSize = 64;
	/**
	 * Number of property constraints on the nested focus node shape. Each ({@code ex:unionProp_0..N-1}) mirrors
	 * {@code bx:PrimaryContactShape}'s {@code name} - {@code sh:minCount 1 ; sh:maxCount 1 ; sh:nodeKind sh:Literal} -
	 * so each contributes <b>three</b> constraint branches (nodeKind, minCount, maxCount) to the folded UNION, plus the
	 * outer {@code sh:node} property's own maxCount/nodeKind branches. All branches share the target+path prefix.
	 * {@code sh:nodeKind} (not {@code sh:datatype}) is used deliberately: a live TRACE dump of a real BuildingX-shaped
	 * validation query showed its per-property branches as simple {@code !isLiteral(?value)}/{@code !isIRI(?value)}
	 * checks (resolved by {@link org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.NodeKindConstraintComponent}),
	 * not the heavier datatype-conformance-function check {@code sh:datatype xsd:string} compiles to.
	 * <p>
	 * <b>Note (see {@link #union(UnionFixture)}):</b> both variants evaluate each branch identically; the difference is
	 * that the old path folds all branches into a single query whose combined projection/join structure is expensive to
	 * evaluate, while the split runs each branch as its own trivially-planned query. More properties is the primary
	 * lever - it grows that combined structure.
	 * <p>
	 * The real-world model that motivated the change (a {@code BuildingPart} whose nested contact/geo shapes fold into
	 * one query) had ~9 branches, where the combined query took ~3.5s on that fork. Scale this up if the smaller effect
	 * on the stock RDF4J evaluator - whose combined-query plan degrades less steeply - needs to clear the measurement
	 * noise.
	 */
	public int unionBranches = 5;
	/**
	 * Number of hops in the path from each UNION target down to the focus node the branch property constraints check
	 * ({@code UnionTarget -unionStep...-> focus}). This is the shared prefix every branch repeats. The real-world shape
	 * this models is a single hop ({@code BuildingPart -hasPrimaryContact-> contact}, mirrored here by
	 * {@code hasUsableArea} in the actual BuildingX trace), so this defaults to 1; raising it makes the shared prefix
	 * heavier (evaluated once in the old combined query, once per query in the split) but no longer matches that
	 * real-world shape.
	 */
	public int unionPathLength = 1;
	/**
	 * Number of targets participating in the UNION benchmark.
	 */
	public int unionTargets = 10000;

	private List<Statement> nodeKindData;
	private List<Statement> stringData;
	private List<Statement> unionData;
	private List<Statement> unionLightData;
	private List<Statement> combinedData;

	/*
	 * --------------- * Setup * --------------- *
	 */

	/**
	 * Generates all data once for the whole trial. Data generation is intentionally kept out of the measured region;
	 * only the load + validation of this pre-generated data is measured.
	 */
	@Setup(Level.Trial)
	public void setUp() throws Exception {
		((Logger) LoggerFactory.getLogger(ShaclSailConnection.class.getName()))
				.setLevel(ch.qos.logback.classic.Level.INFO);
		nodeKindData = createNodeKindData();
		stringData = createStringData();
		unionData = createUnionData();
		unionLightData = createUnionLightData();
		combinedData = createCombinedData();
		System.gc();
		Thread.sleep(100);
	}

	/*
	 * --------------- * NodeKind benchmark * --------------- *
	 */

	/**
	 * Exercises the NodeKindConstraintComponent with a very large number of values.
	 *
	 * <p>
	 * Before the change, every value was evaluated using a considerably larger boolean expression involving
	 * isIRI/isLiteral/isBlank and membership in several SHACL node-kind IRIs.
	 *
	 * <p>
	 * After the change, the node kind is resolved once while the SPARQL query is generated and the evaluator receives a
	 * simple expression such as {@code isIRI(?value)}.
	 *
	 * <p>
	 * The data deliberately mixes value types (IRIs, literals and blank nodes) so the benchmark exercises
	 * {@code isIRI}, {@code isLiteral} and {@code isBlank} rather than only the all-IRI path. A live TRACE dump of a
	 * real BuildingX validation plan (~13MB, thousands of shapes) showed 222 {@code sh:nodeKind} checks in production
	 * and <b>not one</b> of them was an OR-combination ({@code IRIOrLiteral}/{@code BlankNodeOrIRI}/
	 * {@code BlankNodeOrLiteral}) - every single one was a plain {@code IRI}/{@code Literal}/{@code BlankNode} check.
	 * The OR-combination branches previously here were dropped for that reason: they exercise a code path the
	 * resolved-filter implementation still has to support, but not one real shapes are observed to use, so keeping them
	 * here made this "load" benchmark heavier than the load it claims to model.
	 */
	@Benchmark
	public void nodeKind(NodeKindFixture fixture) throws Exception {
		fixture.loadAndValidate();
	}

	/*
	 * --------------- * xsd:string benchmark * --------------- *
	 */

	/**
	 * Exercises xsd:string validation with large literals.
	 *
	 * <p>
	 * The old implementation eventually called:
	 *
	 * <pre>
	 *     literal.stringValue()
	 *     XMLDatatypeUtil.isValidValue(...)
	 * </pre>
	 *
	 * for every xsd:string value. The optimized implementation recognizes xsd:string as having no lexical constraints
	 * and skips that, the intent being to avoid materializing the lexical form.
	 *
	 * <p>
	 * <b>Why this shows almost no improvement on RDF4J, and when it would:</b> the "avoid materializing the lexical
	 * form" gain only exists for a store that lazily loads the lexical form separately from the datatype (e.g. the
	 * datatype in an index and a large label fetched out-of-line only when {@code stringValue()} is called). <b>None of
	 * RDF4J's stores work that way</b> - MemoryStore keeps the whole literal in memory, and NativeStore/LmdbStore store
	 * the label, language and datatype in a single value record that is read as a unit, so reading the datatype (which
	 * the validation must do anyway) has already materialized the label. On top of that,
	 * {@code XMLDatatypeUtil.isValidValue(value, XSD.STRING)} does not even scan the string - it returns {@code true}
	 * immediately - so the old path did no per-character work for xsd:string either. The net effect on any RDF4J store
	 * is therefore within measurement noise; the optimization is aimed at stores with lazy lexical-form loading, not at
	 * the stock RDF4J stores.
	 */
	@Benchmark
	public void xsdString(StringFixture fixture) {
		fixture.loadAndValidate();
	}

	/*
	 * --------------- * UNION benchmark * --------------- *
	 */

	/**
	 * Exercises a node shape whose focus node (reached from the target through a path) carries many property
	 * constraints - the shape whose validation query the split actually targets. This mirrors the real-world model
	 * behind the change: a target reached via a path to a blank-node focus ({@code UnionTarget -unionStep...-> focus},
	 * like {@code BuildingPart -hasPrimaryContact-> contact} with the outer property being
	 * {@code sh:maxCount 1 ; sh:nodeKind sh:BlankNode ; sh:node ...}), whose focus node has N property constraints
	 * ({@code ex:unionProp_0..N-1}, each {@code sh:minCount 1 ; sh:maxCount 1 ; sh:datatype xsd:string} like
	 * {@code bx:PrimaryContactShape}'s {@code name}). Every constraint - each property's datatype/minCount/maxCount,
	 * plus the outer property's own checks - is a branch, and all branches share the
	 * {@code ?target a UnionTarget . ?target <path> ?focus} prefix.
	 *
	 * <p>
	 * <b>Where the difference comes from:</b> the old path folds all N branches into a single SPARQL query - the shared
	 * prefix plus an N-way {@code UNION} - and the new path runs each branch as an independent query, merging the rows
	 * in Java ({@link org.eclipse.rdf4j.sail.shacl.ast.planNodes.SequentialPrefetchPlanNode}, de-duplicating by
	 * {@code ValidationTuple} equality). <b>Both evaluate each branch over exactly the same data</b> - the win is not a
	 * reduction of per-branch work. It is that the single combined query gets planned into one large projection/join
	 * structure that the evaluator is slow to run, whereas each independent branch query gets a trivial plan. On the
	 * author's fork the <em>whole</em> combined query took ~3.5s, while a <em>single</em> split sub-query ran in under
	 * 0.1s - so the split's end-to-end cost is roughly N sub-queries plus the Java merge, still far below the combined
	 * query when its plan degrades super-linearly (as on that fork). On the stock RDF4J evaluator the combined query
	 * degrades less, so the measured gap is smaller and grows with {@link #unionBranches}.
	 */
	@Benchmark
	public void union(UnionFixture fixture) throws Exception {
		fixture.loadAndValidate();
	}

	/**
	 * A lighter UNION workload that isolates the split signal: the same {@code sh:node} nested shape with N property
	 * constraints (so the branches still fold into one query), but <b>without</b> any rdfs inference - targets are
	 * typed directly as {@code ex:UnionLightTarget}, there is no {@code sh:or}/{@code sh:class} and no sub-property.
	 * <p>
	 * The enriched {@link #union(UnionFixture)} workload is inference-dominated on the stock RDF4J evaluator, which
	 * hides the split; this variant keeps the split as the dominant cost so its effect is measurable on RDF4J.
	 */
	@Benchmark
	public void unionLight(UnionLightFixture fixture) {
		fixture.loadAndValidate();
	}

	/*
	 * --------------- * Combined benchmark * --------------- *
	 */

	/**
	 * A realistic combined workload containing all three optimized paths.
	 *
	 * <p>
	 * This should not be used as the only measurement. The individual benchmarks above are the ones that explain where
	 * the improvement comes from.
	 */
	@Benchmark
	public void combined(CombinedFixture fixture) {
		fixture.loadAndValidate();
	}

	/*
	 * --------------- * Data generation * --------------- *
	 */

	private List<Statement> createNodeKindData() {
		List<Statement> statements = new ArrayList<>(nodeKindTargets * (3 + nodeKindBlankProperties));

		// One property per sh:nodeKind variant actually observed in production (see nodeKind(NodeKindFixture)) - no
		// OR-combinations. Each value has the type its constraint requires, so all data stays valid and the Bulk
		// validation runs the full sweep instead of aborting on the first violation.
		IRI iriProp = VF.createIRI(EX + "iriValue");
		IRI literalProp = VF.createIRI(EX + "literalValue");
		IRI blankProp = VF.createIRI(EX + "blankValue");

		// Additional BlankNode-kind properties (see #nodeKindBlankProperties) so the workload is dominated by the old
		// resolved-filter ladder's most expensive case (isIRI and isLiteral both fail before isBlank succeeds).
		IRI[] extraBlankProps = new IRI[nodeKindBlankProperties];
		for (int b = 0; b < nodeKindBlankProperties; b++) {
			extraBlankProps[b] = VF.createIRI(EX + "blankValue_" + b);
		}

		for (int i = 0; i < nodeKindTargets; i++) {
			IRI subject = VF.createIRI(EX + "nodeKindTarget_" + i);
			// Typed as a leaf subclass so sh:targetClass ex:NodeKindTarget selects it only via subClassOf inference.
			statements.add(VF.createStatement(subject, RDF.TYPE,
					VF.createIRI(EX + "NodeKindTargetKind_" + (i % TARGET_KINDS))));

			statements.add(VF.createStatement(subject, iriProp, VF.createIRI(EX + "iri_" + i)));
			statements.add(VF.createStatement(subject, literalProp, VF.createLiteral("literal_" + i)));
			statements.add(VF.createStatement(subject, blankProp, VF.createBNode()));
			for (IRI extraBlankProp : extraBlankProps) {
				statements.add(VF.createStatement(subject, extraBlankProp, VF.createBNode()));
			}
		}
		return statements;
	}

	private List<Statement> createStringData() {
		List<Statement> statements = new ArrayList<>(stringTargets * 3);
		IRI valueProperty = VF.createIRI(EX + "largeString");
		IRI integerProperty = VF.createIRI(EX + "someInteger");
		String value = createLargeString(stringLiteralSize);
		for (int i = 0; i < stringTargets; i++) {
			IRI subject = VF.createIRI(EX + "stringTarget_" + i);
			// Typed as a leaf subclass so sh:targetClass ex:StringTarget selects it only via subClassOf inference.
			statements.add(VF.createStatement(subject, RDF.TYPE,
					VF.createIRI(EX + "StringTargetKind_" + (i % TARGET_KINDS))));
			statements.add(VF.createStatement(subject, valueProperty, VF.createLiteral(value)));
			// VF.createLiteral(int) would produce xsd:int, not xsd:integer - the shape below declares
			// sh:datatype xsd:integer, so the literal's datatype must match that exactly.
			statements.add(
					VF.createStatement(subject, integerProperty, VF.createLiteral(Integer.toString(i), XSD.INTEGER)));
		}
		return statements;
	}

	private List<Statement> createUnionData() {
		// Mirrors the real-world shape whose validation query the split targets (cf. the BuildingX BuildingPart ->
		// hasPrimaryContact -> contact{phone,eMail,name,...} model): a node shape whose target is reached through a
		// path
		// (UnionTarget -unionStep...-> focus) and whose focus node carries many property constraints. Each focus
		// property
		// becomes one UNION branch, and every branch shares the same target+path prefix
		// (?t0 a UnionTarget . ?t0 <path> ?focus). The old code folds all branches into a single query - shared prefix
		// +
		// an N-way UNION - whose combined projection/join structure is slow to evaluate; the split runs each branch as
		// an
		// independent, trivially-planned query. All values are valid xsd:string, so the full validation sweep runs.
		int pathLength = Math.max(1, unionPathLength);
		List<Statement> statements = new ArrayList<>(unionTargets * (pathLength + unionBranches + 3));
		IRI isUnionOfSpecific = VF.createIRI(EX + "isUnionOfSpecific");
		IRI refLeafA = VF.createIRI(EX + "UnionRefLeafA");
		IRI refLeafB = VF.createIRI(EX + "UnionRefLeafB");
		for (int i = 0; i < unionTargets; i++) {
			IRI target = VF.createIRI(EX + "unionTarget_" + i);
			// Type the target as a leaf subclass of ex:UnionTarget, so sh:targetClass ex:UnionTarget only selects it
			// via
			// rdfs:subClassOf inference.
			statements.add(VF.createStatement(target, RDF.TYPE,
					VF.createIRI(EX + "UnionTargetKind_" + (i % TARGET_KINDS))));

			// sh:or/sh:class + sub-property inference: the value is reached via the ex:isUnionOfSpecific sub-property
			// and
			// typed as a leaf of ex:UnionRefA/B, so both the property triple and the class match need inference.
			IRI ref = VF.createIRI(EX + "unionRef_" + i);
			statements.add(VF.createStatement(target, isUnionOfSpecific, ref));
			statements.add(VF.createStatement(ref, RDF.TYPE, (i % 2 == 0) ? refLeafA : refLeafB));

			// target -unionStep_0-> ... -unionStep_(L-1)-> focus (a blank node, as bx:hasPrimaryContact is in the
			// model)
			Resource node = target;
			for (int hop = 0; hop < pathLength; hop++) {
				Resource next = (hop == pathLength - 1)
						? VF.createBNode()
						: VF.createIRI(EX + "unionNode_" + i + "_" + hop);
				statements.add(VF.createStatement(node, VF.createIRI(EX + "unionStep_" + hop), next));
				node = next;
			}

			// Exactly one xsd:string value per focus property, satisfying sh:minCount 1 + sh:maxCount 1 + sh:datatype.
			for (int b = 0; b < unionBranches; b++) {
				statements.add(VF.createStatement(node, VF.createIRI(EX + "unionProp_" + b),
						VF.createLiteral("v" + i + "_" + b)));
			}
		}
		return statements;
	}

	// Lighter UNION data: same target -> blank-node focus -> N xsd:string properties (so the branches still fold into
	// one
	// query), but the target is typed DIRECTLY as ex:UnionLightTarget and there is no sh:or/sub-property - so no rdfs
	// inference is involved and the split stays the dominant cost.
	private List<Statement> createUnionLightData() {
		int pathLength = Math.max(1, unionPathLength);
		List<Statement> statements = new ArrayList<>(unionTargets * (pathLength + 1 + unionBranches));
		IRI type = VF.createIRI(EX + "UnionLightTarget");
		for (int i = 0; i < unionTargets; i++) {
			IRI target = VF.createIRI(EX + "unionLightTarget_" + i);
			statements.add(VF.createStatement(target, RDF.TYPE, type));

			Resource node = target;
			for (int hop = 0; hop < pathLength; hop++) {
				Resource next = (hop == pathLength - 1)
						? VF.createBNode()
						: VF.createIRI(EX + "unionLightNode_" + i + "_" + hop);
				statements.add(VF.createStatement(node, VF.createIRI(EX + "unionLightStep_" + hop), next));
				node = next;
			}
			for (int b = 0; b < unionBranches; b++) {
				statements.add(VF.createStatement(node, VF.createIRI(EX + "unionLightProp_" + b),
						VF.createLiteral("v" + i + "_" + b)));
			}
		}
		return statements;
	}

	private List<Statement> createCombinedData() {
		List<Statement> nodeKind = createNodeKindData();
		List<Statement> string = createStringData();
		List<Statement> union = createUnionData();
		List<Statement> combined = new ArrayList<>(nodeKind.size() + string.size() + union.size());
		combined.addAll(nodeKind);
		combined.addAll(string);
		combined.addAll(union);
		return combined;
	}

	private static String createLargeString(int size) {
		StringBuilder sb = new StringBuilder(size);
		String chunk = "benchmark-value-without-whitespace-" +
				"abcdefghijklmnopqrstuvwxyz-" +
				"0123456789-";
		while (sb.length() < size) {
			sb.append(chunk);
		}
		return sb.substring(0, size);
	}

	/*
	 * --------------- * SHACL shapes * --------------- *
	 */

	private String nodeKindShapes() {
		// Each property shape carries only sh:nodeKind (no count constraints) so the measurement isolates the
		// NodeKindConstraintComponent filter. Only IRI/Literal/BlankNode are modeled - see nodeKind(NodeKindFixture)
		// for why the OR-combination variants were dropped (unrepresented in the real validation plan checked).
		StringBuilder sb = new StringBuilder();
		sb.append("@prefix ex: <" + EX + "> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n\n" +
				"ex:NodeKindShape a sh:NodeShape ;\n" +
				" sh:targetClass ex:NodeKindTarget ;\n" +
				" sh:property [ sh:path ex:iriValue ; sh:nodeKind sh:IRI ; ] ;\n" +
				" sh:property [ sh:path ex:literalValue ; sh:nodeKind sh:Literal ; ] ;\n" +
				" sh:property [ sh:path ex:blankValue ; sh:nodeKind sh:BlankNode ; ]");
		// Extra BlankNode-kind properties (see #nodeKindBlankProperties) so the workload is dominated by the old
		// ladder's worst case (isIRI, then isLiteral, both fail before isBlank succeeds) rather than averaged evenly
		// across all three sh:nodeKind kinds.
		for (int b = 0; b < nodeKindBlankProperties; b++) {
			sb.append(" ;\n sh:property [ sh:path ex:blankValue_").append(b).append(" ; sh:nodeKind sh:BlankNode ; ]");
		}
		sb.append(" .\n");
		return sb.toString();
	}

	private String stringShapes() {
		// A second property with a non-string datatype: the real validation plan's datatype checks are dominated by
		// xsd:string (98 occurrences, all short-circuited - see xsdString(StringFixture)) but also include
		// non-string datatypes (geo:wktLiteral, xsd:integer, xsd:boolean, xsd:date/gYear) that still go through the
		// full valueConformsToXsdDatatypeFunction conformance check the xsd:string shortcut skips. xsd:integer here
		// stands in for that "heavy" path so this fixture isn't exclusively measuring the shortcut it's meant to
		// isolate against something else that's actually there in production too.
		return "@prefix ex: <" + EX + "> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n\n" +
				"ex:StringShape a sh:NodeShape ;\n" +
				" sh:targetClass ex:StringTarget ;\n" +
				" sh:property [\n" +
				" sh:path ex:largeString ;\n" +
				" sh:minCount 1 ;\n" +
				" sh:maxCount 1 ;\n" +
				" sh:datatype xsd:string ;\n" +
				" ] ;\n" +
				" sh:property [\n" +
				" sh:path ex:someInteger ;\n" +
				" sh:minCount 1 ;\n" +
				" sh:maxCount 1 ;\n" +
				" sh:datatype xsd:integer ;\n" +
				" ] .\n";
	}

	private String unionShapes() {
		int pathLength = Math.max(1, unionPathLength);
		StringBuilder sb = new StringBuilder();
		sb.append("@prefix ex: <" + EX + "> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n\n" +
				"ex:UnionShape a sh:NodeShape ;\n");
		// A live TRACE dump of a real BuildingX validation query showed its target-class match as
		// `?target a ?var . FILTER(?var IN (<Room>, <RoomCooling>, ...))` - i.e. the shape declares its leaf
		// subclasses directly as multiple sh:targetClass values (see TargetClass.getTargetQueryFragment), not a
		// single superclass resolved through rdfs:subClassOf inference. A single `sh:targetClass ex:UnionTarget`
		// here does NOT reproduce that: with the default RdfsSubClassOfReasoner it still compiles to a plain
		// `?target a <ex:UnionTarget> .` triple (confirmed by dumping the generated query), missing the IN-list's
		// extra per-branch cost. Declaring every leaf kind explicitly reproduces the real shared-prefix shape.
		for (int k = 0; k < TARGET_KINDS; k++) {
			sb.append(" sh:targetClass ex:UnionTargetKind_").append(k).append(" ;\n");
		}
		sb.append(" sh:property [\n" +
				"  sh:path ");
		// Sequence path ( ex:unionStep_0 ... ) from the target down to the focus; a single hop is the bare predicate.
		if (pathLength == 1) {
			sb.append("ex:unionStep_0");
		} else {
			sb.append("(");
			for (int hop = 0; hop < pathLength; hop++) {
				sb.append(" ex:unionStep_").append(hop);
			}
			sb.append(" )");
		}
		// Outer property mirrors bx:BuildingPartShape's hasPrimaryContact: maxCount 1 + nodeKind sh:BlankNode + sh:node
		// to
		// a nested shape. The focus node is validated by that nested node shape, whose property constraints all get
		// folded
		// into one UNION sharing the target+path prefix above - this is the structure the split targets.
		sb.append(" ;\n  sh:maxCount 1 ;\n  sh:nodeKind sh:BlankNode ;\n  sh:node ex:UnionFocusShape ;\n ] ;\n");
		// A second property that exercises rdfs inference: reached via ex:isUnionOf (materialized from the
		// ex:isUnionOfSpecific sub-property in the data) and validated by an sh:or over two classes matched through the
		// subclass hierarchy - mirrors bx:RoomShape's "isRoomOf sh:or ( [class Floor] [class FloorArea] )".
		sb.append("""
				 sh:property [
				  sh:path ex:isUnionOf ;
				  sh:minCount 1 ;
				  sh:maxCount 1 ;
				  sh:nodeKind sh:IRI ;
				  sh:or ( [ sh:class ex:UnionRefA ] [ sh:class ex:UnionRefB ] ) ;
				 ] .

				""");
		sb.append("ex:UnionFocusShape a sh:NodeShape ;\n");
		// Each focus property mirrors bx:PrimaryContactShape's name (nodeKind + minCount 1 + maxCount 1), so it emits a
		// nodeKind, a minCount (NOT EXISTS) and a maxCount branch - the same branch mix seen in the real split. Uses
		// sh:nodeKind sh:Literal rather than sh:datatype xsd:string: the real trace's per-property branches compile to
		// a plain !isLiteral(?value)/!isIRI(?value) check (NodeKindConstraintComponent), not the heavier
		// datatype-conformance-function check sh:datatype would add.
		for (int b = 0; b < unionBranches; b++) {
			sb.append("  sh:property [ sh:path ex:unionProp_")
					.append(b)
					.append(" ; sh:minCount 1 ; sh:maxCount 1 ; sh:nodeKind sh:Literal ]")
					.append(b == unionBranches - 1 ? " .\n" : " ;\n");
		}
		return sb.toString();
	}

	// Lighter UNION shape: the same sh:node nested shape with N datatype properties (so the branches still fold into
	// one
	// query and the split applies), but plain sh:targetClass on the directly-typed ex:UnionLightTarget and no
	// sh:or/sh:class - i.e. no rdfs inference. Use this to see the split effect on RDF4J without the inference cost.
	private String unionLightShapes() {
		int pathLength = Math.max(1, unionPathLength);
		StringBuilder sb = new StringBuilder();
		sb.append("@prefix ex: <" + EX + "> .\n" +
				"@prefix sh: <http://www.w3.org/ns/shacl#> .\n" +
				"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n\n" +
				"ex:UnionLightShape a sh:NodeShape ;\n" +
				" sh:targetClass ex:UnionLightTarget ;\n" +
				" sh:property [\n" +
				"  sh:path ");
		if (pathLength == 1) {
			sb.append("ex:unionLightStep_0");
		} else {
			sb.append("(");
			for (int hop = 0; hop < pathLength; hop++) {
				sb.append(" ex:unionLightStep_").append(hop);
			}
			sb.append(" )");
		}
		sb.append(" ;\n  sh:maxCount 1 ;\n  sh:nodeKind sh:BlankNode ;\n  sh:node ex:UnionLightFocusShape ;\n ] .\n\n");
		sb.append("ex:UnionLightFocusShape a sh:NodeShape ;\n");
		for (int b = 0; b < unionBranches; b++) {
			sb.append("  sh:property [ sh:path ex:unionLightProp_")
					.append(b)
					.append(" ; sh:minCount 1 ; sh:maxCount 1 ; sh:datatype xsd:string ]")
					.append(b == unionBranches - 1 ? " .\n" : " ;\n");
		}
		return sb.toString();
	}

	private String combinedShapes() {
		return nodeKindShapes() + stringShapes() + unionShapes();
	}

	/*
	 * --------------- * Validation infrastructure * --------------- *
	 */

	// Shared, stateless helpers. These are static (not a shared @State base) on purpose: a JMH @State class cannot be
	// abstract, and its state fields must live in the @State class itself - so each concrete fixture below is a
	// self-contained @State holding its own repository, and delegates the loading/validation to these helpers.

	/**
	 * Creates a fresh repository and loads the ontology and shapes (plus {@code data} when non-null) with validation
	 * disabled - i.e. entirely outside any measured region.
	 */
	// Maps each created repository to its LmdbStore temp dir, so shutDownQuietly() can delete the dir after shutdown.
	private static final Map<SailRepository, Path> TEMP_DIRS = Collections.synchronizedMap(new IdentityHashMap<>());

	private static SailRepository newLoadedRepository(
			ShaclSparqlValidationOptimizationsBenchmark bench, String shapes,
			List<Statement> data) {
		Path dataDir = newTempDir();
		ShaclSail sail = new ShaclSail(bench.createBaseSail(dataDir.toFile()));
		sail.setShapesGraphs(Set.of(RDF4J.SHACL_SHAPE_GRAPH));
		sail.setEclipseRdf4jShaclExtensions(true);
		sail.setDashDataShapes(true);
		sail.setSerializableValidation(false);
		sail.setParallelValidation(true);
		sail.setCacheSelectNodes(true);
		sail.setPerformanceLogging(false);
		SailRepository repository = new SailRepository(sail);
		repository.init();
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Disabled);
			connection.add(new StringReader(bench.ontology()), "", RDFFormat.TURTLE);
			connection.add(new StringReader(shapes), "", RDFFormat.TURTLE, RDF4J.SHACL_SHAPE_GRAPH);
			if (data != null) {
				connection.add(data);
			}
			connection.commit();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		TEMP_DIRS.put(repository, dataDir);
		return repository;
	}

	private static void shutDownQuietly(SailRepository repository) {
		if (repository != null) {
			repository.shutDown();
			deleteRecursively(TEMP_DIRS.remove(repository));
		}
	}

	private static Path newTempDir() {
		try {
			return Files.createTempDirectory("shacl-bench-lmdb");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void deleteRecursively(Path dir) {
		if (dir == null) {
			return;
		}
		try (Stream<Path> paths = Files.walk(dir)) {
			paths.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.deleteIfExists(p);
				} catch (IOException ignored) {
					// best-effort cleanup of a temp dir; leftover files are harmless
				}
			});
		} catch (IOException ignored) {
			// best-effort cleanup
		}
	}

	/** The measured operation for the load + validation fixtures: load the data and run the Bulk SPARQL validation. */
	private static void bulkLoadAndValidate(SailRepository repository, List<Statement> data) {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(ShaclSail.TransactionSettings.ValidationApproach.Bulk,
					ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation,
					ShaclSail.TransactionSettings.PerformanceHint.CacheEnabled);
			connection.add(data);
			connection.commit();
		}
	}

	/**
	 * The measured operation for the validation-only fixtures: re-validate the already-populated store via
	 * {@link ShaclSailConnection#revalidate()}.
	 *
	 * <p>
	 * <b>This runs the same Bulk validation, not a different code path.</b> A Bulk commit validates via
	 * {@code validate(shapes, shapesModifiedInCurrentTransaction || isBulkValidation())} =
	 * {@code validate(shapes, true)}, and {@code revalidate()} calls exactly {@code validate(shapes, true)} (i.e.
	 * {@code validateEntireBaseSail}). So the generated SPARQL and the validation work are identical to the Bulk load
	 * benchmarks; the only difference is that this reaches that validation directly, over already-loaded data, instead
	 * of through a commit with a delta.
	 *
	 * <p>
	 * That is also why no {@link ShaclSail.TransactionSettings} are passed to {@code begin()}: those only configure
	 * <em>commit-time</em> validation, which {@code revalidate()} bypasses. Parallel validation and node caching still
	 * apply - they are read from the sail-level settings ({@code setParallelValidation}/{@code setCacheSelectNodes} in
	 * {@link #newLoadedRepository}), which {@code begin()} seeds into the transaction defaults.
	 *
	 * <p>
	 * {@code begin()} is needed only because {@code revalidate()} requires an active transaction; nothing is added or
	 * removed, so the transaction is ended with {@code rollback()} (discard) rather than {@code commit()} - a commit
	 * would run the sail's commit-time validation/listener machinery over an empty delta, which we do not want in the
	 * measured region.
	 *
	 * <p>
	 * The declared return of {@code revalidate()} is the {@code @Deprecated ValidationReport} (deprecated only because
	 * it is planned to move packages), but {@code performValidation} always constructs a {@link LazyValidationReport},
	 * so we work with that non-deprecated subtype here to avoid the deprecation noise.
	 */
	private static LazyValidationReport revalidate(SailRepository repository) {
		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(
					ShaclSail.TransactionSettings.ValidationApproach.Bulk,
					ShaclSail.TransactionSettings.PerformanceHint.ParallelValidation,
					ShaclSail.TransactionSettings.PerformanceHint.CacheEnabled);
			LazyValidationReport report = (LazyValidationReport) ((ShaclSailConnection) connection.getSailConnection())
					.revalidate();
			connection.rollback();
			return report;
		}
	}

	/*
	 * Load + validation fixtures: a fresh store per invocation (a Bulk commit leaves the data behind), with shapes +
	 * ontology loaded outside the measured region; only the data load + Bulk validation is measured.
	 */

	@State(Scope.Thread)
	public static class NodeKindFixture {
		private SailRepository repository;
		private List<Statement> data;

		@Setup(Level.Invocation)
		public void setup(ShaclSparqlValidationOptimizationsBenchmark bench) {
			data = bench.nodeKindData;
			repository = newLoadedRepository(bench, bench.nodeKindShapes(), null);
		}

		@TearDown(Level.Invocation)
		public void tearDown() {
			shutDownQuietly(repository);
			repository = null;
		}

		public void loadAndValidate() {
			bulkLoadAndValidate(repository, data);
		}
	}

	@State(Scope.Thread)
	public static class StringFixture {
		private SailRepository repository;
		private List<Statement> data;

		@Setup(Level.Invocation)
		public void setup(ShaclSparqlValidationOptimizationsBenchmark bench) {
			data = bench.stringData;
			repository = newLoadedRepository(bench, bench.stringShapes(), null);
		}

		@TearDown(Level.Invocation)
		public void tearDown() {
			shutDownQuietly(repository);
			repository = null;
		}

		public void loadAndValidate() {
			bulkLoadAndValidate(repository, data);
		}
	}

	@State(Scope.Thread)
	public static class UnionFixture {
		private SailRepository repository;
		private List<Statement> data;

		@Setup(Level.Invocation)
		public void setup(ShaclSparqlValidationOptimizationsBenchmark bench) {
			data = bench.unionData;
			repository = newLoadedRepository(bench, bench.unionShapes(), null);
		}

		@TearDown(Level.Invocation)
		public void tearDown() {
			shutDownQuietly(repository);
			repository = null;
		}

		public void loadAndValidate() {
			bulkLoadAndValidate(repository, data);
		}
	}

	@State(Scope.Thread)
	public static class UnionLightFixture {
		private SailRepository repository;
		private List<Statement> data;

		@Setup(Level.Invocation)
		public void setup(ShaclSparqlValidationOptimizationsBenchmark bench) {
			data = bench.unionLightData;
			repository = newLoadedRepository(bench, bench.unionLightShapes(), null);
		}

		@TearDown(Level.Invocation)
		public void tearDown() {
			shutDownQuietly(repository);
			repository = null;
		}

		public void loadAndValidate() {
			bulkLoadAndValidate(repository, data);
		}
	}

	@State(Scope.Thread)
	public static class CombinedFixture {
		private SailRepository repository;
		private List<Statement> data;

		@Setup(Level.Invocation)
		public void setup(ShaclSparqlValidationOptimizationsBenchmark bench) {
			data = bench.combinedData;
			repository = newLoadedRepository(bench, bench.combinedShapes(), null);
		}

		@TearDown(Level.Invocation)
		public void tearDown() {
			shutDownQuietly(repository);
			repository = null;
		}

		public void loadAndValidate() {
			bulkLoadAndValidate(repository, data);
		}
	}

	/*
	 * --------------- * Validation-only benchmarks * --------------- *
	 */

	/**
	 * Same workloads as above, but the data is loaded once (with validation disabled) in {@link Level#Trial} setup and
	 * the measured operation only re-validates the already-populated store. Use these to look at the validation cost
	 * itself - e.g. the NodeKind filter - without the data load, RDFS inference and store initialization dominating
	 * (and dwarfing) the signal, as they do in the load + validation benchmarks above.
	 */
	@Benchmark
	public LazyValidationReport nodeKindValidationOnly(NodeKindValidationOnlyFixture fixture) {
		return fixture.validate();
	}

	@Benchmark
	public LazyValidationReport xsdStringValidationOnly(StringValidationOnlyFixture fixture) {
		return fixture.validate();
	}

	@Benchmark
	public LazyValidationReport unionValidationOnly(UnionValidationOnlyFixture fixture) {
		return fixture.validate();
	}

	@Benchmark
	public LazyValidationReport unionLightValidationOnly(UnionLightValidationOnlyFixture fixture) {
		return fixture.validate();
	}

	@Benchmark
	public LazyValidationReport combinedValidationOnly(CombinedValidationOnlyFixture fixture) {
		return fixture.validate();
	}

	/*
	 * Validation-only fixtures: the data is loaded once per trial (validation disabled) in Level.Trial setup, and the
	 * measured validate() only re-validates the already-populated store (adding no data). This isolates the validation
	 * from the data load, the RDFS inference and the store initialization. revalidate() is read-only, so one populated
	 * store is validated repeatedly across all warmup/measurement iterations; the store is shut down in Level.Trial
	 * teardown.
	 */

	@State(Scope.Thread)
	public static class NodeKindValidationOnlyFixture {
		private SailRepository repository;

		@Setup(Level.Trial)
		public void setup(ShaclSparqlValidationOptimizationsBenchmark bench) {
			repository = newLoadedRepository(bench, bench.nodeKindShapes(), bench.createNodeKindData());
		}

		@TearDown(Level.Trial)
		public void tearDown() throws InterruptedException {
			shutDownQuietly(repository);
			repository = null;
		}

		public LazyValidationReport validate() {
			return revalidate(repository);
		}
	}

	@State(Scope.Thread)
	public static class StringValidationOnlyFixture {
		private SailRepository repository;

		@Setup(Level.Trial)
		public void setup(ShaclSparqlValidationOptimizationsBenchmark bench) {
			repository = newLoadedRepository(bench, bench.stringShapes(), bench.createStringData());
		}

		@TearDown(Level.Trial)
		public void tearDown() {
			shutDownQuietly(repository);
			repository = null;
		}

		public LazyValidationReport validate() {
			return revalidate(repository);
		}
	}

	@State(Scope.Thread)
	public static class UnionValidationOnlyFixture {
		private SailRepository repository;

		@Setup(Level.Trial)
		public void setup(ShaclSparqlValidationOptimizationsBenchmark bench) {
			repository = newLoadedRepository(bench, bench.unionShapes(), bench.createUnionData());
		}

		@TearDown(Level.Trial)
		public void tearDown() {
			shutDownQuietly(repository);
			repository = null;
		}

		public LazyValidationReport validate() {
			return revalidate(repository);
		}
	}

	@State(Scope.Thread)
	public static class UnionLightValidationOnlyFixture {
		private SailRepository repository;

		@Setup(Level.Trial)
		public void setup(ShaclSparqlValidationOptimizationsBenchmark bench) {
			repository = newLoadedRepository(bench, bench.unionLightShapes(), bench.createUnionLightData());
		}

		@TearDown(Level.Trial)
		public void tearDown() {
			shutDownQuietly(repository);
			repository = null;
		}

		public LazyValidationReport validate() {
			return revalidate(repository);
		}
	}

	@State(Scope.Thread)
	public static class CombinedValidationOnlyFixture {
		private SailRepository repository;

		@Setup(Level.Trial)
		public void setup(ShaclSparqlValidationOptimizationsBenchmark bench) {
			repository = newLoadedRepository(bench, bench.combinedShapes(), bench.createCombinedData());
		}

		@TearDown(Level.Trial)
		public void tearDown() {
			shutDownQuietly(repository);
			repository = null;
		}

		public LazyValidationReport validate() {
			return revalidate(repository);
		}
	}

	protected NotifyingSail createBaseSail(File dataDir) {
		// NativeStore (disk-backed B-Tree) rather than MemoryStore: MemoryStore is documented for < 100,000 triples
		// and,
		// past that, spends its time growing/copying hash tables and thrashing the GC - which at these target counts
		// (hundreds of thousands of targets => millions of triples) dominates and destabilizes the measurement.
		// NativeStore is designed for 100k-100M triples and, with enough RAM, is OS-cached so it behaves like an
		// in-memory store while staying stable. dataDir is a throwaway temp dir deleted in teardown (see
		// shutDownQuietly / deleteRecursively).
		NativeStore store = new NativeStore(dataDir);
		store.setDefaultQueryEvaluationMode(QueryEvaluationMode.STRICT);
		return new SchemaCachingRDFSInferencer(store, true);
	}

	/*
	 * --------------- * Ontology * --------------- *
	 */

	private String ontology() {
		StringBuilder sb = new StringBuilder();
		sb.append("@prefix ex: <" + EX + "> .\n" +
				"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n\n" +
				"ex:NodeKindTarget rdfs:subClassOf ex:BenchmarkTarget .\n" +
				"ex:StringTarget rdfs:subClassOf ex:BenchmarkTarget .\n" +
				"ex:UnionTarget rdfs:subClassOf ex:BenchmarkTarget .\n\n");

		// Leaf subclasses per target class: every workload types its targets as these leaves, so the shapes'
		// sh:targetClass only selects them through rdfs:subClassOf inference (like RoomShape/targetClass Room over
		// OfficeRoom/MeetingRoom instances).
		for (int k = 0; k < TARGET_KINDS; k++) {
			sb.append("ex:NodeKindTargetKind_").append(k).append(" rdfs:subClassOf ex:NodeKindTarget .\n");
			sb.append("ex:StringTargetKind_").append(k).append(" rdfs:subClassOf ex:StringTarget .\n");
			sb.append("ex:UnionTargetKind_").append(k).append(" rdfs:subClassOf ex:UnionTarget .\n");
		}

		// --- Extra inference for the UNION workload (mirrors the BuildingX sh:or/sh:class + sub-property model) ---
		// Two-branch reference class hierarchy for the sh:or/sh:class constraint (like Floor / FloorArea). Values are
		// typed as the leaves, so sh:class ex:UnionRefA/B matches only via inference.
		sb.append("ex:UnionRefA rdfs:subClassOf ex:UnionRefBase .\n" +
				"ex:UnionRefB rdfs:subClassOf ex:UnionRefBase .\n" +
				"ex:UnionRefLeafA rdfs:subClassOf ex:UnionRefA .\n" +
				"ex:UnionRefLeafB rdfs:subClassOf ex:UnionRefB .\n");
		// Sub-property: the data uses ex:isUnionOfSpecific while the shape's path is ex:isUnionOf, so the triple the
		// constraint checks exists only after rdfs:subPropertyOf inference (like ex:hasGeoPoint -> geo:hasGeometry).
		sb.append("ex:isUnionOfSpecific rdfs:subPropertyOf ex:isUnionOf .\n");
		return sb.toString();
	}
}
