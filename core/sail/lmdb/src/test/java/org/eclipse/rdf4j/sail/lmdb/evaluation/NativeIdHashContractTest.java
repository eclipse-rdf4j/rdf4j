/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAggregateCompiler.*;
import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeKernelIr.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.lmdb.RecordIterator;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.*;
import org.junit.jupiter.api.Test;

/** Actual authority/interner/catalog, native tables, kernel runtime, generated Java and interpreter. */
public class NativeIdHashContractTest {
	static final SimpleValueFactory VF = SimpleValueFactory.getInstance();
	static void check(boolean condition) { if (!condition) throw new AssertionError(); }
	static void equal(long expected, long actual) {
		if (expected != actual) throw new AssertionError(expected + " != " + actual);
	}
	static void fails(Class<? extends Throwable> type, Runnable operation) {
		try { operation.run(); } catch (Throwable t) { if (type.isInstance(t)) return; throw new AssertionError(t); }
		throw new AssertionError("Expected " + type);
	}

	/** Boundary fixture: counters prove no value/dictionary access, not a measurement of actual LMDB I/O. */
	static class Source implements NativeLmdbQuerySource {
		final boolean canonical;
		final Map<Long, Value> values = new HashMap<>();
		final Map<Value, Long> semanticIds = new HashMap<>();
		final Map<NativeValueKey, Long> spellingIds = new HashMap<>();
		long reads, lookups;
		boolean forbidReads, forbidLookups;
		Source(boolean canonical) { this.canonical = canonical; }
		void put(long id, Value value) {
			values.put(id, value); semanticIds.putIfAbsent(value, id); spellingIds.put(NativeValueKey.of(value), id);
		}
		@Override public long idOf(Value value) {
			lookups++; if (forbidLookups) throw new AssertionError("dictionary lookup during raw-id operation");
			return (canonical ? semanticIds.get(value) : spellingIds.get(NativeValueKey.of(value))) == null ? UNKNOWN
					: (canonical ? semanticIds.get(value) : spellingIds.get(NativeValueKey.of(value)));
		}
		@Override public Value lazyValue(long id) {
			reads++; if (forbidReads) throw new AssertionError("Value materialization during raw-id operation");
			return values.get(id);
		}
		@Override public Object idSpace() { return this; }
		@Override public boolean hasCanonicalIds() { return canonical; }
		@Override public RecordIterator statements(long s, long p, long o, long c) { throw new AssertionError(); }
		@Override public long count(long s, long p, long o, long c) { throw new AssertionError(); }
		@Override public boolean has(long s, long p, long o, long c) { throw new AssertionError(); }
		@Override public double estimate(long s, long p, long o, long c) { return 0; }
		@Override public boolean hasStatementsInSource() { return !values.isEmpty(); }
	}

	/** Delegations mirror production LmdbNativeKernelHooks; unrelated value hooks are deliberately unusable. */
	static class Hooks implements KernelHooks {
		final NativeTermAuthority authority;
		long valueHashes;
		Hooks(NativeTermAuthority authority) { this.authority = authority; }
		@Override public long termHashKey(long id) { return authority.termHashKey(id); }
		@Override public long rdfTermHash(long id) { valueHashes++; return authority.rdfTermHash(id); }
		@Override public boolean sameRdfTerm(long a, long b) { return authority.sameRdfTerm(a, b); }
		@Override public long importRdfTerm(KernelHooks other, long id) {
			return authority.importId(((Hooks) other).authority, id);
		}
		@Override public boolean testFilter(int filter, long a, long b, long c) { throw new AssertionError(); }
		@Override public long computeBind(int bind, long a, long b) { throw new AssertionError(); }
		@Override public int compareValues(long a, long b) { throw new AssertionError("value ordering not required"); }
		@Override public boolean isNumeric(long id) { throw new AssertionError(); }
		@Override public double doubleValue(long id) { throw new AssertionError(); }
		@Override public void accumulateNumeric(int a, int g, long id) { throw new AssertionError(); }
	}

	static LmdbNativeTermAuthority authority(Source source) {
		return new LmdbNativeTermAuthority(source, PlanValueCatalog.EMPTY, new NativeExecutionContext());
	}
	static Source unreadable() {
		Source source = new Source(true); source.forbidReads = source.forbidLookups = true; return source;
	}

