/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.FactorPlanKernelContractTest.*;
import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeKernelIr.*;
import java.nio.file.Files;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.*;
import org.eclipse.rdf4j.sail.lmdb.factor.*;
import org.eclipse.rdf4j.sail.lmdb.factor.BorrowedTupleContractTest.WeightedTuples;

/** Actual masks, independent-group reducer and both execution tiers; explicit source fixtures. */
public class FactorWindowKernelContractTest {
 static KernelFactorPredicate ranges(long... dependencies) {
  return new KernelFactorPredicate(){
   public int guardCount(){return dependencies.length;}
   public long dependencies(int g){return dependencies[g];}
   public boolean test(int g,long[] row){return row[Long.numberOfTrailingZeros(dependencies[g])]==7;}
   public void filter(int g,long[][] c,long[] row,long[] m,int n){
    int at=Long.numberOfTrailingZeros(dependencies[g]);KernelIdMasks.compare(c[at],row[at],null,7,false,m,n);
   }
  };
 }
 /** One physical fiber can represent a huge exact long multiplicity. No unsafe load is performed. */
 static final class Weighted extends BorrowedFactorBatch.Source {
  final long value,count;int reads,opens,closes;
  Weighted(long value,long count){super(new Object());this.value=value;this.count=count;}
  BorrowedFactorBatch batch(){var b=new BorrowedFactorBatch(this,1);b.reset(1);
   if(count!=0)b.bindNative(0,BorrowedFactorBatch.ENCODED_RUN,1,0,0,count);return b;}
  public BorrowedFactorBatch.Reader openReader(){opens++;return new BorrowedFactorBatch.Reader(){
   public void bind(BorrowedFactorBatch b,int lane){checkOpen();}
   public int copyFibers(long offset,int max,long[] v,long[] w){checkOpen();reads++;if(offset==count)return 0;
    v[0]=value;w[0]=count-offset;return 1;}
   public void close(){closes++;}
  };}
 }
 @Test public void arithmeticMasksMatchUnsignedOracleOnRandomAndExtremeIds(){
  Random random=new Random(874981);long[] special={0,1,-1,-2,Long.MIN_VALUE,Long.MAX_VALUE,Long.MIN_VALUE+1};
  for(long a:special)for(long b:special){equal(a==b?-1:0,KernelIdMasks.equalMask(a,b));equal(Long.compareUnsigned(a,b)<0?-1:0,KernelIdMasks.belowMask(a,b));}
  for(int i=0;i<200000;i++){long a=random.nextLong(),b=random.nextLong();equal(a==b?-1:0,KernelIdMasks.equalMask(a,b));equal(Long.compareUnsigned(a,b)<0?-1:0,KernelIdMasks.belowMask(a,b));}
 }
 @Test public void vectorMasksPreserveTailsSelectionsAndSources(){
  Random random=new Random(7321);
  for(int n:new int[]{0,1,3,7,8,15,31,63,255,256,257,511}){
   long[] a=new long[n+5],b=new long[n+5],before,mask=new long[n+5];
   for(int i=0;i<a.length;i++){a[i]=random.nextLong();b[i]=i%3==0?a[i]:random.nextLong();}a[0]=-1;before=a.clone();
   for(boolean negate:new boolean[]{false,true}){Arrays.fill(mask,-1);KernelIdMasks.compare(a,0,b,0,negate,mask,n);
    for(int i=0;i<n;i++)equal((a[i]==b[i])!=negate?-1:0,mask[i]);for(int i=n;i<mask.length;i++)equal(-1,mask[i]);}
   for(long[] bound:new long[][]{{0,-1},{Long.MIN_VALUE,-1},{7,3},{-1,-1}}){Arrays.fill(mask,-1);if(n>3)mask[3]=0;
    KernelIdMasks.range(a,0,bound[0],bound[1],mask,n);for(int i=0;i<n;i++)equal(i!=3&&Long.compareUnsigned(a[i],bound[0])>=0&&Long.compareUnsigned(a[i],bound[1])<=0?-1:0,mask[i]);}
   Arrays.fill(mask,-1);KernelIdMasks.compatible(a,0,a[a.length-1],mask,n);
   for(int i=0;i<n;i++)equal(a[i]==-1||a[i]==a[a.length-1]?-1:0,mask[i]);
   Arrays.fill(mask,-1);KernelIdMasks.in4(a,0,a[0],a[1],a[2],a[3],mask,n);
   for(int i=0;i<n;i++)equal(a[i]==a[0]||a[i]==a[1]||a[i]==a[2]||a[i]==a[3]?-1:0,mask[i]);
   check(Arrays.equals(a,before));
  }
  long[] m={-1,-1,0,-1};KernelIdMasks.range(null,8,3,9,m,3);check(Arrays.equals(m,new long[]{-1,-1,0,-1}));
  KernelIdMasks.compare(null,9,null,9,true,m,3);check(Arrays.equals(m,new long[]{0,0,0,-1}));
  fails(IndexOutOfBoundsException.class,()->KernelIdMasks.range(new long[1],0,0,-1,new long[2],2));
  equal(Long.MAX_VALUE,KernelIdMasks.sumBounded(new long[]{Long.MAX_VALUE-5,5},new long[]{-1,-1},2));
 }
 @Test public void independentGroupsScanOnceAndNeverMultiplyBeforeAnnihilatingZero(){
  try(Weighted a=new Weighted(7,Long.MAX_VALUE);Weighted b=new Weighted(8,2);var reducer=new KernelFactorCount(new int[]{1,2},null)){
   FactorEnvironment e=new FactorEnvironment(3);e.bind(1,a.batch(),0,1);e.bind(2,b.batch(),0,1);
   equal(0,reducer.count(e,new long[2],Long.MAX_VALUE,ranges(1,2)));
   equal(1,a.reads);equal(1,b.reads);
  }
 }
 @Test public void positiveHugeProductOverflowsButExistenceDoesNot(){
  try(Weighted a=new Weighted(7,Long.MAX_VALUE);Weighted b=new Weighted(7,2);var reducer=new KernelFactorCount(new int[]{1,2},null)){
   FactorEnvironment e=new FactorEnvironment(3);e.bind(1,a.batch(),0,1);e.bind(2,b.batch(),0,1);
   fails(ArithmeticException.class,()->reducer.count(e,new long[2],2,ranges(1,2)));
   equal(1,reducer.count(e,new long[2],2,ranges(1,2),false));
  }
 }
 @Test public void crossGroupPredicateDeclinesBeforeInvocationOrReaderOpening(){
  try(Weighted a=new Weighted(7,2);Weighted b=new Weighted(7,3);var reducer=new KernelFactorCount(new int[]{1,2},null)){
   FactorEnvironment e=new FactorEnvironment(3);e.bind(1,a.batch(),0,1);e.bind(2,b.batch(),0,1);
   KernelFactorPredicate noEvaluation=new KernelFactorPredicate(){
    public int guardCount(){return 1;}public long dependencies(int g){return 3;}
    public boolean test(int g,long[] r){throw new AssertionError();}
    public void filter(int g,long[][] c,long[] r,long[] m,int n){throw new AssertionError();}
   };
   equal(KernelFactorCount.UNSUPPORTED,reducer.count(e,new long[2],1,noEvaluation));equal(0,a.opens);equal(0,b.opens);
  }
 }
 @Test public void sixtyFourthGuardAndOutputBitsAreNotLost(){
  try(HeapFactorSource source=new HeapFactorSource(this);var reducer=new KernelFactorCount(java.util.stream.IntStream.range(0,64).toArray(),null)){
   FactorEnvironment e=new FactorEnvironment(64);e.bind(63,heap(source,7,7,9),0,1);
   long[] deps=new long[64];Arrays.fill(deps,Long.MIN_VALUE);
   equal(6,reducer.count(e,new long[64],3,ranges(deps)));
  }
 }
 @Test public void sourceReplacementInvalidationAndCloseRemainCorrect(){
  Weighted a=new Weighted(7,3),b=new Weighted(7,5);var reducer=new KernelFactorCount(new int[]{1},null);
  try{
   FactorEnvironment first=new FactorEnvironment(2);var ba=a.batch();first.bind(1,ba,0,1);
   equal(3,reducer.count(first,new long[1],1,ranges(1)));
   FactorEnvironment second=new FactorEnvironment(2);second.bind(1,b.batch(),0,1);
   equal(5,reducer.count(second,new long[1],1,ranges(1)));equal(1,a.closes);
   ba.reset(1);fails(IllegalStateException.class,()->reducer.count(first,new long[1],1,ranges(1)));
   reducer.close();equal(1,b.closes);fails(IllegalStateException.class,()->reducer.count(second,new long[1],1,ranges(1)));
  }finally{reducer.close();a.close();b.close();}
 }
 @Test public void generatedAndInterpretedIndependentFiltersPreserveWeightsAndAliases()throws Exception{
  try(HeapFactorSource source=new HeapFactorSource(this);Metadata hidden=new Metadata()){
   FactorEnvironment e=new FactorEnvironment(4);e.bind(1,heap(source,7,7,8),0,1);e.bind(2,heap(source,4,5,5),0,1);e.bind(3,hidden.batch(11),0,1);
   Kernel k=shape(List.of(new BindAlias(Operand.col(1),4),new FilterCompareId(false,Operand.col(4),Operand.constant(0)),new FilterRangeUnsigned(Operand.col(2),1,2)),5,new int[]{0,1,2,3},new int[]{0},AggregateOutput.countStar(),AggregateOutput.count(0));
   check(k.factorCountGuards!=null);
   both(k,()->new Groups(new FactorEnvironment[]{e,e},new long[][]{{47,0,0,0},{47,0,0,0}},new long[]{2,3},0,1,2,3),new long[]{7,5,5},new long[][]{{47,220,220}});
   equal(0,hidden.opens);
  }
 }
 @Test public void tupleLocalComparisonIsZippedNotAnIndependentJoin()throws Exception{
  try(WeightedTuples tuple=new WeightedTuples(2,new long[][]{{7,7},{7,9},{9,9}},new long[]{3,5,2});Metadata hidden=new Metadata()){
   FactorEnvironment e=new FactorEnvironment(4);e.bindTuple(new int[]{1,2},tuple.batch(),0,1);e.bind(3,hidden.batch(13),0,1);
   Kernel k=shape(List.of(new FilterCompareId(false,Operand.col(1),Operand.col(2))),4,new int[]{0,1,2,3},new int[]{0},AggregateOutput.countStar());
   both(k,()->new Groups(new FactorEnvironment[]{e},new long[][]{{47,0,0,0}},new long[]{2},0,1,2,3),new long[0],new long[][]{{47,130}});equal(0,hidden.opens);
  }
 }
 @Test public void crossFactorComparisonRetainsExactProductFallback()throws Exception{
  try(HeapFactorSource source=new HeapFactorSource(this)){
   FactorEnvironment e=new FactorEnvironment(3);e.bind(1,heap(source,7,7,8),0,1);e.bind(2,heap(source,7,8,8),0,1);
   Kernel k=shape(List.of(new FilterCompareId(false,Operand.col(1),Operand.col(2))),3,new int[]{0,1,2},new int[]{0},AggregateOutput.countStar());
   both(k,()->new Groups(new FactorEnvironment[]{e},new long[][]{{47,0,0}},new long[]{3},0,1,2),new long[0],new long[][]{{47,12}});
  }
 }
 @Test public void nullCountOnlyNeedsExistenceAndPreservesZeroCountGroup()throws Exception{
  try(Weighted a=new Weighted(7,Long.MAX_VALUE);Metadata hidden=new Metadata()){
   FactorEnvironment e=new FactorEnvironment(4);e.bind(1,a.batch(),0,1);e.bind(2,hidden.batch(Long.MAX_VALUE),0,1);
   Kernel k=shape(equalTo(1,0),4,new int[]{0,1,2,3},new int[]{0},AggregateOutput.count(3));
   both(k,()->new Groups(new FactorEnvironment[]{e},new long[][]{{47,0,0,-1}},new long[]{2},0,1,2,3),new long[]{7},new long[][]{{47,0}});equal(0,hidden.opens);
  }
 }
 @Test public void unsignedWindowGuardsMatchFlatOracleOverSeededRandomProducts()throws Exception{
  Kernel k=shape(List.of(new FilterRangeUnsigned(Operand.col(1),0,1),new FilterInConstants(Operand.col(2),new int[]{2,3}),new BindAlias(Operand.col(0),4)),5,new int[]{0,1,2,3},new int[]{4},AggregateOutput.countStar());
  check(k.factorCountGuards!=null);
  var dir=Files.createTempDirectory("window-random-");
  try{
   JaninoKernel compiled=GeneratedBorrowedFtreeContractTest.compile(k.className(),LmdbNativeKernelEmitter.emit(k),dir);
   Random r=new Random(90184);long[] constants={Long.MIN_VALUE+2,Long.MIN_VALUE+5,3,4};
   for(int trial=0;trial<160;trial++)try(HeapFactorSource source=new HeapFactorSource(this)){
    int roots=1+r.nextInt(6);FactorEnvironment[] e=new FactorEnvironment[roots];long[][] rows=new long[roots][4];long[] weights=new long[roots];Map<Long,Long> expected=new TreeMap<>(Long::compareUnsigned);
    for(int root=0;root<roots;root++){
     long[] a=new long[r.nextInt(16)],b=new long[r.nextInt(14)],c=new long[r.nextInt(8)];
     for(int i=0;i<a.length;i++)a[i]=Long.MIN_VALUE+r.nextInt(8);for(int i=0;i<b.length;i++)b[i]=r.nextInt(7);Arrays.fill(c,21);
     rows[root][0]=r.nextInt(3);weights[root]=1+r.nextInt(5);e[root]=new FactorEnvironment(4);
     e[root].bind(1,heap(source,a),0,1);e[root].bind(2,heap(source,b),0,1);e[root].bind(3,heap(source,c),0,1);
     for(long x:a)if(Long.compareUnsigned(x,constants[0])>=0&&Long.compareUnsigned(x,constants[1])<=0)
      for(long y:b)if(y==3||y==4)for(long ignored:c)expected.merge(rows[root][0],weights[root],Math::addExact);
    }
    long[][] oracle=expected.entrySet().stream().map(v->new long[]{v.getKey(),v.getValue()}).toArray(long[][]::new);
    for(JaninoKernel exec:new JaninoKernel[]{compiled.getClass().getConstructor().newInstance(),LmdbNativeKernelInterpreter.forAggregate(k)}){
     long[][] actual=drain(exec,k,new Groups(e,rows,weights,0,1,2,3),constants,null);
     if(!Arrays.deepEquals(oracle,actual))throw new AssertionError("trial "+trial+Arrays.deepToString(actual));
    }
   }
  }finally{GeneratedBorrowedFtreeContractTest.remove(dir);}
 }
 @Test public void defaultCanBeDisabledAndUnsafeAliasLiftingIsDeclined(){
  Kernel on=shape(equalTo(1,0),2,new int[]{0,1},new int[0],AggregateOutput.countStar());check(on.factorCountGuards!=null);
  String previous=System.getProperty(FACTOR_WINDOWS_PROPERTY);
  try{System.setProperty(FACTOR_WINDOWS_PROPERTY,"false");Kernel off=shape(equalTo(1,0),2,new int[]{0,1},new int[0],AggregateOutput.countStar());check(off.factorCountGuards==null);check(!on.shapeKey().equals(off.shapeKey()));}
  finally{if(previous==null)System.clearProperty(FACTOR_WINDOWS_PROPERTY);else System.setProperty(FACTOR_WINDOWS_PROPERTY,previous);}
  Kernel unsafe=shape(List.of(new BindAlias(Operand.col(3),4),new FilterCompareId(false,Operand.col(1),Operand.col(4))),5,new int[]{0,1},new int[0],AggregateOutput.countStar());check(unsafe.factorCountGuards==null);
  Kernel large=shape(List.of(new FilterInConstants(Operand.col(1),new int[]{0,1,2,3,4})),2,new int[]{0,1},new int[0],AggregateOutput.countStar());check(large.factorCountGuards==null);
 }
 @Test public void scalarRejectionPrecedesCrossFactorFallbackAndDeferredGroupKeys()throws Exception{
  try(Metadata hidden=new Metadata()){
   FactorEnvironment env=new FactorEnvironment(3);env.bind(1,hidden.batch(Long.MAX_VALUE),0,1);env.bind(2,hidden.batch(Long.MAX_VALUE),0,1);
   Kernel k=shape(List.of(new FilterCompareId(false,Operand.col(0),Operand.constant(0)),new FilterCompareId(false,Operand.col(1),Operand.col(2))),3,new int[]{0,1,2},new int[]{1},AggregateOutput.countStar());
   both(k,()->new Groups(new FactorEnvironment[]{env},new long[][]{{47,0,0}},new long[]{2},0,1,2),new long[]{48},new long[0][]);equal(0,hidden.opens);
  }
 }
 @Test public void witnessPhaseAvoidsCompletingLargeDoomedSiblingAndResumesWithoutReplay(){
  class Sequence extends BorrowedFactorBatch.Source{
   int copies,decoded;Sequence(){super(new Object());}
   BorrowedFactorBatch batch(){var b=new BorrowedFactorBatch(this,1);b.reset(1);b.bindNative(0,BorrowedFactorBatch.ENCODED_RUN,1,0,0,4096);return b;}
   public BorrowedFactorBatch.Reader openReader(){return new BorrowedFactorBatch.Reader(){
    public void bind(BorrowedFactorBatch b,int lane){}
    public int copyFibers(long offset,int maximum,long[] values,long[] weights){int n=(int)Math.min(maximum,4096-offset);copies++;decoded+=n;Arrays.fill(values,0,n,7);Arrays.fill(weights,0,n,1);return n;}
   };}
  }
  try(Sequence source=new Sequence();Weighted reject=new Weighted(8,3);Weighted accept=new Weighted(7,3);var counter=new KernelFactorCount(new int[]{1,2},null)){
   FactorEnvironment env=new FactorEnvironment(3);var a=source.batch();env.bind(1,a,0,1);env.bind(2,reject.batch(),0,1);
   equal(0,counter.count(env,new long[2],1,ranges(1,2)));equal(256,source.decoded);equal(1,source.copies);
   FactorEnvironment next=new FactorEnvironment(3);next.bind(1,a,0,1);next.bind(2,accept.batch(),0,1);
   source.decoded=source.copies=0;equal(4096*3,counter.count(next,new long[2],1,ranges(1,2)));equal(4096,source.decoded);equal(16,source.copies);
  }
 }

}
