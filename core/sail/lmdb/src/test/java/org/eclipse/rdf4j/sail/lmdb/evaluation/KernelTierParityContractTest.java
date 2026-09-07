/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeKernelIr.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.*;
import org.junit.jupiter.api.Test;

/**
 * Differential tests with fresh state per tier and an independent expected result. Compiled means generated code,
 * never an interpreter fallback. Janino is required by default; the offline harness explicitly selects javac.
 */
public class KernelTierParityContractTest {
	private static Emit emit(int... columns) { return new Emit(columns, false, OutputMods.none()); }
	private static Kernel shape(Node node) { return new Kernel(2, List.of(node), emit(0)); }
	private static JaninoKernel interpreted(Kernel ir) {
		JaninoKernel result = ir.terminal instanceof Emit ? LmdbNativeKernelInterpreter.forRows(ir)
				: LmdbNativeKernelInterpreter.forAggregate(ir);
		if (result == null) throw new AssertionError("interpreter declined test shape " + ir.shapeKey());
		return result;
	}
	private static void eq(Object expected, Object actual) {
		if (!java.util.Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual);
	}
	private static List<String> drain(JaninoKernel kernel, Kernel ir, int batch) {
		long[] buffer = new long[ir.stride() * batch];
		List<String> rows = new ArrayList<>();
		for (int calls = 0; calls < 10000; calls++) {
			int n = kernel.fill(buffer, batch);
			if (n < 0 || n > batch) throw new AssertionError("invalid filled count " + n);
			if (n == 0) return rows;
			for (int i = 0; i < n; i++) rows.add(Arrays.toString(Arrays.copyOfRange(buffer, i * ir.stride(), (i + 1) * ir.stride())));
		}
		throw new AssertionError("kernel failed to exhaust");
	}
	private static List<String> rows(long[]... rows) { return Arrays.stream(rows).map(Arrays::toString).toList(); }
	private static RuntimeException boom(String message) { return new IllegalStateException(message); }

	private static class Hooks implements KernelHooks {
        @Override public boolean testFilter(int id,long a,long b,long c){return true;}
        @Override public long computeBind(int id,long a,long b){return a;}
        @Override public int compareValues(long a,long b){return Long.compare(a,b);}
        @Override public boolean isNumeric(long id){return id!=-1;}
        @Override public double doubleValue(long id){return id;}
        @Override public void accumulateNumeric(int id,int group,long value){}
    }
    private static final class Scan implements KernelScanner {
		final long[][] rows;
		int opens, closes, active, fills;
		RuntimeException fillFailure, closeFailure;
		boolean infinite;
		KernelCancellation cancelOnFill;
		Scan(long[]... rows) { this.rows = rows; }
		@Override public KernelQuadCursor open(int scan, long s, long p, long o, long c) {
			opens++; active++;
			return new KernelQuadCursor() {
				int next; boolean closed;
				@Override public int fill(long[] buffer, int maxRows) {
					if (closed) throw new AssertionError("closed scan read");
					fills++;
					if (fillFailure != null) throw fillFailure;
					if (cancelOnFill != null) cancelOnFill.cancel();
					if (infinite && fills > 1100) throw new AssertionError("unpolled term enumeration");
					int n = infinite ? Math.min(256, maxRows) : Math.min(maxRows, rows.length - next);
					for (int i = 0; i < n; i++) System.arraycopy(rows[infinite ? 0 : next++], 0, buffer, 4 * i, 4);
					return n;
				}
				@Override public void close() {
					if (closed) throw new AssertionError("scan closed twice");
					closed = true; active--; closes++;
					if (closeFailure != null) throw closeFailure;
				}
			};
		}
	}

	private static final class Plan implements KernelPlan {
		final long[][] rows; final long[] inputs = new long[4];
		int opens, closes, active, ownerCloses;
		RuntimeException fillFailure, closeFailure, ownerFailure;
		Plan(long[]... rows) { this.rows = rows; }
		@Override public void setInput(int i, long v) { inputs[i] = v; }
		@Override public long input(int i) { return inputs[i]; }
		@Override public Cursor open() {
			opens++; active++;
			return new Cursor() {
				int next; boolean closed;
				@Override public int fill(long[] buffer, int maxRows) {
					if (fillFailure != null) throw fillFailure;
					int n = Math.min(maxRows, rows.length - next);
					for (int i = 0; i < n; i++) System.arraycopy(rows[next++], 0, buffer, i * rows[0].length, rows[0].length);
					return n;
				}
				@Override public void close() {
					if (closed) throw new AssertionError("plan cursor closed twice");
					closed = true; active--; closes++;
					if (closeFailure != null) throw closeFailure;
				}
			};
		}
		@Override public void close() { ownerCloses++; if(ownerFailure!=null)throw ownerFailure; }
	}

