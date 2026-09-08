/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeAggregateCompiler.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.lmdb.RecordIterator;
import org.junit.jupiter.api.Test;

/** Real source/interner/authority contracts. Count source-boundary lookups rather than assuming every call is I/O. */
public class NativeValueResolutionContractTest {
	static final SimpleValueFactory VF = SimpleValueFactory.getInstance();
	static void equal(long a, long b) { if (a != b) throw new AssertionError(a + " != " + b); }
	static void check(boolean ok) { if (!ok) throw new AssertionError(); }
	static void fails(Class<? extends Throwable> type, Runnable r) {
		try { r.run(); } catch (Throwable t) { if (type.isInstance(t)) return; throw new AssertionError(t); }
		throw new AssertionError("expected " + type);
	}

	public static class Source implements NativeLmdbQuerySource {
		final Map<NativeValueKey, Long> ids = new ConcurrentHashMap<>();
		final AtomicLong calls = new AtomicLong();
		volatile Object scope = new Object();
		volatile Runnable duringLookup = () -> {};
		boolean canonical = true;
		void put(Value v, long id) { ids.put(NativeValueKey.of(v), id); }
		void renew() { scope = new Object(); }
		@Override public Object valueLookupScope() { return scope; }
		@Override public long idOf(Value v) { calls.incrementAndGet(); duringLookup.run(); return ids.getOrDefault(NativeValueKey.of(v), UNKNOWN); }
		@Override public Value lazyValue(long id) { for (var e : ids.entrySet()) if (e.getValue() == id) return VF.createLiteral(e.getKey().lexical()); return null; }
		@Override public Object idSpace() { return this; }
		@Override public boolean hasCanonicalIds() { return canonical; }
		@Override public RecordIterator statements(long s,long p,long o,long c) { throw new AssertionError(); }
		@Override public long count(long s,long p,long o,long c) { throw new AssertionError(); }
		@Override public boolean has(long s,long p,long o,long c) { throw new AssertionError(); }
		@Override public double estimate(long s,long p,long o,long c) { return 0; }
		@Override public boolean hasStatementsInSource() { return !ids.isEmpty(); }
	}
	static SyntheticValueSource eval(Source s) { return new SyntheticValueSource(s, PlanValueCatalog.EMPTY).forEvaluation(); }

	@Test public void repeatedAbsentComputedValuesNeedOneDictionaryMiss() {
		Source s = new Source(); SyntheticValueSource q = eval(s); long id = UNKNOWN;
		for (int i=0;i<10000;i++) {
			long next = q.internComputedValue(VF.createLiteral("long-computed-type-label-not-in-the-store"));
			if (i == 0) id=next; else equal(id,next);
		}
		equal(1,s.calls.get()); check(q.executionContext().contains(id));
		equal(UNKNOWN,q.idOf(VF.createLiteral("long-computed-type-label-not-in-the-store")));
		equal(1,s.calls.get()); // lookup-only idOf must not expose the runtime id
		q.executionContext().close();
	}

	@Test public void hitsAreSharedByComputedBindImportLookupAndTermHashNormalization() {
		Source s=new Source();Value known=VF.createIRI("urn:known-result"), absent=VF.createLiteral("UnknownType");s.put(known,123);
		SyntheticValueSource q=eval(s);
		for(int i=0;i<1000;i++) { equal(123,q.internComputedValue(known));equal(123,q.authority().idOfOrIntern(known));equal(123,q.idOf(known)); }
		equal(1,s.calls.get());long id=q.internComputedValue(absent);q.authority().termHashKey(id);equal(2,s.calls.get());
		q.executionContext().close();
	}

	@Test public void lookupScopeRenewalInvalidatesPositiveAndNegativeEntries() {
		Source s=new Source();SyntheticValueSource q=eval(s);Value a=VF.createLiteral("a"),b=VF.createLiteral("b");s.put(a,11);
		equal(11,q.internComputedValue(a));long old=q.internComputedValue(b);equal(2,s.calls.get());
		s.put(a,22);s.put(b,33);s.renew();equal(22,q.internComputedValue(a));equal(33,q.internComputedValue(b));
		equal(4,s.calls.get());check(q.executionContext().contains(old)); // existing runtime spelling remains resolvable
		q.executionContext().close();
	}

