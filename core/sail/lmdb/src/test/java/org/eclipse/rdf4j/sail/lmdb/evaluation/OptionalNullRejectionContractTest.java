package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeKernelIr.*;

import java.util.*;

import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.*;

/**
 * Regression derived from the independently reproduced r17 audit counterexample. Uses actual r17 IR constructor,
 * interpreter, emitter and javac backend. The adjacency and semantic hook are explicit deterministic fixtures, not an
 * RDF4J/SPARQL integration run.
 */
public final class OptionalNullRejectionContractTest {
	static final class Hooks implements KernelHooks {
		final String predicate;
		int calls;

		Hooks(String predicate) {
			this.predicate = predicate;
		}

		@Override
		public boolean testFilter(int id, long a, long b, long c) {
			calls++;
			return switch (predicate) {
			case "not-bound" -> a == -1;
			case "coalesce-accepts-unbound" -> a == -1 || a == 99;
			case "bound-control" -> a != -1;
			default -> throw new AssertionError(predicate);
			};
		}

		@Override
		public long computeBind(int id, long a, long b) {
			return a;
		}

		@Override
		public int compareValues(long a, long b) {
			return Long.compare(a, b);
		}

		@Override
		public boolean isNumeric(long v) {
			return v != -1;
		}

		@Override
		public double doubleValue(long v) {
			return v;
		}

		@Override
		public void accumulateNumeric(int id, int group, long value) {
		}
	}

	static KernelContext context(String predicate) {
		var a = new NativeLmdbQuerySource.NativeAdjacency() {
			@Override
			public long find(long key) {
				return key == 20 ? 20 : NOT_FOUND;
			}

			@Override
			public long size(long handle) {
				return 1;
			}

			@Override
			public long neighborAt(long handle, long offset) {
				return 99;
			}

			@Override
			public long contextAt(long handle, long offset) {
				return 0;
			}

			@Override
			public boolean runsNeighborOrdered() {
				return true;
			}
		};
		return new KernelContext(new NativeLmdbQuerySource.NativeAdjacency[] { a }, new long[0], new long[0],
				new long[][] { new long[] { 10, 20 } }, new Hooks(predicate));
	}

	static Kernel shape(boolean optimize) {
		System.setProperty(DISTINCT_ROOT_EXISTS_PROPERTY, Boolean.toString(optimize));
		return new Kernel(2, List.of(new EnumerateDomain(0, 0, false, "audit", true),
				new LeftGroup(List.of(new Probe(0, Operand.col(0), 1))),
				new FilterValue(0, new Operand[] { Operand.col(1) })),
				new Aggregate(new int[0], new AggregateOutput[] { AggregateOutput.countDistinct(0) },
						null, OutputMods.none()));
	}

	static long run(Kernel shape, String predicate, boolean compiled) throws Exception {
		JaninoKernel kernel = compiled ? KernelCompilationTestSupport.compile(shape)
				: LmdbNativeKernelInterpreter.forAggregate(shape);
		if (kernel == null)
			throw new AssertionError("kernel declined");
		try {
			kernel.bind(context(predicate));
			long[] row = new long[shape.stride()];
			if (kernel.fill(row, 1) != 1)
				throw new AssertionError("no aggregate row");
			long result = row[0];
			if (kernel.fill(row, 1) != 0)
				throw new AssertionError("extra aggregate row");
			return result;
		} finally {
			kernel.close();
		}
	}

	@org.junit.jupiter.api.Test
	public void nullAcceptingOpaqueFiltersPreserveOptionalAcrossBothTiers() throws Exception {
		String old = System.getProperty(DISTINCT_ROOT_EXISTS_PROPERTY);
		try {
			for (String predicate : List.of("not-bound", "coalesce-accepts-unbound", "bound-control")) {
				long expected = predicate.equals("coalesce-accepts-unbound") ? 2 : 1;
				for (boolean optimize : new boolean[] { false, true })
					for (boolean compiled : new boolean[] { false, true }) {
						long actual = run(shape(optimize), predicate, compiled);
						if (actual != expected)
							throw new AssertionError(predicate + " optimize=" + optimize + " tier=" + compiled
									+ " expected=" + expected + " got=" + actual);
					}
			}
		} finally {
			if (old == null)
				System.clearProperty(DISTINCT_ROOT_EXISTS_PROPERTY);
			else
				System.setProperty(DISTINCT_ROOT_EXISTS_PROPERTY, old);
		}
	}