	private static KernelContext context(Scan scan, Plan plan, KernelHooks hooks) {
		return new KernelContext(new NativeLmdbQuerySource.NativeAdjacency[0], new long[] { 7, 9 }, new long[] { 10 },
				new long[][] { { 10, 20 } }, new int[] { 0 }, new int[] { 2 }, hooks, scan,
				plan == null ? new KernelPlan[0] : new KernelPlan[] { plan }, 16, null, null);
	}
	private static Scan sample() { return new Scan(new long[] { 7, 4, 8, 0 }, new long[] { 9, 4, 10, 0 }); }
	private static ScanQuad quad(int column) { return new ScanQuad(0, new Operand[4], new int[] { column, -1, -1, -1 }); }

	@Test public void quadScanWitnessClosesAtEveryActivation() throws Exception {
		witnessCleanup(quad(1), false);
	}
	@Test public void termEnumerationWitnessClosesAtEveryActivation() throws Exception {
		witnessCleanup(new EnumerateTerms(0, 1), false);
	}
	@Test public void planWitnessClosesAtEveryActivation() throws Exception {
		witnessCleanup(new PlanRows(0, new int[] { 1 }), true);
	}
	private static void witnessCleanup(Node node, boolean isPlan) throws Exception {
		Kernel ir = new Kernel(2, List.of(new EnumerateDomain(0, 0), new Exists(false, List.of(node))), emit(0));
		for (boolean compiled : new boolean[] { false, true }) {
			Scan scan = sample(); Plan plan = new Plan(new long[] { 7 }, new long[] { 9 });
			JaninoKernel kernel = compiled ? KernelCompilationTestSupport.compile(ir) : interpreted(ir);
			try (AutoCloseable owner = kernel::close) {
				kernel.bind(context(scan, isPlan ? plan : null, null));
				eq(rows(new long[] { 10 }, new long[] { 20 }), drain(kernel, ir, 1));
				eq(2, isPlan ? plan.opens : scan.opens);
				// Check before kernel.close: correlated re-open must not leak the previous activation.
				eq(0, isPlan ? plan.active : scan.active);
				eq(2, isPlan ? plan.closes : scan.closes);
			}
		}
	}
	@Test public void rejectedQuadWitnessCloses() throws Exception {
		Kernel ir = new Kernel(2, List.of(new EnumerateDomain(0, 0), new Exists(true,
				List.of(quad(1), new FilterCompareId(true, Operand.col(1), Operand.constant(0))))), emit(0));
		// Both 7 and 9 exist, so the !=7 witness is true: NOT EXISTS rejects both parents.
		for (boolean compiled : new boolean[] { false, true }) {
			Scan scan = sample();
			JaninoKernel kernel = compiled ? KernelCompilationTestSupport.compile(ir) : interpreted(ir);
			try (AutoCloseable owner = kernel::close) {
				kernel.bind(context(scan, null, null)); eq(List.of(), drain(kernel, ir, 3)); eq(0, scan.active);
			}
		}
	}
	@Test public void quadScanFailureClosesWithoutWaitingForOwner() throws Exception { scanFailure(quad(0)); }
	@Test public void termEnumerationFailureClosesWithoutWaitingForOwner() throws Exception { scanFailure(new EnumerateTerms(0, 0)); }
	private static void scanFailure(Node producer) throws Exception {
		Kernel ir = new Kernel(2, List.of(producer, new FilterValue(0, new Operand[] { Operand.col(0) })),
				new Emit(new int[] { 0 }, true, OutputMods.none())); // nonresumable, nonvector path
		for (boolean compiled : new boolean[] { false, true }) {
			Scan scan = sample(); RuntimeException failure = boom("predicate failure");
			KernelHooks hooks = new Hooks() {
				@Override public boolean testFilter(int id, long a, long b, long c) { throw failure; }
			};
			JaninoKernel kernel = compiled ? KernelCompilationTestSupport.compile(ir) : interpreted(ir);
			try (AutoCloseable owner = kernel::close) {
				kernel.bind(context(scan, null, hooks));
				try { kernel.fill(new long[1], 1); throw new AssertionError("exception lost"); }
				catch (RuntimeException e) { if (e != failure) throw new AssertionError("wrong primary failure", e); }
				eq(0, scan.active); eq(1, scan.closes);
			}
		}
	}
	@Test public void streamingQuadScanClosesWhenConsumerStopsEarly() throws Exception {
		Kernel ir = shape(quad(0));
		if (!ir.resumable) throw new AssertionError("test must exercise the resumable scan");
		for (boolean compiled : new boolean[] { false, true }) {
			Scan scan = sample(); JaninoKernel kernel = compiled ? KernelCompilationTestSupport.compile(ir) : interpreted(ir);
			kernel.bind(context(scan, null, null)); eq(1, kernel.fill(new long[1], 1)); kernel.close(); eq(0, scan.active);
			kernel.close(); eq(1, scan.closes);
		}
	}
	@Test public void duplicateOnlyTermScanRespondsToCancellation() throws Exception {
		Kernel ir = shape(new EnumerateTerms(0, 0));
		for (boolean compiled : new boolean[] { false, true }) {
			Scan scan = new Scan(new long[] { 7, 4, 7, 0 }); scan.infinite = true;
			KernelCancellation token = new KernelCancellation(System.nanoTime() + 60_000_000_000L);
			scan.cancelOnFill = token; KernelContext ctx = context(scan, null, null); ctx.cancellation = token;
			JaninoKernel kernel = compiled ? KernelCompilationTestSupport.compile(ir) : interpreted(ir);
			try (AutoCloseable owner = kernel::close) {
				kernel.bind(ctx);
				try { kernel.fill(new long[1], 1); throw new AssertionError("cancellation lost"); }
				catch (KernelCancelledException expected) { }
				eq(0, scan.active);
			}
		}
	}
	@Test public void termEnumerationAndQuadScanProduceIdenticalBagsAtEveryFillSize() throws Exception {
		for (Node producer : List.of(quad(0), new EnumerateTerms(0, 0))) {
			Kernel ir = shape(producer);
			List<String> expected = producer instanceof ScanQuad ? rows(new long[] { 7 }, new long[] { 9 })
					: rows(new long[] { 7 }, new long[] { 8 }, new long[] { 9 }, new long[] { 10 });
			for (int batch : new int[] { 1, 2, 3, 64, 257 }) for (boolean compiled : new boolean[] { false, true }) {
				Scan scan = sample();
				JaninoKernel kernel = compiled ? KernelCompilationTestSupport.compile(ir) : interpreted(ir);
			try (AutoCloseable owner = kernel::close) {
					kernel.bind(context(scan, null, null)); eq(expected, drain(kernel, ir, batch)); eq(0, scan.active);
				}
			}
		}
	}

