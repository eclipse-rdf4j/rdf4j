/* Copyright (c) 2026 Eclipse RDF4J contributors. SPDX-License-Identifier: BSD-3-Clause */
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;

/** Exhaustive finite domains plus concurrent linearization tests. These are not all-JVM model checking. */
class LmdbNativeArbiterInvariantTest {
    private static final int[][] PERMUTATIONS={{0,1,2},{0,2,1},{1,0,2},{1,2,0},{2,0,1},{2,1,0}};
    static LmdbNativePhysicalVariantKey key(String name){return LmdbNativeArbiterConsistencyTest.key(name);}
    static LmdbNativeAdaptiveArbitration.Candidate<String> arm(String name,int rank){
        return LmdbNativeArbiterConsistencyTest.candidate(name,rank);
    }
    private static LmdbNativeCostPrediction quote(String name,int code){
        if(code==0)return LmdbNativeCostPrediction.ordinalOnly("ordinal");
        double mean=switch(code%4){case 0->1;case 1->10;case 2->100;default->1000;};
        long latest=code>=7 ? (long)(1000/mean) : 0;
        boolean allowed=code!=5, quarantined=code==6;
        var c=new LmdbNativeCostPrediction.Components(LmdbNativeRegimeKey.STEADY,
            code%2==0?LmdbNativeCostPosteriorStore.Lane.DIRECT:LmdbNativeCostPosteriorStore.Lane.RESIDUAL,
            LmdbNativeFamilyShapeKey.of(key(code%3==0?"shared":name)),0,StrictMath.log(mean),
            code%3==0?.5:0,code%2==0?.2:0,code%4==0?.1:0,0);
        return new LmdbNativeCostPrediction(0,mean,Double.MAX_VALUE,0,Double.MAX_VALUE,0,Double.MAX_VALUE,
            LmdbNativeCostPrediction.PriceBasis.DIRECT_POSTERIOR,allowed,quarantined,10,10,latest,
            LmdbNativeCostPrediction.EvidenceSource.EXACT_VARIANT,c,"");
    }
    private static double location(LmdbNativeCostPrediction p){
        return p.latestObservedNanos()>0?StrictMath.log((double)p.latestObservedNanos()):
            p.components().logBase()+p.components().meanLog();
    }
    private static boolean edge(LmdbNativeAdaptiveArbitration.Priced<String> c,
            LmdbNativeAdaptiveArbitration.Priced<String> i,double normal,double losing){
        var ck=c.candidate().estimate().variantKey();var ik=i.candidate().estimate().variantKey();
        double margin=ck.strategyFamily().equals(ik.strategyFamily()) && ik.propertyPreserving() && !ck.propertyPreserving()?losing:normal;
        return LmdbNativeCostPrediction.displaces(c.prediction(),i.prediction(),margin);
    }
    private static int priority(LmdbNativeAdaptiveArbitration.Priced<String> a,
            LmdbNativeAdaptiveArbitration.Priced<String> b){
        int p=Integer.compare(a.candidate().staticPreference(),b.candidate().staticPreference());
        return p!=0?p:a.candidate().estimate().variantKey().compareTo(b.candidate().estimate().variantKey());
    }
    private static LmdbNativeAdaptiveArbitration.Priced<String> oracle(
            List<LmdbNativeAdaptiveArbitration.Priced<String>> arms,double normal,double losing){
        // Declarative reference: materialize dominance matrix, remove all targets, min by explicit total priority.
        boolean[] dominated=new boolean[arms.size()];
        for(int c=0;c<arms.size();c++)for(int i=0;i<arms.size();i++)if(c!=i && edge(arms.get(c),arms.get(i),normal,losing)){
            assertTrue(location(arms.get(c).prediction())<location(arms.get(i).prediction()),"edge must lower potential");
            dominated[i]=true;
        }
        LmdbNativeAdaptiveArbitration.Priced<String> best=null;
        for(int i=0;i<arms.size();i++)if(!dominated[i] && (best==null||priority(arms.get(i),best)<0))best=arms.get(i);
        assertNotNull(best,"acyclic finite relation has a frontier");return best;
    }
    @Test void exhaustiveThreeArmDomainHasUniqueOrderIndependentFrontier(){
        long decisions=0;
        for(int a=0;a<12;a++)for(int b=0;b<12;b++)for(int c=0;c<12;c++)for(int rankMode=0;rankMode<2;rankMode++){
            var x=new LmdbNativeAdaptiveArbitration.Priced<>(arm("a",rankMode==0?0:2),quote("a",a));
            var y=new LmdbNativeAdaptiveArbitration.Priced<>(arm("b",rankMode==0?0:1),quote("b",b));
            var z=new LmdbNativeAdaptiveArbitration.Priced<>(arm("c",rankMode==0?0:0),quote("c",c));
            var base=List.of(x,y,z);
            for(double margin:new double[]{0,.1,1}){
                var expected=oracle(base,margin,margin+0.5);
                for(int[] p:PERMUTATIONS){
                    var perm=List.of(base.get(p[0]),base.get(p[1]),base.get(p[2]));
                    assertSame(expected,LmdbNativeAdaptiveArbitration.selectWithinFrontier(perm,margin,margin+0.5));
                    decisions++;
                }
            }
        }
        assertEquals(62208L,decisions);
        System.out.println("EXHAUSTIVE frontier decisions="+decisions+"; quote triples=1728; permutations=6");
    }
    @Test void frozenModelReplayDoesNotMutatePricingAndIsPermutationInvariant(){
        AtomicLong clock=new AtomicLong(10000);
        var store=new LmdbNativeStoreCostModel(null,LmdbNativePosteriorConfig.defaults(),clock::get);
        var model=new LmdbNativeAdaptiveCostModel(new LmdbNativeMachineCostModel(),store,
            new LmdbNativeAdaptiveCostModel.Configuration(true,true));
        var arms=List.of(arm("a",0),arm("b",1),arm("c",2));
        Random random=new Random(761943);
        for(int i=0;i<200;i++){
            var item=arms.get(random.nextInt(arms.size()));
            model.recordCompleted(item.estimate(),item.estimate().total(),1000+random.nextInt(100000),1,model.currentRegime());
        }
        var before=model.predictAll(arms.stream().map(v->v.estimate()).toList());
        String winner=null;
        for(int iteration=0;iteration<100;iteration++)for(int[] p:PERMUTATIONS){
            var chosen=LmdbNativeAdaptiveArbitration.choose(List.of(arms.get(p[0]),arms.get(p[1]),arms.get(p[2])),
                model,LmdbNativeAdaptiveArbitration.ProbeContext.disabled());
            var normal=(LmdbNativeAdaptiveArbitration.DispatchPlan.Normal<String>)chosen;
            String actual=normal.candidate().estimate().variantKey().strategyFamily();
            if(winner==null)winner=actual;else assertEquals(winner,actual);
        }
        assertEquals(before,model.predictAll(arms.stream().map(v->v.estimate()).toList()));
    }
    @Test void identityTokenRejectsSameEpochABAAndDuplicatedCallbacks(){
        var s=new LmdbNativeProbeScheduler(LmdbNativeProbeConfig.defaults(),()->10000L);
        var k=key("arm");var r=LmdbNativeRegimeKey.STEADY;
        var old=s.tryBeginProbe(k,r,0);assertNotNull(old);s.completed(old,false);
        var next=s.tryBeginProbe(k,r,0);assertNotNull(next);
        s.completed(old,false);s.censored(old,true);s.capacityExceeded(old);s.probeAbandoned(old);s.quarantine(old);
        assertEquals(LmdbNativeProbeScheduler.State.PROBING,s.state(k,r,0));
        s.probeAbandoned(next);assertTrue(s.mayProbe(k,r,0));
    }
    @Test void concurrentFlightAdmissionHasExactlyOneOwner() throws Exception {
        for(int round=0;round<100;round++){
            var s=new LmdbNativeProbeScheduler(LmdbNativeProbeConfig.defaults(),()->10000L);var k=key("same");
            var owners=Collections.synchronizedList(new ArrayList<LmdbNativeProbeScheduler.Flight>());
            parallel(8,()->{var f=s.tryBeginProbe(k,LmdbNativeRegimeKey.STEADY,0);if(f!=null)owners.add(f);});
            assertEquals(1,owners.size());s.probeAbandoned(owners.get(0));
        }
    }
    @Test void concurrentDifferentArmsCannotBypassStoreProbeSpacing() throws Exception {
        var config=LmdbNativeProbeConfig.defaults();
        var l=new LmdbNativeSafetyLedger(config,()->1000000000L);l.earn(1_000_000_000_000L);
        var admitted=Collections.synchronizedList(new ArrayList<LmdbNativeSafetyLedger.Reservation>());
        parallel(16,()->{var r=l.tryReserveProbe(100000,false);if(r!=null)admitted.add(r);});
        assertEquals(1,admitted.size());admitted.get(0).refund();
        assertFalse(l.spacingAllows(),"refund does not mean a new normal decision occurred");
    }
    @Test void concurrentDistinctAdmissionsNeverExceedSchedulerBound() throws Exception {
        var s=new LmdbNativeProbeScheduler(LmdbNativeProbeConfig.defaults(),()->10000L);
        AtomicInteger seq=new AtomicInteger(),accepted=new AtomicInteger();
        parallel(8,()->{for(int i=0;i<700;i++)if(s.tryBeginProbe(key("arm"+seq.getAndIncrement()),
            LmdbNativeRegimeKey.STEADY,0)!=null)accepted.incrementAndGet();});
        assertEquals(4096,accepted.get());assertEquals(4096,s.trackedArms());
    }
    @Test void quotesShareExactlyOneClockAndLifecycleCapture(){
        AtomicInteger wallReads=new AtomicInteger();
        var store=new LmdbNativeStoreCostModel(null,LmdbNativePosteriorConfig.defaults(),()->1000+wallReads.incrementAndGet());
        var model=new LmdbNativeAdaptiveCostModel(new LmdbNativeMachineCostModel(),store,
            new LmdbNativeAdaptiveCostModel.Configuration(true,true));
        var estimates=List.of(arm("a",0).estimate(),arm("b",1).estimate(),arm("c",2).estimate());
        int before=wallReads.get();var batch=model.predictAll(estimates);
        assertEquals(1,wallReads.get()-before);assertEquals(3,batch.predictions().size());
        try{batch.predictions().clear();throw new AssertionError("mutable quote list");}
        catch(UnsupportedOperationException expected){}
    }
    @Test void malformedNumericQuoteAndMarginsFailExplicitly(){
        for(double margin:new double[]{-1,Double.NaN,Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY}){
            try{LmdbNativeCostPrediction.displaces(quote("a",1),quote("b",2),margin);
                throw new AssertionError("accepted bad margin");}catch(IllegalArgumentException expected){}
        }
        try{new LmdbNativeCostPrediction.Components(LmdbNativeRegimeKey.STEADY,
            LmdbNativeCostPosteriorStore.Lane.DIRECT,LmdbNativeFamilyShapeKey.of(key("bad")),0,0,0,0,Double.NaN,0);
            throw new AssertionError("accepted NaN");}catch(IllegalArgumentException expected){}
    }
    @Test void duplicatePhysicalIdentitiesFailInsteadOfDependingOnOfferOrder(){
        var store=new LmdbNativeStoreCostModel(null,LmdbNativePosteriorConfig.defaults(),()->10000L);
        var model=new LmdbNativeAdaptiveCostModel(new LmdbNativeMachineCostModel(),store,
            new LmdbNativeAdaptiveCostModel.Configuration(true,true));
        try{LmdbNativeAdaptiveArbitration.choose(List.of(arm("same",0),arm("same",1)),model,
            LmdbNativeAdaptiveArbitration.ProbeContext.disabled());throw new AssertionError("ambiguous duplicate accepted");}
        catch(IllegalArgumentException expected){}
    }
    @Test void heldFlightDrainsBeforeNewEpochBeginsAndCannotClearNewQuarantine(){
        var s=new LmdbNativeProbeScheduler(LmdbNativeProbeConfig.defaults(),()->10000L);
        var k=key("arm");var r=LmdbNativeRegimeKey.STEADY;var old=s.tryBeginProbe(k,r,0);
        assertNull(s.tryBeginProbe(k,r,1));s.quarantine(k,r,2);s.completed(old,false);
        assertEquals(LmdbNativeProbeScheduler.State.QUARANTINED,s.state(k,r,2));
    }
    @Test void exhaustiveSchedulerTracesMatchIndependentReference(){
        int length=5,actions=8,total=1;for(int i=0;i<length;i++)total*=actions;
        for(int encoded=0;encoded<total;encoded++){
            int bits=encoded;AtomicLong clock=new AtomicLong(10000);long epoch=0;
            var config=LmdbNativeProbeConfig.defaults();var s=new LmdbNativeProbeScheduler(config,clock::get);
            var r=LmdbNativeRegimeKey.STEADY;var k=key("one");var ref=new Reference(config.cooldownBaseMillis());
            var flights=new ArrayList<LmdbNativeProbeScheduler.Flight>();var ids=new ArrayList<Integer>();
            for(int depth=0;depth<length;depth++){
                int action=bits%actions;bits/=actions;
                switch(action){
                    case 0->{var f=s.tryBeginProbe(k,r,epoch);int id=ref.begin(epoch,clock.get());
                        assertEquals(id!=0,f!=null);if(f!=null){flights.add(f);ids.add(id);}}
                    case 1,2,3,4->{if(!flights.isEmpty()){
                        int index=action==4?0:flights.size()-1;var f=flights.get(index);int id=ids.get(index);
                        if(action==1){s.completed(f,false);ref.finish(id,0,clock.get());}
                        else if(action==2){s.censored(f,false);ref.finish(id,1,clock.get());}
                        else if(action==3){s.capacityExceeded(f);ref.finish(id,2,clock.get());}
                        else{s.probeAbandoned(f);ref.finish(id,3,clock.get());}}}
                    case 5->{epoch++;}
                    case 6->{clock.addAndGet(config.cooldownBaseMillis()*12);}
                    case 7->{s.quarantine(k,r,epoch);ref.quarantine(epoch,clock.get());}
                }
                assertEquals(ref.state(epoch,clock.get()),s.state(k,r,epoch),"trace="+encoded+" step="+depth);
            }
        }
        System.out.println("EXHAUSTIVE scheduler traces="+total+"; length="+length+"; checked transitions="+(total*length));
    }
    // Independent one-key reference: policy tuple and a separate token counter, no production map/helper calls.
    private static final class Reference {
        final long base;long stateEpoch=-1,until,flightEpoch;int strikes,token,seq;
        LmdbNativeProbeScheduler.State policy=LmdbNativeProbeScheduler.State.UNKNOWN;
        Reference(long base){this.base=base;}
        LmdbNativeProbeScheduler.State state(long epoch,long now){
            if(stateEpoch>epoch)return LmdbNativeProbeScheduler.State.DORMANT_UNTIL_EPOCH;
            if(token!=0){if(stateEpoch==epoch && policy==LmdbNativeProbeScheduler.State.QUARANTINED && now<until)return policy;
                return LmdbNativeProbeScheduler.State.PROBING;}
            if(stateEpoch==-1)return LmdbNativeProbeScheduler.State.UNKNOWN;
            if(stateEpoch<epoch)return LmdbNativeProbeScheduler.State.PROBE_ELIGIBLE;
            if((policy==LmdbNativeProbeScheduler.State.COOLDOWN||policy==LmdbNativeProbeScheduler.State.QUARANTINED)&&now>=until)
                return LmdbNativeProbeScheduler.State.PROBE_ELIGIBLE;
            return policy;
        }
        int begin(long epoch,long now){
            var status=state(epoch,now);
            if(status!=LmdbNativeProbeScheduler.State.UNKNOWN && status!=LmdbNativeProbeScheduler.State.PROBE_ELIGIBLE
                && status!=LmdbNativeProbeScheduler.State.ACTIVE)return 0;
            if(stateEpoch<epoch)strikes=0;stateEpoch=epoch;flightEpoch=epoch;token=++seq;
            until=0;policy=LmdbNativeProbeScheduler.State.PROBING;return token;
        }
        void quarantine(long epoch,long now){if(epoch<stateEpoch)return;stateEpoch=epoch;strikes=0;until=now+10*base;
            policy=LmdbNativeProbeScheduler.State.QUARANTINED;}
        void finish(int id,int kind,long now){
            if(id!=token)return;token=0;if(flightEpoch<stateEpoch)return;
            stateEpoch=flightEpoch;
            if(kind==0){strikes=0;until=0;policy=LmdbNativeProbeScheduler.State.ACTIVE;}
            if(kind==1){strikes++;until=strikes>=2?0:now+base;
                policy=strikes>=2?LmdbNativeProbeScheduler.State.DORMANT_UNTIL_EPOCH:LmdbNativeProbeScheduler.State.COOLDOWN;}
            if(kind==2){until=now+base;policy=LmdbNativeProbeScheduler.State.COOLDOWN;}
            if(kind==3 && policy==LmdbNativeProbeScheduler.State.PROBING){until=0;
                policy=strikes==0?LmdbNativeProbeScheduler.State.UNKNOWN:LmdbNativeProbeScheduler.State.PROBE_ELIGIBLE;}
        }
    }