	@org.junit.jupiter.api.Test
	public void structuralNullRejectionStillPermitsTheWitnessRewrite() throws Exception {
		String old = System.getProperty(DISTINCT_ROOT_EXISTS_PROPERTY);
		try {
			System.setProperty(DISTINCT_ROOT_EXISTS_PROPERTY, "true");
			Kernel ir = new Kernel(2, List.of(new EnumerateDomain(0, 0, false, "control", true),
					new LeftGroup(List.of(new Probe(0, Operand.col(0), 1))),
					new FilterCompareId(false, Operand.col(1), Operand.col(0))),
					new Aggregate(new int[0], new AggregateOutput[] { AggregateOutput.countDistinct(0) }, null,
							OutputMods.none()));
			if (ir.pipeline.stream().anyMatch(n -> n instanceof LeftGroup))
				throw new AssertionError("proven rejection not optimized");
			for (boolean compiled : new boolean[] { false, true })
				if (run(ir, "bound-control", compiled) != 0)
					throw new AssertionError("wrong witness count");
		} finally {
			if (old == null)
				System.clearProperty(DISTINCT_ROOT_EXISTS_PROPERTY);
			else
				System.setProperty(DISTINCT_ROOT_EXISTS_PROPERTY, old);
		}
	}

	@org.junit.jupiter.api.Test
	public void opaqueFilterBeforeStructuralRejectionKeepsNullArmEffects() throws Exception {
		String old = System.getProperty(DISTINCT_ROOT_EXISTS_PROPERTY);
		try {
			System.setProperty(DISTINCT_ROOT_EXISTS_PROPERTY, "true");
			Kernel ir = new Kernel(2, List.of(new EnumerateDomain(0, 0, false, "effects", true),
					new LeftGroup(List.of(new Probe(0, Operand.col(0), 1))),
					new FilterValue(0, new Operand[] { Operand.col(1) }),
					new FilterCompareId(false, Operand.col(1), Operand.col(0))),
					new Aggregate(new int[0], new AggregateOutput[] { AggregateOutput.countDistinct(0) }, null,
							OutputMods.none()));
			if (ir.pipeline.stream().noneMatch(n -> n instanceof LeftGroup))
				throw new AssertionError("opaque effects erased");
			for (boolean compiled : new boolean[] { false, true }) {
				KernelContext ctx = context("coalesce-accepts-unbound");
				JaninoKernel kernel = compiled ? KernelCompilationTestSupport.compile(ir)
						: LmdbNativeKernelInterpreter.forAggregate(ir);
				try {
					kernel.bind(ctx);
					long[] out = new long[1];
					if (kernel.fill(out, 1) != 1 || out[0] != 0)
						throw new AssertionError("wrong filtered count");
					if (((Hooks) ctx.hooks).calls != 2)
						throw new AssertionError("null extension callback skipped");
				} finally {
					kernel.close();
				}
			}
		} finally {
			if (old == null)
				System.clearProperty(DISTINCT_ROOT_EXISTS_PROPERTY);
			else
				System.setProperty(DISTINCT_ROOT_EXISTS_PROPERTY, old);
		}
	}

	@org.junit.jupiter.api.Test
	public void runtimeConstantThatIsNullIsNotAssumedToRejectNullExtension() throws Exception {
		String old = System.getProperty(DISTINCT_ROOT_EXISTS_PROPERTY);
		try {
			System.setProperty(DISTINCT_ROOT_EXISTS_PROPERTY, "true");
			Kernel ir = new Kernel(2, List.of(new EnumerateDomain(0, 0, false, "constant", true),
					new LeftGroup(List.of(new Probe(0, Operand.col(0), 1))),
					new FilterCompareId(false, Operand.col(1), Operand.constant(0))),
					new Aggregate(new int[0], new AggregateOutput[] { AggregateOutput.countDistinct(0) }, null,
							OutputMods.none()));
			if (ir.pipeline.stream().noneMatch(n -> n instanceof LeftGroup))
				throw new AssertionError("runtime NULL assumed bound");
			for (boolean compiled : new boolean[] { false, true }) {
				KernelContext base = context("bound-control");
				KernelContext ctx = new KernelContext(base.adjacencies, new long[] { -1 }, new long[0], base.keyDomains,
						base.hooks);
				JaninoKernel kernel = compiled ? KernelCompilationTestSupport.compile(ir)
						: LmdbNativeKernelInterpreter.forAggregate(ir);
				try {
					kernel.bind(ctx);
					long[] out = new long[1];
					if (kernel.fill(out, 1) != 1 || out[0] != 1)
						throw new AssertionError("runtime NULL semantics");
				} finally {
					kernel.close();
				}
			}
		} finally {
			if (old == null)
				System.clearProperty(DISTINCT_ROOT_EXISTS_PROPERTY);
			else
				System.setProperty(DISTINCT_ROOT_EXISTS_PROPERTY, old);
		}
	}
}
