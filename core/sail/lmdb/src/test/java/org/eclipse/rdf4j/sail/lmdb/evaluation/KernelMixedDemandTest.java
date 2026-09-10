/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeKernelIr.*;

import java.util.*;

import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.*;
import org.junit.jupiter.api.Test;

/** Tests the actual IR with a demand-observable delegated relation. Not HTTP/federation integration. */
public class KernelMixedDemandTest {
	static final class Boundary implements KernelPlan {
		int opens, pulls, closes;
		final boolean failOnExcess;

		Boundary(boolean failOnExcess) {
			this.failOnExcess = failOnExcess;
		}

		@Override
		public Cursor open() {
			opens++;
			return new Cursor() {
				boolean emitted, closed;

				@Override
				public int fill(long[] values, int maximum) {
					if (closed)
						throw new AssertionError("read after close");
					if (maximum <= 0)
						return 0;
					pulls++;
					if (emitted) {
						if (failOnExcess)
							throw new AssertionError("delegated relation pulled beyond requested row");
						return 0;
					}
					emitted = true;
					return 1; // identity mapping: no columns, one result
				}

				@Override
				public void close() {
					if (!closed) {
						closed = true;
						closes++;
					}
				}
			};
		}

		@Override
		public void close() {
		}
	}

	static KernelContext append(KernelContext original, Boundary boundary) {
		KernelPlan[] plans = Arrays.copyOf(original.plans, original.plans.length + 1);
		plans[plans.length - 1] = boundary;
		KernelContext result = new KernelContext(original.adjacencies, original.constants, original.entrySlots,
				original.keyDomains, original.keyDomainOffsets, original.keyDomainLengths, original.hooks,
				original.scanner, plans, original.distinctExpected, original.nodePredicates,
				original.dynamicAdjacencies, original.wildcardAdjacencies);
		result.withNodeDomainIntersections(original.nodeDomainIntersections);
		result.cancellation = original.cancellation;
		return result;
	}

	static List<String> bag(long[][] rows) {
		List<String> result = new ArrayList<>();
		for (long[] row : rows)
			result.add(Arrays.toString(row));
		Collections.sort(result);
		return result;
	}

