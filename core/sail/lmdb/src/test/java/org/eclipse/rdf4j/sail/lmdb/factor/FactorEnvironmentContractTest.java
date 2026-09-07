/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.factor;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** No engine/IR dependency: sidecars can also be used by standalone adjacency consumers. */
public class FactorEnvironmentContractTest {
    private static void eq(long a,long b){if(a!=b)throw new AssertionError(a+" != "+b);}
    private static void yes(boolean b){if(!b)throw new AssertionError();}
    private static void fails(Class<? extends Throwable> type,Runnable r){try{r.run();}catch(Throwable t){if(type.isInstance(t))return;throw new AssertionError(t);}throw new AssertionError("expected "+type);}
    private static BorrowedFactorBatch batch(HeapFactorSource source,long[]... lists){
        BorrowedFactorBatch b=new BorrowedFactorBatch(source,lists.length);b.reset(lists.length);
        for(int i=0;i<lists.length;i++)b.bindHeap(i,lists[i],0,lists[i].length);return b;
    }
    @Test public void demandedSubsetLeavesUnrelatedGroupsUnopened(){
        try(HeapFactorSource source=new HeapFactorSource(this)){
            BorrowedFactorBatch b=batch(source,new long[]{1,1,2},new long[]{10,20,30,40});
            FactorEnvironment e=new FactorEnvironment(3);e.bind(1,b,0,1);e.bind(2,b,1,1);
            try(FactorProductCursor c=new FactorProductCursor(3,2)){
                c.bind(e,2,5);eq(2,c.expandedMask());long total=0;int n=0;
                while(c.next()){total+=c.multiplicity();n++;fails(IllegalArgumentException.class,()->c.value(2));}
                eq(2,n);eq(15,total);eq(4,e.count(2));eq(60,e.countProduct(-1,5));
            }
        }
    }
    @Test public void independentReadersAndWeightedReplayMatchCartesianBag(){
        Random random=new Random(734);
        try(HeapFactorSource source=new HeapFactorSource(this)){
            for(int trial=0;trial<150;trial++){
                long[] a=new long[random.nextInt(20)],b=new long[random.nextInt(20)];
                for(int i=0;i<a.length;i++)a[i]=Long.MIN_VALUE+i/3;for(int i=0;i<b.length;i++)b[i]=i/2;
                BorrowedFactorBatch batch=batch(source,a,b);FactorEnvironment e=new FactorEnvironment(4);e.bind(1,batch,0,1);e.bind(3,batch,1,1);
                Map<String,Long> expected=new HashMap<>(),actual=new HashMap<>();
                for(long x:a)for(long y:b)expected.merge(x+":"+y,7L,Long::sum);
                try(FactorProductCursor c=new FactorProductCursor(4,1);FactorProductCursor independent=new FactorProductCursor(4,4)){
                    c.bind(e,-1,7);independent.bind(e,2,1);long total=0;
                    while(c.next()){actual.merge(c.value(1)+":"+c.value(3),c.multiplicity(),Long::sum);total+=c.multiplicity();}
                    if(!expected.equals(actual))throw new AssertionError("bag differs");eq(7L*a.length*b.length,total);
                    long independentCount=0;while(independent.next())independentCount+=independent.multiplicity();
                    eq(b.length==0?0:a.length,independentCount);
                }
            }
        }
    }
    @Test public void copiedSidecarsDoNotOwnOrAdvanceTheSharedRelation(){
        try(HeapFactorSource source=new HeapFactorSource(this)){
            BorrowedFactorBatch b=batch(source,new long[]{11,12},new long[]{47});
            FactorEnvironment e=new FactorEnvironment(4),copy=new FactorEnvironment(4);e.bind(1,b,0,1);e.bind(2,b,1,1);
            copy.append(e,2);e.clear();eq(2,copy.count(1));yes(copy.batch(1)==b);copy.clear();eq(2,b.count(0));
        }
    }
    @Test public void invalidDependenciesAndConflictingSlotsAreRejected(){
        try(HeapFactorSource s=new HeapFactorSource(this)){
            BorrowedFactorBatch b=batch(s,new long[]{1});FactorEnvironment e=new FactorEnvironment(4),x=new FactorEnvironment(4);
            e.bind(1,b,0,1);fails(IllegalArgumentException.class,()->e.bind(1,b,0,1));
            fails(IllegalArgumentException.class,()->e.bind(2,b,0,1L<<63));
            fails(IllegalArgumentException.class,()->e.bind(2,b,0,2));fails(IllegalArgumentException.class,()->e.bind(0,b,0,0));
            x.bind(1,b,0,1);fails(IllegalArgumentException.class,()->e.append(x,-1));
            fails(IllegalArgumentException.class,()->e.append(e,-1));
        }
    }
    @Test public void resetCloseAndEnvironmentMutationInvalidateReaders(){
        HeapFactorSource s=new HeapFactorSource(this);BorrowedFactorBatch b=batch(s,new long[]{1});FactorEnvironment e=new FactorEnvironment(2);e.bind(1,b,0,1);
        try(FactorProductCursor c=new FactorProductCursor(2,1)){
            c.bind(e,0,1);b.reset(1);fails(IllegalStateException.class,c::next);
            b.bindHeap(0,new long[]{2},0,1);e.clear();e.bind(1,b,0,1);c.bind(e,0,1);s.close();fails(IllegalStateException.class,c::next);
        }
        try(HeapFactorSource source=new HeapFactorSource(this);FactorProductCursor c=new FactorProductCursor(2,1)){
            BorrowedFactorBatch other=batch(source,new long[]{1});e.clear();e.bind(1,other,0,1);c.bind(e,2,1);e.clear();fails(IllegalStateException.class,c::next);
        }
    }
    @Test public void emptyFactorsAnnihilateProductsBeforeOverflow(){
        try(HeapFactorSource source=new HeapFactorSource(this)){
            BorrowedFactorBatch b=batch(source,new long[]{1,2},new long[0]);FactorEnvironment e=new FactorEnvironment(3);
            e.bind(1,b,0,1);e.bind(2,b,1,1);eq(0,e.countProduct(-1,Long.MAX_VALUE));
            fails(ArithmeticException.class,()->e.countProduct(2,Long.MAX_VALUE));
            try(FactorProductCursor c=new FactorProductCursor(3,1)){c.bind(e,0,Long.MAX_VALUE);yes(!c.next());}
        }
    }
    @Test public void windowsInterleaveWithScalarReadsWithoutLosingMembers(){
        long[] a={1,1,2,3,4,4,5,6};
        try(HeapFactorSource source=new HeapFactorSource(this)){
            BorrowedFactorBatch b=batch(source,a);try(BorrowedFactorBatch.Cursor c=b.cursor(3)){
                c.bind(0);yes(c.next());eq(1,c.value());eq(2,c.weight());
                int n=c.nextWindow(),start=c.windowStart();eq(2,n);eq(2,c.windowValues()[start]);eq(3,c.windowValues()[start+1]);
                yes(c.next());eq(4,c.value());eq(2,c.weight());eq(2,c.nextWindow());eq(0,c.nextWindow());yes(!c.next());
                c.bind(0);long total=0;while((n=c.nextWindow())!=0)for(int i=c.windowStart();i<c.windowStart()+n;i++)total+=c.windowWeights()[i];eq(a.length,total);
                b.reset(1);fails(IllegalStateException.class,c::nextWindow);
            }
        }
    }

    @Test public void copiedSelectedProvenanceIsCheckedBeforeAppending() {
        try(HeapFactorSource heap=new HeapFactorSource(this);SelectedFactorSource selected=new SelectedFactorSource(2)){
            BorrowedFactorBatch b=batch(heap,new long[]{1,2,3});FactorSelection ranges=new FactorSelection(2);ranges.add(0,2);
            BorrowedFactorBatch view=selected.select(b,0,ranges),copy=new BorrowedFactorBatch(selected,1);copy.reset(1);copy.copyLaneFrom(0,view,0);
            FactorEnvironment from=new FactorEnvironment(2),to=new FactorEnvironment(2);from.bind(1,copy,0,1);selected.select(b,0,ranges);
            fails(IllegalStateException.class,()->to.append(from,-1));eq(0,to.mask());
        }
    }
}