    private static List<Node> hashBranch(int table) {
        return List.of(new HashBuild(table, new int[]{2}, new int[]{3},
                List.of(new EnumerateDomain(0,2), new BindAlias(Operand.constant(0),3)), 100),
                new EnumerateDomain(0,0), new HashProbe(table,new Operand[]{Operand.col(0)},new int[]{1}));
    }
    private static void evaluateBoth(Kernel ir, List<String> expected) throws Exception {
        for(boolean compiled:new boolean[]{false,true}) {
            JaninoKernel kernel=compiled?KernelCompilationTestSupport.compile(ir):interpreted(ir);
            try(AutoCloseable owner=kernel::close) {
                kernel.bind(context(sample(),null,null)); eq(expected,drain(kernel,ir,1));
            }
        }
    }
    @Test public void hashBuildInsideUnionHasCompiledStorage() throws Exception {
        List<Node> a=hashBranch(0), b=hashBranch(1);
        Kernel ir=new Kernel(4,List.of(new Union(List.of(a,b))),emit(0,1));
        evaluateBoth(ir,rows(new long[]{10,7},new long[]{20,7},new long[]{10,7},new long[]{20,7}));
    }
    @Test public void hashBuildInsideExistsHasCompiledStorage() throws Exception {
        Kernel ir=new Kernel(4,List.of(new Exists(false,hashBranch(0)),new BindAlias(Operand.entry(0),0)),emit(0));
        evaluateBoth(ir,rows(new long[]{10}));
    }
    @Test public void hashBuildInsideOptionalGroupHasCompiledStorage() throws Exception {
        Kernel ir=new Kernel(4,List.of(new LeftGroup(hashBranch(0))),emit(0,1));
        evaluateBoth(ir,rows(new long[]{10,7},new long[]{20,7}));
    }
    @Test public void hashBuildInsideLexicalFrameHasCompiledStorage() throws Exception {
        Kernel ir=new Kernel(5,List.of(new BindAlias(Operand.entry(0),4),
                new LexicalFrameLeftJoin(hashBranch(0),List.of(new BindAlias(Operand.constant(0),1)),new int[]{4})),emit(0,1,4));
        evaluateBoth(ir,rows(new long[]{10,7,10},new long[]{20,7,10}));
    }
    @Test public void hashBuildInsideHashBuildHasCompiledStorage() throws Exception {
        Kernel ir=new Kernel(5,List.of(new HashBuild(1,new int[]{0},new int[]{1},hashBranch(0),100),
                new EnumerateDomain(0,4),new HashProbe(1,new Operand[]{Operand.col(4)},new int[]{1})),emit(4,1));
        evaluateBoth(ir,rows(new long[]{10,7},new long[]{20,7}));
    }