	@Test public void canonicalIdsNeverResolveValuesOrDictionaryRecords() {
		Source source = unreadable(); LmdbNativeTermAuthority a = authority(source);
		check(a.supportsCanonicalTermKeys());
		long[] special = {0, UNKNOWN, Long.MIN_VALUE, Long.MAX_VALUE, 1, 2, 3, 128, 256,
				Long.MIN_VALUE + 256, RUNTIME_INTERN_BASE, SYNTHETIC_VALUE_BASE};
		for (long id : special) { equal(id, a.canonicalTermKey(id)); equal(id, a.termHashKey(id)); }
		Random r = new Random(85133);
		for (int i = 0; i < 20_000; i++) {
			long id = r.nextLong(); equal(id, a.termHashKey(id)); check(!a.sameRdfTerm(id, id ^ 0x100000000L));
		}
		equal(0, source.reads); equal(0, source.lookups);
	}

	@Test public void nativeDistinctScalarBatchAndWideKeysStayIdOnly() throws Exception {
		Source source = unreadable(); LmdbNativeTermAuthority a = authority(source);
		for (int width : new int[] {1, 2, 4, 7}) {
			int[] slots = java.util.stream.IntStream.range(0, width).toArray();
			NativeDistinctTracker distinct = new NativeDistinctTracker(slots, a);
			NativeBatch batch = new NativeBatch(width, 4096);
			for (int i = 0; i < 4096; i++) {
				long[] row = new long[width];
				for (int j = 0; j < width; j++) row[j] = Long.MIN_VALUE + 256L * (1 + i / 2) + j;
				check(distinct.add(row) == ((i & 1) == 0)); batch.copyFromRow(row, i);
			}
			batch.finishRows(4096); distinct.clear(); equal(2048, distinct.selectBatch(batch, batch.selection, 4096));
			var field = NativeTermReferenceCache.class.getDeclaredField("ids"); field.setAccessible(true);
			check(field.get(distinct.termCache) == null); // no per-store-id semantic cache
		}
		equal(0, source.reads); equal(0, source.lookups);
	}

	@Test public void allKernelTablesGrowAndCompareFullIdsWithoutValues() {
		Source source = unreadable(); Hooks hooks = new Hooks(authority(source));
		KernelRuntime.LongHashSet set = new KernelRuntime.LongHashSet(2, hooks);
		KernelRuntime.LongIntMap groups = new KernelRuntime.LongIntMap(2, hooks);
		KernelRuntime.RowSet tuples = new KernelRuntime.RowSet(2, 2, hooks);
		LongHashSet nativeSet = new LongHashSet(2, hooks.authority);
		for (int i = 0; i < 4096; i++) {
			long id = i == 0 ? 0 : i == 1 ? UNKNOWN : i == 2 ? Long.MIN_VALUE : Long.MIN_VALUE + 256L * i;
			check(set.add(id)); check(!set.add(id)); check(set.contains(id));
			equal(i, groups.getOrInsert(id)); equal(i, groups.get(id)); equal(id, groups.keyAt(i));
			equal(i, tuples.internOrGet(new long[]{id, id ^ Long.MIN_VALUE}, 0));
			equal(i, tuples.internOrGet(new long[]{id, id ^ Long.MIN_VALUE}, 0));
			check(nativeSet.add(id)); check(!nativeSet.add(id)); check(nativeSet.contains(id));
		}
		equal(4096, set.size()); equal(4096, groups.size()); equal(4096, tuples.size()); equal(4096, nativeSet.size());
		equal(0, source.reads); equal(0, source.lookups); equal(0, hooks.valueHashes);
	}