	static void check(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	private void first(String name) throws Exception {
		var fixture = KernelInstructionCoverageTest.cases()
				.stream()
				.filter(c -> c.name().equals(name))
				.findFirst()
				.orElseThrow();
		for (boolean compiled : new boolean[] { false, true }) {
			Kernel original = fixture.shape().get();
			Boundary endpoint = new Boundary(true);
			KernelContext ctx = append(fixture.context().get(), endpoint);
			List<Node> nodes = new ArrayList<>(original.pipeline);
			nodes.add(new PlanRows(ctx.plans.length - 1, new int[0]));
			Kernel ir = new Kernel(original.columnCount, nodes, original.terminal);
			JaninoKernel executable = compiled ? KernelCompilationTestSupport.compile(ir)
					: LmdbNativeKernelInterpreter.forRows(ir);
			try (AutoCloseable ignored = executable::close) {
				executable.bind(ctx);
				long[] row = new long[ir.stride()];
				check(executable.fill(row, 1) == 1, name + " lost first row, compiled=" + compiled);
				check(bag(fixture.expected()).contains(Arrays.toString(row)), name + " fabricated row");
				check(endpoint.opens == 1 && endpoint.pulls == 1, name + " overread");
			} catch (Throwable failure) {
				throw new AssertionError(name + " compiled=" + compiled, failure);
			}
			check(endpoint.closes == 1, name + " leaked delegated cursor");
		}
	}

	@Test
	public void keysOnlyBeforeDelegatedFirstRow() throws Exception {
		first("keys-only");
	}

	@Test
	public void wildcardKeysOnlyBeforeDelegatedFirstRow() throws Exception {
		first("wildcard-keys-only");
	}

	@Test
	public void intersectionDomainBeforeDelegatedFirstRow() throws Exception {
		first("intersection-domain");
	}

	@Test
	public void termsBeforeDelegatedFirstRow() throws Exception {
		first("terms");
	}

	@Test
	public void intersectBeforeDelegatedFirstRow() throws Exception {
		first("intersect");
	}

	@Test
	public void pathBeforeDelegatedFirstRow() throws Exception {
		first("path");
	}

	@Test
	public void leftProbeBeforeDelegatedFirstRow() throws Exception {
		first("left-probe");
	}

	@Test
	public void allExistingRowFixturesComposeOnBothSidesOfDelegatedIdentity() throws Exception {
		for (var fixture : KernelInstructionCoverageTest.cases()) {
			Kernel original = fixture.shape().get();
			if (!(original.terminal instanceof Emit))
				continue;
			for (boolean prefix : new boolean[] { true, false })
				for (boolean compiled : new boolean[] { false, true }) {
					Boundary endpoint = new Boundary(false);
					KernelContext ctx = append(fixture.context().get(), endpoint);
					List<Node> nodes = new ArrayList<>(original.pipeline);
					nodes.add(prefix ? 0 : nodes.size(), new PlanRows(ctx.plans.length - 1, new int[0]));
					Kernel ir = new Kernel(original.columnCount, nodes, original.terminal);
					JaninoKernel executable = compiled ? KernelCompilationTestSupport.compile(ir)
							: LmdbNativeKernelInterpreter.forRows(ir);
					List<String> actual = new ArrayList<>();
					try (AutoCloseable ignored = executable::close) {
						executable.bind(ctx);
						long[] buffer = new long[ir.stride()];
						for (int calls = 0;; calls++) {
							if (calls > 1000)
								throw new AssertionError("nontermination");
							int n = executable.fill(buffer, 1);
							if (n == 0)
								break;
							check(n == 1, "bad fill");
							actual.add(Arrays.toString(buffer.clone()));
						}
					} catch (Throwable failure) {
						throw new AssertionError(fixture.name() + " prefix=" + prefix + " compiled=" + compiled,
								failure);
					}
					Collections.sort(actual);
					check(actual.equals(bag(fixture.expected())), fixture.name() + " prefix=" + prefix + " compiled="
							+ compiled + " " + actual + " != " + bag(fixture.expected()));
					check(endpoint.opens == endpoint.closes, "leaked cursor");
				}
		}
	}

	@Test
	public void optionalNullContinuationSurvivesMultipleDelegatedRows() throws Exception {
		for (boolean group : new boolean[] { false, true })
			for (boolean compiled : new boolean[] { false, true }) {
				var empty = new KernelExpansionContractTest.ArrayView();
				KernelPlan twoRows = new KernelPlan() {
					@Override
					public Cursor open() {
						return new Cursor() {
							int at;

							@Override
							public int fill(long[] b, int max) {
								int n = Math.min(max, 2 - at);
								for (int i = 0; i < n; i++)
									b[i] = 20 + at++;
								return n;
							}

							@Override
							public void close() {
							}
						};
					}

					@Override
					public void close() {
					}
				};
				Node optional = group ? new LeftGroup(List.of(new Probe(0, Operand.constant(0), 1)))
						: new LeftProbe(0, Operand.constant(0), 1);
				Kernel ir = new Kernel(3,
						List.of(new BindAlias(Operand.constant(0), 0), optional, new PlanRows(0, new int[] { 2 })),
						new Emit(new int[] { 0, 1, 2 }, false, OutputMods.none()));
				KernelContext ctx = new KernelContext(new NativeLmdbQuerySource.NativeAdjacency[] { empty },
						new long[] { 7 }, new long[0], null, null, null, new KernelPlan[] { twoRows }, 16);
				check(KernelExpansionContractTest.execute(ir, ctx, compiled, 1)
						.equals(List.of("[7, -1, 20]", "[7, -1, 21]")),
						"null continuation lost, group=" + group + " compiled=" + compiled);
			}
	}

	@Test
	public void identityOptionalAndUnionArmsPreserveIdentityWithoutOverreading() throws Exception {
		for (Node container : List.of(new LeftGroup(List.of(new EnumerateEntry())),
				new Union(List.of(List.of(new EnumerateEntry()), List.of(new EnumerateEntry()))))) {
			for (boolean compiled : new boolean[] { false, true }) {
				Boundary endpoint = new Boundary(true);
				KernelContext ctx = append(new KernelContext(null, new long[] { 7 }, new long[0], null), endpoint);
				Kernel ir = new Kernel(1,
						List.of(new BindAlias(Operand.constant(0), 0), container, new PlanRows(0, new int[0])),
						new Emit(new int[] { 0 }, false, OutputMods.none()));
				JaninoKernel k = compiled ? KernelCompilationTestSupport.compile(ir)
						: LmdbNativeKernelInterpreter.forRows(ir);
				try (AutoCloseable close = k::close) {
					k.bind(ctx);
					long[] b = new long[1];
					check(k.fill(b, 1) == 1 && b[0] == 7, "identity row");
				}
				check(endpoint.pulls == 1 && endpoint.closes == 1,
						"identity arm overread " + container.getClass() + " compiled=" + compiled);
			}
		}
	}

	@Test
	public void identityOptionalAndUnionArmsExhaustExactly() throws Exception {
		for (Node container : List.of(new LeftGroup(List.of(new EnumerateEntry())),
				new Union(List.of(List.of(new EnumerateEntry()), List.of(new EnumerateEntry())))))
			for (boolean compiled : new boolean[] { false, true }) {
				KernelContext ctx = append(new KernelContext(null, new long[] { 7 }, new long[0], null),
						new Boundary(false));
				Kernel ir = new Kernel(1,
						List.of(new PlanRows(0, new int[0]), new BindAlias(Operand.constant(0), 0), container),
						new Emit(new int[] { 0 }, false, OutputMods.none()));
				List<String> actual = KernelExpansionContractTest.execute(ir, ctx, compiled, 1);
				check(actual.equals(container instanceof Union ? List.of("[7]", "[7]") : List.of("[7]")),
						"identity arm replayed " + actual);
			}
	}

	@Test
	public void interpretedDuplicateCountDoesNotSilentlyWrapToZero() throws Exception {
		checkLargeDuplicateCount(false);
	}

	@Test
	public void compiledDuplicateCountDoesNotSilentlyWrapToZero() throws Exception {
		checkLargeDuplicateCount(true);
	}

	private void checkLargeDuplicateCount(boolean compiled) throws Exception {
		int[] indices = { 0, 1, 2 };
		Operand[] keys = { Operand.constant(0), Operand.constant(0), Operand.constant(0) };
		Kernel ir = new Kernel(1, List.of(new Intersect(indices, keys, 0)), new Aggregate(new int[0],
				new AggregateOutput[] { AggregateOutput.countStar() }, null, OutputMods.none()));
		{
			NativeLmdbQuerySource.NativeAdjacency repeated = new NativeLmdbQuerySource.NativeAdjacency() {
				@Override
				public long find(long k) {
					return 1;
				}

				@Override
				public long size(long h) {
					return 1L << 21;
				}

				@Override
				public long neighborAt(long h, long i) {
					if (i < 0 || i >= (1L << 21))
						throw new AssertionError("index");
					return 5;
				}

				@Override
				public long contextAt(long h, long i) {
					return 0;
				}

				@Override
				public boolean runsNeighborOrdered() {
					return true;
				}
			};
			KernelContext ctx = new KernelContext(
					new NativeLmdbQuerySource.NativeAdjacency[] { repeated, repeated, repeated }, new long[] { 7 },
					new long[0], null);
			try {
				KernelExpansionContractTest.execute(ir, ctx, compiled, 1);
				throw new AssertionError("2^63 mappings silently lost, compiled=" + compiled);
			} catch (ArithmeticException expected) {
			}
		}
	}

}