	@Test public void planConstantsRevalidateStoreFirstInEveryNewReadViewAndEvaluation() {
		Source s=new Source();Value v=VF.createLiteral("constant");PlanValueCatalog.Builder b=new PlanValueCatalog.Builder();long pid=b.internConstant(v);
		SyntheticValueSource carrier=new SyntheticValueSource(s,b.build());SyntheticValueSource q=carrier.forEvaluation();
		equal(pid,q.internComputedValue(v));equal(pid,q.internComputedValue(v));equal(1,s.calls.get());
		s.put(v,987);s.renew();equal(987,q.internComputedValue(v));q.executionContext().close();
		SyntheticValueSource next=carrier.forEvaluation();equal(987,next.internComputedValue(v));equal(3,s.calls.get());next.executionContext().close();
	}

	@Test public void noViewProofOrWriteViewKeepsFreshLookups() {
		Source s=new Source();s.scope=null;SyntheticValueSource q=eval(s);Value v=VF.createLiteral("later");
		q.internComputedValue(v);q.internComputedValue(v);equal(2,s.calls.get());s.put(v,19);equal(19,q.internComputedValue(v));
		equal(0,q.authority().valueResolver().retainedEntries());q.executionContext().close();
	}

	@Test public void revisionChangingDuringLookupDoesNotPublishItsResult() {
		Source s=new Source();Value v=VF.createLiteral("racing");SyntheticValueSource q=eval(s);
		s.duringLookup=()->{s.renew();s.duringLookup=()->{};};q.idOf(v);equal(0,q.authority().valueResolver().retainedEntries());
		q.idOf(v);q.idOf(v);equal(2,s.calls.get());q.executionContext().close();
	}

	@Test public void exceptionsAreNotRememberedAsNegativeResults() {
		Source s=new Source();SyntheticValueSource q=eval(s);Value v=VF.createLiteral("retry");
		s.duringLookup=()->{throw new IllegalStateException("I/O fixture");};fails(IllegalStateException.class,()->q.internComputedValue(v));
		equal(0,q.authority().valueResolver().retainedEntries());s.duringLookup=()->{};s.put(v,42);equal(42,q.internComputedValue(v));equal(42,q.internComputedValue(v));equal(2,s.calls.get());q.executionContext().close();
	}

	@Test public void languageSpellingAndNumericLexicalIdentityAreNotMerged() {
		Source s=new Source();s.canonical=false;SyntheticValueSource q=eval(s);
		Value[] vs={VF.createLiteral("same","en"),VF.createLiteral("same","EN"),
				VF.createLiteral("01",VF.createIRI("http://www.w3.org/2001/XMLSchema#integer")),
				VF.createLiteral("1",VF.createIRI("http://www.w3.org/2001/XMLSchema#integer")),VF.createLiteral("1")};
		Set<Long> seen=new HashSet<>();for(Value v:vs)seen.add(q.internComputedValue(v));equal(vs.length,seen.size());
		for(int i=0;i<100;i++)for(Value v:vs)q.internComputedValue(v);equal(vs.length,s.calls.get());q.executionContext().close();
	}

	@Test public void cacheCollisionsAndEvictionNeverChangeIds() {
		Source s=new Source();SyntheticValueSource q=eval(s);List<Value> values=new ArrayList<>();int set=-1;
		for(int i=0;values.size()<7;i++){Value v=VF.createLiteral("collision-"+i);int k=NativeValueResolver.setIndex(NativeValueKey.of(v));if(set<0)set=k;if(k==set){values.add(v);s.put(v,100+values.size());}}
		for(int round=0;round<50;round++)for(int i=0;i<values.size();i++)equal(101+i,q.internComputedValue(values.get(i)));
		check(s.calls.get()>values.size());equal(4,q.authority().valueResolver().retainedEntries());q.executionContext().close();
	}