	@Test public void planRuntimeAndStoredAliasesShareOneKeyButKeepTheirOriginalValues() {
		Source source = new Source(true); source.put(100, VF.createLiteral("stored", "en")); source.forbidReads = true;
		PlanValueCatalog.Builder builder = new PlanValueCatalog.Builder();
		long storedAlias = builder.internConstant(VF.createLiteral("stored", "EN"));
		long lower = builder.internConstant(VF.createLiteral("absent", "en"));
		long upper = builder.internConstant(VF.createLiteral("absent", "EN"));
		NativeExecutionContext context = new NativeExecutionContext();
		long runtime = context.internValue(VF.createLiteral("absent", "En"));
		LmdbNativeTermAuthority a = new LmdbNativeTermAuthority(source, builder.build(), context);
		equal(100, a.canonicalTermKey(storedAlias)); equal(a.canonicalTermKey(lower), a.canonicalTermKey(upper));
		equal(a.canonicalTermKey(lower), a.canonicalTermKey(runtime)); check(a.sameRdfTerm(lower, runtime));
		for (int i = 0; i < 100; i++) { a.termHashKey(storedAlias); a.termHashKey(lower); a.termHashKey(upper); a.termHashKey(runtime); }
		equal(4, source.lookups); equal(0, source.reads);
		check(context.valueOf(runtime).equals(VF.createLiteral("absent", "en")));
		check(((org.eclipse.rdf4j.model.Literal)context.valueOf(runtime)).getLanguage().orElseThrow().equals("En"));
	}

	@Test public void aliasesAddedAfterRawKeysDoNotChangeExistingHashBuckets() {
		Source source = new Source(true); source.put(123, VF.createLiteral("x", "en")); source.forbidReads = true;
		NativeExecutionContext context = new NativeExecutionContext();
		LmdbNativeTermAuthority a = new LmdbNativeTermAuthority(source, PlanValueCatalog.EMPTY, context);
		Hooks hooks = new Hooks(a); KernelRuntime.LongHashSet set = new KernelRuntime.LongHashSet(2, hooks);
		NativeDistinctTracker distinct = new NativeDistinctTracker(new int[]{0}, a);
		check(set.add(123)); check(distinct.add(new long[]{123}));
		long alias = context.internValue(VF.createLiteral("x", "EN"));
		check(!set.add(alias)); check(!distinct.add(new long[]{alias})); check(set.contains(alias));
		equal(1, source.lookups); equal(0, source.reads);
	}

	@Test public void contextRuntimeNegativeGateDoesNotConfuseRetainedOrHighBitStoreIds() {
		Source source = unreadable(); NativeExecutionContext context = new NativeExecutionContext();
		long runtime = context.internValue(VF.createLiteral("runtime"));
		context.retainQueryScopedValue(VF.createLiteral("clock"), 100);
		LmdbNativeTermAuthority a = new LmdbNativeTermAuthority(source, PlanValueCatalog.EMPTY, context);
		check(context.internedValueOf(runtime) != null); check(context.internedValueOf(runtime + 1) == null);
		check(context.internedValueOf(100) == null);
		for (long id : new long[]{100, 456, Long.MIN_VALUE, Long.MAX_VALUE, runtime - 1, runtime + 1}) equal(id, a.termHashKey(id));
		equal(0, source.reads); equal(0, source.lookups);
	}

	@Test public void lexicalNumericIdentityIsNotNumericValueEquality() {
		Source source = new Source(true); PlanValueCatalog.Builder builder = new PlanValueCatalog.Builder();
		var datatype = VF.createIRI("http://www.w3.org/2001/XMLSchema#integer");
		long one = builder.internConstant(VF.createLiteral("1", datatype));
		long padded = builder.internConstant(VF.createLiteral("01", datatype));
		LmdbNativeTermAuthority a = new LmdbNativeTermAuthority(source, builder.build(), new NativeExecutionContext());
		check(!a.sameRdfTerm(one, padded)); check(a.canonicalTermKey(one) != a.canonicalTermKey(padded));
		LongHashSet set = new LongHashSet(2, a); check(set.add(one)); check(set.add(padded)); equal(2, set.size());
	}

	@Test public void legacyDictionaryKeepsSemanticHashFallbackAndDefaultHooksStayCompatible() {
		Source source = new Source(false); source.put(101, VF.createLiteral("x", "en")); source.put(102, VF.createLiteral("x", "EN"));
		LmdbNativeTermAuthority a = authority(source); check(!a.supportsCanonicalTermKeys());
		fails(UnsupportedOperationException.class, () -> a.canonicalTermKey(101));
		equal(a.rdfTermHash(101), a.rdfTermHash(102)); check(a.sameRdfTerm(101, 102));
		Hooks hooks = new Hooks(a);
		KernelRuntime.LongHashSet set = new KernelRuntime.LongHashSet(2, hooks); check(set.add(101)); check(!set.add(102));
		NativeDistinctTracker distinct = new NativeDistinctTracker(new int[]{0}, a); check(distinct.add(new long[]{101})); check(!distinct.add(new long[]{102}));
		LongHashSet nativeSet = new LongHashSet(2, a); check(nativeSet.add(101)); check(!nativeSet.add(102));
		check(source.reads > 0);
	}