    @Test public void primaryFailureSurvivesAllCursorAndOwnerCloseFailures() throws Exception {
        Kernel ir=new Kernel(2,List.of(new PlanRows(0,new int[]{0}),quad(1),new FilterValue(0,new Operand[]{Operand.col(1)})),
            new Emit(new int[]{0},false,new OutputMods(new int[]{0},null,false,-1,0)));
        for(boolean compiled:new boolean[]{false,true}){
            Scan scan=sample();Plan plan=new Plan(new long[]{7});
            RuntimeException primary=boom("evaluation"),scanClose=boom("scan close"),planClose=boom("cursor close"),ownerClose=boom("owner close");
            scan.closeFailure=scanClose;plan.closeFailure=planClose;plan.ownerFailure=ownerClose;
            Hooks hooks=new Hooks(){@Override public boolean testFilter(int id,long a,long b,long c){throw primary;}};
            JaninoKernel k=compiled?KernelCompilationTestSupport.compile(ir):interpreted(ir);k.bind(context(scan,plan,hooks));
            try{k.fill(new long[1],1);throw new AssertionError("primary lost");}catch(RuntimeException error){if(error!=primary)throw new AssertionError("wrong primary",error);}
            eq(0,scan.active);eq(0,plan.active);eq(1,plan.ownerCloses);k.close();
            java.util.Set<Throwable> actual=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            gatherSuppressed(primary,actual);
            for(Throwable expected:List.of(scanClose,planClose,ownerClose))if(!actual.contains(expected))throw new AssertionError("close failure lost: "+expected);
        }
    }
    private static void gatherSuppressed(Throwable failure,java.util.Set<Throwable> result){
        for(Throwable child:failure.getSuppressed())if(result.add(child))gatherSuppressed(child,result);
    }
    @Test public void failedOwnerCloseDoesNotSkipOtherOwnersAndIsIdempotent() throws Exception {
        Kernel ir=new Kernel(2,List.of(new PlanRows(0,new int[]{0}),new PlanRows(1,new int[]{1})),emit(0,1));
        for(boolean compiled:new boolean[]{false,true}){
            Plan first=new Plan(new long[]{7}),second=new Plan(new long[]{9});RuntimeException failure=boom("first owner");first.ownerFailure=failure;
            KernelContext ctx=new KernelContext(null,new long[0],new long[0],null,new Hooks(),null,new KernelPlan[]{first,second},16);
            JaninoKernel k=compiled?KernelCompilationTestSupport.compile(ir):interpreted(ir);k.bind(ctx);
            try{k.close();throw new AssertionError("close failure lost");}catch(RuntimeException error){if(error!=failure)throw new AssertionError("wrong close failure",error);}
            eq(1,first.ownerCloses);eq(1,second.ownerCloses);k.close();eq(1,second.ownerCloses);
        }
    }
    @Test public void siblingRegionsCanRebuildOneHashTableWithDifferentWidths() throws Exception {
        List<Node> first=hashBranch(0);
        List<Node> second=List.of(new HashBuild(0,new int[]{2,3},new int[]{4,5},
            List.of(new EnumerateDomain(0,2),new BindAlias(Operand.constant(0),3),new BindAlias(Operand.constant(1),4),new BindAlias(Operand.constant(0),5)),100),
            new EnumerateDomain(0,0),new HashProbe(0,new Operand[]{Operand.col(0),Operand.constant(0)},new int[]{1,4}));
        Kernel ir=new Kernel(6,List.of(new Union(List.of(first,second))),emit(0,1,4));
        evaluateBoth(ir,rows(new long[]{10,7,-1},new long[]{20,7,-1},new long[]{10,9,7},new long[]{20,9,7}));
    }
}