	@Test public void retentionIsBoundedAcrossHighCardinalityLookupTraffic() {
		Source s=new Source();SyntheticValueSource q=eval(s);
		for(int i=0;i<30000;i++)q.idOf(VF.createLiteral("unretained-runtime-value-"+i));
		check(q.authority().valueResolver().retainedEntries()<=NativeValueResolver.MAX_ENTRIES);equal(0,q.executionContext().internedCount());
		NativeValueResolver resolver=q.authority().valueResolver();q.executionContext().close();equal(0,resolver.retainedEntries());fails(IllegalStateException.class,()->resolver.lookup(VF.createLiteral("x")));
		equal(UNKNOWN,q.idOf(VF.createLiteral("post-close-pure-lookup"))); // original source lookup survives without cache
	}

	@Test public void everyLongIncludingSignBitIsCacheable() {
		Source s=new Source();SyntheticValueSource q=eval(s);long[] ids={0,1,Long.MIN_VALUE,Long.MAX_VALUE,Long.MIN_VALUE+7};
		for(int i=0;i<ids.length;i++){Value v=VF.createIRI("urn:key"+i);s.put(v,ids[i]);equal(ids[i],q.idOf(v));equal(ids[i],q.idOf(v));}equal(ids.length,s.calls.get());q.executionContext().close();
	}

	@Test public void simultaneousEqualMissesLoadOnce() throws Exception {
		Source s=new Source();SyntheticValueSource q=eval(s);ExecutorService pool=Executors.newFixedThreadPool(8);CyclicBarrier gate=new CyclicBarrier(8);
		try {List<Future<Long>> out=new ArrayList<>();for(int t=0;t<8;t++)out.add(pool.submit(()->{gate.await();long id=0;for(int i=0;i<1000;i++)id=q.internComputedValue(VF.createLiteral("parallel-UnknownType"));return id;}));long first=out.get(0).get();for(var f:out)equal(first,f.get());equal(1,s.calls.get());}
		finally{pool.shutdownNow();q.executionContext().close();}
	}

	@Test public void closingDuringAMissPreventsPublication() throws Exception {
		Source s=new Source();SyntheticValueSource q=eval(s);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
		s.duringLookup=()->{entered.countDown();try{release.await();}catch(InterruptedException e){throw new RuntimeException(e);}};
		ExecutorService pool=Executors.newFixedThreadPool(2);try {
			Future<?> f=pool.submit(()->fails(IllegalStateException.class,()->q.idOf(VF.createLiteral("blocked"))));entered.await();
			Future<?> closed=pool.submit(()->q.executionContext().close());while(!q.executionContext().isClosed())Thread.onSpinWait();release.countDown();f.get();closed.get();equal(0,q.authority().valueResolver().retainedEntries());
		}finally{release.countDown();pool.shutdownNow();}
	}

	@Test public void compiledCarrierAndConcurrentEvaluationsOwnNoSharedCache() {
		Source s=new Source();SyntheticValueSource carrier=new SyntheticValueSource(s,PlanValueCatalog.EMPTY);Value v=VF.createLiteral("x");
		for(int i=0;i<3;i++)carrier.idOf(v);equal(3,s.calls.get());check(carrier.executionContext()==null);fails(IllegalStateException.class,()->carrier.internComputedValue(v));
		SyntheticValueSource a=carrier.forEvaluation(),b=carrier.forEvaluation();a.internComputedValue(v);b.internComputedValue(v);equal(5,s.calls.get());
		a.executionContext().close();b.internComputedValue(v);equal(5,s.calls.get());b.executionContext().close();
	}