	@Test public void fullEqualitySurvivesDeliberatelyConstantHashKeys() {
		Source source = unreadable(); Hooks hooks = new Hooks(authority(source)) {
			@Override public long termHashKey(long id) { return 0; }
		};
		KernelRuntime.LongHashSet set = new KernelRuntime.LongHashSet(2, hooks);
		KernelRuntime.RowSet rows = new KernelRuntime.RowSet(2, 2, hooks);
		PrimitiveTupleTable primitive = new PrimitiveTupleTable(2, 2, false, hash -> 0);
		for (int i = 1; i <= 256; i++) {
			check(set.add(i)); check(set.add(Long.MIN_VALUE + i));
			long[] key = {i, Long.MIN_VALUE + i}; equal(i - 1, rows.internOrGet(key, 0));
			equal(i - 1, primitive.findOrInsert(key, new int[]{0, 1}, false));
		}
		equal(512, set.size()); equal(256, rows.size()); equal(0, source.reads);
	}

	@Test public void nativeSetRemoveAndIterationRetainOriginalAliasRepresentatives() {
		Source source = new Source(true); PlanValueCatalog.Builder builder = new PlanValueCatalog.Builder();
		long lower = builder.internConstant(VF.createLiteral("x", "en")), upper = builder.internConstant(VF.createLiteral("x", "EN"));
		LmdbNativeTermAuthority a = new LmdbNativeTermAuthority(source, builder.build(), new NativeExecutionContext());
		// Establish a different canonical representative before the set sees its first member.
		a.canonicalTermKey(lower);
		LongHashSet set = new LongHashSet(2, a); check(set.add(upper)); check(!set.add(lower)); equal(upper, set.values()[0]);
		for (int i = 1; i < 1024; i++) check(set.add(i));
		check(set.contains(lower)); check(set.remove(lower)); check(!set.contains(upper));
		for (int i = 1; i < 1024; i++) check(set.contains(i));
		equal(0, source.reads);
	}

	@Test public void crossAuthorityMergesImportBeforeUsingLocalKeys() {
		Source source = new Source(true);
		NativeExecutionContext leftContext = new NativeExecutionContext(), rightContext = new NativeExecutionContext();
		long leftId = leftContext.internValue(VF.createLiteral("left"));
		long rightId = rightContext.internValue(VF.createLiteral("right")); equal(leftId, rightId);
		LmdbNativeTermAuthority left = new LmdbNativeTermAuthority(source, PlanValueCatalog.EMPTY, leftContext);
		LmdbNativeTermAuthority right = new LmdbNativeTermAuthority(source, PlanValueCatalog.EMPTY, rightContext);
		KernelRuntime.LongHashSet a = new KernelRuntime.LongHashSet(2, new Hooks(left));
		KernelRuntime.LongHashSet b = new KernelRuntime.LongHashSet(2, new Hooks(right));
		check(a.add(leftId)); check(b.add(rightId)); a.addAll(b); equal(2, a.size());
		LongHashSet c = new LongHashSet(2, left), d = new LongHashSet(2, right);
		check(c.add(leftId)); check(d.add(rightId)); c.addAll(d); equal(2, c.size());
		Set<Value> actual = new HashSet<>(); for (long id : c.values()) actual.add(left.valueOf(id));
		check(actual.equals(Set.of(VF.createLiteral("left"), VF.createLiteral("right"))));
	}