    @Test void checkpointIsCanonicalForSameStateRegardlessOfInsertionOrder() throws Exception {
        var dirs=new ArrayList<java.nio.file.Path>();String setting="rdf4j.lmdb.costModel.persist.enabled";
        String old=System.getProperty(setting);System.setProperty(setting,"true");
        try {
            byte[][] files=new byte[2][];
            for(int pass=0;pass<2;pass++){
                var dir=java.nio.file.Files.createTempDirectory("arbiter-order-");dirs.add(dir);
                var context=new LmdbNativeCostModelContext(dir,new UUID(0,901),()->0L,()->"DISABLED",()->0L,()->0L);
                var store=new LmdbNativeStoreCostModel(context,LmdbNativePosteriorConfig.defaults(),()->1000L);
                var keys=new ArrayList<LmdbNativeCostPosteriorStore.ExactKey>();
                for(String name:List.of("FB","Ea","a","z")){
                    var k=LmdbNativePhysicalVariantKey.builder(name).build();
                    keys.add(LmdbNativeCostPosteriorStore.ExactKey.of(LmdbNativeRegimeKey.STEADY,
                            LmdbNativeCostPosteriorStore.Lane.DIRECT,k));
                }
                if(pass==1)Collections.reverse(keys);
                for(var k:keys){
                    var node=store.posteriors().read(k,0,1000).exact();
                    store.posteriors().restoreExact(k,node);
                    store.probeScheduler().quarantine(k.variant(),LmdbNativeRegimeKey.STEADY,0);
                }
                store.persistence().persistIfDue(true);
                files[pass]=java.nio.file.Files.readAllBytes(dir.resolve(LmdbNativeCostModelPersistence.FILE_NAME));
            }
            assertTrue(Arrays.equals(files[0],files[1]),"same state serialized differently by insertion order");
        } finally {
            if(old==null)System.clearProperty(setting);else System.setProperty(setting,old);
            for(var dir:dirs)try(var walk=java.nio.file.Files.walk(dir)){
                for(var entry:walk.sorted(Comparator.reverseOrder()).toList())java.nio.file.Files.delete(entry);
            }
        }
    }
    @Test void checkpointCannotMixGlobalFamilyAndExactUpdateRevisions() throws Exception {
        var dir=java.nio.file.Files.createTempDirectory("arbiter-concurrent-checkpoint-");
        String setting="rdf4j.lmdb.costModel.persist.enabled",old=System.getProperty(setting);System.setProperty(setting,"true");
        AtomicBoolean stop=new AtomicBoolean();AtomicReference<Throwable> error=new AtomicReference<>();Thread worker=null;
        try {
            var context=new LmdbNativeCostModelContext(dir,new UUID(0,903),()->0L,()->"DISABLED",()->0L,()->0L);
            var store=new LmdbNativeStoreCostModel(context,LmdbNativePosteriorConfig.defaults(),()->1000L);
            var k=LmdbNativeArbiterConsistencyTest.exact("one");
            store.posteriors().updateCompleted(k,StrictMath.log(1000),1,0,1000);
            worker=Thread.ofPlatform().start(()->{try{
                while(!stop.get())store.posteriors().updateCompleted(k,StrictMath.log(1000),1,0,1000);
            }catch(Throwable failure){error.set(failure);}});
            for(int round=0;round<32;round++){
                store.persistence().persistIfDue(true);
                var loaded=new LmdbNativeStoreCostModel(context,LmdbNativePosteriorConfig.defaults(),()->1000L);
                var r=loaded.posteriors().read(k,0,1000);
                assertTrue(r.exact().completedCount>0);
                assertEquals(r.exact().completedCount,r.global().completedCount);
                assertEquals(r.exact().completedCount,r.family().completedCount);
            }
        } finally {
            stop.set(true);if(worker!=null)worker.join();
            if(old==null)System.clearProperty(setting);else System.setProperty(setting,old);
            try(var walk=java.nio.file.Files.walk(dir)){
                for(var entry:walk.sorted(Comparator.reverseOrder()).toList())java.nio.file.Files.delete(entry);
            }
        }
        if(error.get()!=null)throw new AssertionError(error.get());
    }
    static void parallel(int count,Runnable body)throws Exception{
        CountDownLatch gate=new CountDownLatch(1);AtomicReference<Throwable> problem=new AtomicReference<>();
        Thread[] threads=new Thread[count];
        for(int i=0;i<count;i++)threads[i]=Thread.ofPlatform().start(()->{try{gate.await();body.run();}
            catch(Throwable failure){problem.compareAndSet(null,failure);}});
        gate.countDown();for(Thread thread:threads)thread.join();
        if(problem.get()!=null)throw new AssertionError(problem.get());
    }
}