	@Test public void authorityAndCatalogResolutionShareCacheWithoutSharingRuntimeNamespaces() {
		Source a=new Source();Source b=new Source();Object sameScope=new Object();a.scope=b.scope=sameScope;Value v=VF.createLiteral("same");a.put(v,11);b.put(v,22);
		NativeExecutionContext c=new NativeExecutionContext();LmdbNativeTermAuthority x=new LmdbNativeTermAuthority(a,PlanValueCatalog.EMPTY,c),y=new LmdbNativeTermAuthority(b,PlanValueCatalog.EMPTY,c);
		for(int i=0;i<100;i++){equal(11,x.idOfOrIntern(v));equal(22,y.idOfOrIntern(v));}equal(1,a.calls.get());equal(1,b.calls.get());c.close();
	}

	@Test public void authoritativeQueryScopedObjectSurvivesCaching() {
		Source s=new Source();Value v=VF.createLiteral("a-retained-now-result"),copy=VF.createLiteral("a-retained-now-result");s.put(v,31);SyntheticValueSource q=eval(s);
		q.retainQueryScopedValue(v);equal(31,q.internComputedValue(copy));check(q.lazyValue(31)==v);equal(1,s.calls.get());q.executionContext().close();
	}

	@Test public void disablingCacheIsCapturedAtEvaluationConstruction() {
		String old=System.getProperty(NativeValueResolver.PROPERTY);System.setProperty(NativeValueResolver.PROPERTY,"false");
		try{Source s=new Source();SyntheticValueSource q=eval(s);System.setProperty(NativeValueResolver.PROPERTY,"true");for(int i=0;i<20;i++)q.internComputedValue(VF.createLiteral("x"));equal(20,s.calls.get());q.executionContext().close();}
		finally{if(old==null)System.clearProperty(NativeValueResolver.PROPERTY);else System.setProperty(NativeValueResolver.PROPERTY,old);}
	}
	@Test public void rawStoredIdHashingNeverAllocatesAResolver() throws Exception {
		Source s=new Source();NativeExecutionContext c=new NativeExecutionContext();LmdbNativeTermAuthority a=new LmdbNativeTermAuthority(s,PlanValueCatalog.EMPTY,c);
		for(long id=1;id<10000;id++)equal(id,a.termHashKey(id));equal(0,s.calls.get());
		var field=LmdbNativeTermAuthority.class.getDeclaredField("values");field.setAccessible(true);check(field.get(a)==null);c.close();
	}

	@Test public void equalLookingSourcesDoNotShareResolutionState() {
		class EqualSource extends Source {public int hashCode(){return 1;}public boolean equals(Object o){return o instanceof EqualSource;}}
		Source a=new EqualSource(),b=new EqualSource();Value v=VF.createLiteral("source-dependent");a.put(v,71);b.put(v,72);
		NativeExecutionContext c=new NativeExecutionContext();NativeValueResolver x=c.valueResolver(a,PlanValueCatalog.EMPTY),y=c.valueResolver(b,PlanValueCatalog.EMPTY);
		check(x!=y);check(x==c.valueResolver(a,PlanValueCatalog.EMPTY));equal(71,x.intern(v));equal(72,y.intern(v));c.close();
	}

	@Test public void sameLexicalTextOfDifferentRdfKindsDoesNotCollide() {
		Source s=new Source();SyntheticValueSource q=eval(s);Value[] values={VF.createIRI("urn:same"),VF.createLiteral("urn:same"),VF.createBNode("urn:same")};
		Set<Long> ids=new HashSet<>();for(Value v:values)ids.add(q.internComputedValue(v));equal(3,ids.size());
		for(Value v:values)q.internComputedValue(v);equal(3,s.calls.get());q.executionContext().close();
	}