	@Test public void parallelSyntheticNormalizationPublishesOneStableRepresentative() throws Exception {
		Source source = new Source(true); NativeExecutionContext context = new NativeExecutionContext();
		long lower = context.internValue(VF.createLiteral("x", "en")), upper = context.internValue(VF.createLiteral("x", "EN"));
		LmdbNativeTermAuthority a = new LmdbNativeTermAuthority(source, PlanValueCatalog.EMPTY, context);
		try (ExecutorService pool = Executors.newFixedThreadPool(6)) {
			List<Future<Long>> futures = new ArrayList<>();
			for (int i = 0; i < 12; i++) {
				long id = (i & 1) == 0 ? lower : upper;
				futures.add(pool.submit(() -> { long key = a.canonicalTermKey(id); for (int j = 0; j < 500; j++) equal(key, a.canonicalTermKey(id)); return key; }));
			}
			long key = futures.get(0).get(); for (Future<Long> future : futures) equal(key, future.get());
		}
		equal(2, source.lookups); equal(0, source.reads);
	}

	@Test public void nativeEmptyBucketAcceptsAliasesAndImportsItsOriginalRepresentative() {
		Source source = new Source(true); source.put(Long.MIN_VALUE, VF.createLiteral("sentinel", "en"));
		NativeExecutionContext context = new NativeExecutionContext();
		long alias = context.internValue(VF.createLiteral("sentinel", "EN"));
		LmdbNativeTermAuthority a = new LmdbNativeTermAuthority(source, PlanValueCatalog.EMPTY, context);
		LongHashSet set = new LongHashSet(1, a);
		check(set.add(alias)); check(!set.add(Long.MIN_VALUE)); check(set.contains(Long.MIN_VALUE));
		equal(alias, set.values()[0]);
		for (int i = 1; i < 256; i++) check(set.add(i));
		check(set.contains(alias)); check(set.remove(Long.MIN_VALUE)); check(!set.contains(alias));
		check(set.add(Long.MIN_VALUE)); check(set.contains(alias)); check(set.remove(alias));
		check(set.add(alias));
		Source targetSource = new Source(true); targetSource.put(700L, VF.createLiteral("sentinel", "en"));
		LmdbNativeTermAuthority targetAuthority = authority(targetSource);
		LongHashSet target = new LongHashSet(2, targetAuthority);
		LongHashSet one = new LongHashSet(2, a); one.add(alias); target.addAll(one);
		check(target.contains(700L)); equal(700L, target.values()[0]);
	}

	static KernelPlan rows(long[][] rows) {
		return new KernelPlan() {
			@Override public Cursor open() { return new Cursor() {
				int at;
				@Override public int fill(long[] out, int maxRows) {
					int count = Math.min(maxRows, rows.length - at);
					for (int i = 0; i < count; i++) System.arraycopy(rows[at++], 0, out, i * rows[0].length, rows[0].length);
					return count;
				}
				@Override public void close() {}
			}; }
			@Override public void close() {}
		};
	}

	static List<long[]> execute(JaninoKernel implementation, Kernel shape, long[][] input, Hooks hooks) {
		implementation.bind(new KernelContext(null, new long[0], new long[0], null, hooks, null, new KernelPlan[]{rows(input)}, 2));
		List<long[]> result = new ArrayList<>(); long[] buffer = new long[3 * shape.stride()]; int n;
		try { while ((n = implementation.fill(buffer, 3)) != 0) for (int i = 0; i < n; i++) result.add(Arrays.copyOfRange(buffer, i * shape.stride(), (i + 1) * shape.stride())); }
		finally { implementation.close(); }
		return result;
	}

	@Test public void generatedAndInterpretedGroupByAndCountDistinctMakeZeroValueReads() throws Exception {
		Source source = unreadable(); Hooks hooks = new Hooks(authority(source));
		long[][] input = new long[2000][2];
		for (int i = 0; i < input.length; i++) { input[i][0] = Long.MIN_VALUE + (i % 13); input[i][1] = i % 71; }
		for (int[] groupColumns : new int[][]{{0}, {0, 1}, {}}) {
			Kernel shape = new Kernel(2, List.of(new PlanRows(0, new int[]{0,1})),
					new Aggregate(groupColumns, new AggregateOutput[]{AggregateOutput.countStar(), AggregateOutput.countDistinct(1)}, null, OutputMods.none()));
			Path dir = Files.createTempDirectory("native-id-hash-ir-");
			try {
				JaninoKernel generated = GeneratedBorrowedFtreeContractTest.compile(shape.className(), LmdbNativeKernelEmitter.emit(shape), dir);
				Map<List<Long>, long[]> expected = new HashMap<>(); Map<List<Long>, Set<Long>> distinct = new HashMap<>();
				for (long[] row : input) {
					List<Long> key = new ArrayList<>(); for (int c : groupColumns) key.add(row[c]);
					expected.computeIfAbsent(key, x -> new long[2])[0]++;
					if (row[1] != UNKNOWN) distinct.computeIfAbsent(key, x -> new HashSet<>()).add(row[1]);
				}
				for (JaninoKernel implementation : new JaninoKernel[]{generated, LmdbNativeKernelInterpreter.forAggregate(shape)}) {
					check(implementation != null); List<long[]> results = execute(implementation, shape, input, hooks); equal(expected.size(), results.size());
					for (long[] result : results) {
						List<Long> key = new ArrayList<>(); for (int i = 0; i < groupColumns.length; i++) key.add(result[i]);
						equal(expected.get(key)[0], result[groupColumns.length]); equal(distinct.get(key).size(), result[groupColumns.length + 1]);
					}
				}
			} finally { GeneratedBorrowedFtreeContractTest.remove(dir); }
		}
		equal(0, source.reads); equal(0, source.lookups); equal(0, hooks.valueHashes);
	}
	@Test public void generatedAndInterpretedAliasesShareGroupsWithoutChangingOutputs() throws Exception {
		Source source = new Source(true); source.put(41, VF.createLiteral("x", "en"));
		NativeExecutionContext context = new NativeExecutionContext();
		long alias = context.internValue(VF.createLiteral("x", "EN"));
		LmdbNativeTermAuthority a = new LmdbNativeTermAuthority(source, PlanValueCatalog.EMPTY, context);
		Hooks hooks = new Hooks(a); long[][] input = {{alias, alias}, {41, 41}, {alias, 41}};
		for (int[] groups : new int[][] {{0}, {0, 1}}) {
			Kernel shape = new Kernel(2, List.of(new PlanRows(0, new int[]{0,1})),
					new Aggregate(groups, new AggregateOutput[]{AggregateOutput.countStar(), AggregateOutput.countDistinct(1)}, null, OutputMods.none()));
			Path dir = Files.createTempDirectory("native-id-hash-alias-ir-");
			try {
				JaninoKernel generated = GeneratedBorrowedFtreeContractTest.compile(shape.className(), LmdbNativeKernelEmitter.emit(shape), dir);
				for (JaninoKernel kernel : new JaninoKernel[] {generated, LmdbNativeKernelInterpreter.forAggregate(shape)}) {
					List<long[]> result = execute(kernel, shape, input, hooks); equal(1, result.size());
					long[] row = result.get(0); for (int i=0;i<groups.length;i++) equal(alias,row[i]);
					equal(3, row[groups.length]); equal(1, row[groups.length+1]);
				}
			} finally { GeneratedBorrowedFtreeContractTest.remove(dir); }
		}
		equal(0, source.reads); equal(1, source.lookups); equal(0, hooks.valueHashes);
	}

	@Test public void publicValueHashAndDefaultSemanticHookRemainUnchanged() {
		Source source = new Source(true); Value value = VF.createLiteral("not-an-id", "EN"); source.put(900, value);
		LmdbNativeTermAuthority a = authority(source);
		equal(900, a.termHashKey(900)); equal(0, source.reads);
		equal(value.hashCode(), a.rdfTermHash(900)); check(source.reads > 0);
		KernelHooks defaultHook = new KernelHooks() {
			@Override public long rdfTermHash(long id) { return id ^ 99; }
			@Override public boolean testFilter(int f,long a,long b,long c) { throw new AssertionError(); }
			@Override public long computeBind(int f,long a,long b) { throw new AssertionError(); }
			@Override public int compareValues(long a,long b) { throw new AssertionError(); }
			@Override public boolean isNumeric(long id) { throw new AssertionError(); }
			@Override public double doubleValue(long id) { throw new AssertionError(); }
			@Override public void accumulateNumeric(int a,int g,long id) { throw new AssertionError(); }
		};
		equal(33 ^ 99, defaultHook.termHashKey(33));
	}

}