	@Test public void allocationFreeProbeAgreesWithExactStructuralKeys() {
		Random random=new Random(170170L);List<Value> values=new ArrayList<>();
		for(int i=0;i<500;i++) {String label="spelling-"+random.nextInt(31);switch(i%5) {
		case 0: values.add(VF.createIRI(label));break;
		case 1: values.add(VF.createBNode(label));break;
		case 2: values.add(VF.createLiteral(label));break;
		case 3: values.add(VF.createLiteral(label,i%2==0?"en":"EN"));break;
		default: values.add(VF.createLiteral(label,VF.createIRI("urn:datatype:"+(i%3))));
		}}
		for(Value v:values){NativeValueKey key=NativeValueKey.of(v);check(key.matches(v));equal(key.spellingHash(),NativeValueKey.spellingHash(v));}
		for(int i=0;i<20000;i++){Value a=values.get(random.nextInt(values.size())),b=values.get(random.nextInt(values.size()));
			NativeValueKey key=NativeValueKey.of(a);check(key.matches(b)==key.equals(NativeValueKey.of(b)));}
	}

	@Test public void identicalFingerprintsStillRequireExactSpelling() {
		Value a=VF.createLiteral("Aa"),b=VF.createLiteral("BB");equal(NativeValueKey.spellingHash(a),NativeValueKey.spellingHash(b));
		Source s=new Source();s.put(a,19);s.put(b,23);SyntheticValueSource q=eval(s);
		try{for(int i=0;i<1000;i++){equal(19,q.internComputedValue(a));equal(23,q.internComputedValue(b));}equal(2,s.calls.get());}
		finally{q.executionContext().close();}
	}

	@Test public void saturatedSetsRetainHitsAndPeriodicallyAdmitNewHotKeys() {
		Source s=new Source();List<Value> values=new ArrayList<>();int set=-1;
		for(int i=0;values.size()<13;i++){Value v=VF.createLiteral("churn-"+i);int k=NativeValueResolver.setIndex(NativeValueKey.of(v));
			if(set<0)set=k;if(k==set){values.add(v);s.put(v,100+values.size());}}
		SyntheticValueSource q=eval(s);try{
			for(int i=0;i<12;i++)equal(101+i,q.idOf(values.get(i)));equal(12,s.calls.get());
			for(int i=0;i<100;i++)equal(112,q.idOf(values.get(11)));equal(12,s.calls.get());
			for(int i=0;i<32;i++)equal(113,q.idOf(values.get(12)));equal(44,s.calls.get());
			for(int i=0;i<100;i++)equal(113,q.idOf(values.get(12)));equal(45,s.calls.get());
			s.put(values.get(12),999);s.renew();equal(999,q.idOf(values.get(12)));equal(999,q.idOf(values.get(12)));equal(46,s.calls.get());
		}finally{q.executionContext().close();}
	}

	@Test public void catalogsRemainIsolatedEvenWithinOneExecutionAndSource() {
		Source s=new Source();Value v=VF.createLiteral("catalog-local");PlanValueCatalog.Builder first=new PlanValueCatalog.Builder(),second=new PlanValueCatalog.Builder();
		long a=first.internConstant(v);second.internConstant(VF.createLiteral("padding"));long b=second.internConstant(v);check(a!=b);
		NativeExecutionContext c=new NativeExecutionContext();try{
			NativeValueResolver x=c.valueResolver(s,first.build()),y=c.valueResolver(s,second.build());
			for(int i=0;i<100;i++){equal(a,x.intern(v));equal(b,y.intern(v));}equal(2,s.calls.get());
		}finally{c.close();}
	}

	@Test public void lazyStoreValuesReachTheirExistingIdFastPathWithoutDecoding() {
		Value value=(Value)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[]{org.eclipse.rdf4j.sail.lmdb.model.LmdbValue.class},
				(proxy,method,args)->{throw new AssertionError("unexpected lazy value access: "+method);});
		Source s=new Source(){
			@Override public Object valueLookupScope(){throw new AssertionError("unneeded cache scope");}
			@Override public long idOf(Value v){check(v==value);calls.incrementAndGet();return 1234;}
		};
		SyntheticValueSource q=eval(s);try{equal(1234,q.internComputedValue(value));equal(1234,q.idOf(value));
			equal(1234,q.authority().idOfOrIntern(value));equal(3,s.calls.get());equal(0,q.authority().valueResolver().retainedEntries());
		}finally{q.executionContext().close();}
	}

}
