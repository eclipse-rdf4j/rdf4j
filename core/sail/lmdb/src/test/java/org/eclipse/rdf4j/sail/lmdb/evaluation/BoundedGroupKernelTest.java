/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;
import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeKernelIr.*;
import java.util.*;
import org.eclipse.rdf4j.sail.lmdb.LmdbQueryMemoryManager;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.*;
import org.junit.jupiter.api.Test;

public class BoundedGroupKernelTest {
 static void check(boolean b,String m){NativeCountGroupStoreTest.check(b,m);}
 static void eq(Object a,Object b){NativeCountGroupStoreTest.eq(a,b);}
 static class Hooks extends KernelOrderSinkContractTest.Hooks {
  public boolean supportsCanonicalTermKeys(){return true;}
  public long canonicalTermKey(long id){return id;}
  public long termHashKey(long id){return canonicalTermKey(id);}
  public boolean sameRdfTerm(long a,long b){return canonicalTermKey(a)==canonicalTermKey(b);}
 }
 static class Plan implements KernelPlan {
  final long[][] rows;final long[] weights;int opens,closes;
  Plan(long[][] rows,long[] weights){this.rows=rows;this.weights=weights;}
  public void close() {}
  public Cursor open(){opens++;return new Cursor(){int at;boolean closed;
   public int fill(long[] target,int max){return fillWeighted(target,null,max);}
   public int fillWeighted(long[] target,long[] outWeights,int max){int n=Math.min(max,rows.length-at);for(int i=0;i<n;i++){
    System.arraycopy(rows[at],0,target,i*rows[at].length,rows[at].length);if(outWeights!=null)outWeights[i]=weights==null?1:weights[at];at++;}return n;}
   public void close(){if(!closed){closed=true;closes++;}}
  };}
 }
 static KernelContext context(Plan p,KernelHooks hooks,LmdbQueryMemoryManager.QueryLedger ledger){
  return new KernelContext(new NativeLmdbQuerySource.NativeAdjacency[0],new long[0],new long[0],new long[0][],new int[0],new int[0],hooks,null,new KernelPlan[]{p},16,null,null).withMemoryLedger(ledger);
 }
 static JaninoKernel create(Kernel k,boolean compiled)throws Exception{return compiled?KernelCompilationTestSupport.compile(k):(k.terminal instanceof Emit?LmdbNativeKernelInterpreter.forRows(k):LmdbNativeKernelInterpreter.forAggregate(k));}
 static List<String> run(Kernel ir,long[][] rows,boolean compiled,KernelHooks hooks,int batch)throws Exception{
  LmdbQueryMemoryManager manager=NativeCountGroupStoreTest.manager();Plan p=new Plan(rows,null);
  try(var q=manager.openQuery()){
   JaninoKernel k=create(ir,compiled);try {
   k.bind(context(p,hooks,q));long[] out=new long[batch*ir.stride()];List<String> result=new ArrayList<>();int n;
   while((n=k.fill(out,batch))>0)for(int i=0;i<n;i++)result.add(Arrays.toString(Arrays.copyOfRange(out,i*ir.stride(),(i+1)*ir.stride())));
   return result;
   } finally { k.close(); }
  }finally{eq(p.opens,p.closes);eq(0L,manager.usedBytes());}
 }
 static void budget(String value,Throwing body)throws Exception{String old=System.getProperty(NativeCountGroupStore.MAX_BYTES_PROPERTY);System.setProperty(NativeCountGroupStore.MAX_BYTES_PROPERTY,value);try{body.run();}finally{if(old==null)System.clearProperty(NativeCountGroupStore.MAX_BYTES_PROPERTY);else System.setProperty(NativeCountGroupStore.MAX_BYTES_PROPERTY,old);}}
 interface Throwing{void run()throws Exception;}
 static Kernel countKernel(int[] groups,AggregateOutput[] outputs,OutputMods mods){return new Kernel(3,List.of(new PlanRows(0,new int[]{0,1,2})),new Aggregate(groups,outputs,null,mods));}
 @Test public void compiledAndInterpretedMultiChannelCountsSpillAndKeepStableGroups()throws Exception{
  Kernel k=countKernel(new int[]{0},new AggregateOutput[]{AggregateOutput.countStar(),AggregateOutput.count(1),AggregateOutput.countDistinct(2)},new OutputMods(null,null,false,-1,0));
  check(k.boundedGroups,"not admitted");
  long[][] rows=new long[4096][3];Map<Long,long[]> count=new LinkedHashMap<>();Map<Long,Set<Long>> ds=new HashMap<>();
  for(int i=0;i<rows.length;i++){long g=(i*17)%701;rows[i]=new long[]{g,i%5==0?-1:i,i%79};long[] c=count.computeIfAbsent(g,x->new long[2]);c[0]++;if(rows[i][1]!=-1)c[1]++;ds.computeIfAbsent(g,x->new HashSet<>()).add(rows[i][2]);}
  List<String> expected=new ArrayList<>();count.forEach((g,c)->expected.add("["+g+", "+c[0]+", "+c[1]+", "+ds.get(g).size()+"]"));
  budget("4096",()->{for(boolean compiled:new boolean[]{false,true})eq(expected,run(k,rows,compiled,new Hooks(),7));});
 }
 @Test public void groupHavingOrderAndOffsetAreAppliedAfterExactDistinctMerge()throws Exception{
  Aggregate a=new Aggregate(new int[]{0},new AggregateOutput[]{AggregateOutput.countDistinct(2)},new Having(0,OP_GT,2),new OutputMods(new int[]{1,0},new boolean[]{true,false},false,11,3));
  Kernel k=new Kernel(3,List.of(new PlanRows(0,new int[]{0,1,2})),a);List<long[]> in=new ArrayList<>();List<long[]> expectedRows=new ArrayList<>();
  for(int g=0;g<50;g++){int n=1+g%9;for(int pass=0;pass<3;pass++)for(int i=0;i<n;i++)in.add(new long[]{g,0,i});if(n>2)expectedRows.add(new long[]{g,n});}
  List<String> expected=KernelOrderSinkContractTest.oracle(expectedRows,new int[]{1,0},new boolean[]{true,false},3,11);
  budget("4096",()->{for(boolean compiled:new boolean[]{false,true})eq(expected,run(k,in.toArray(long[][]::new),compiled,new Hooks(),1));});
 }
 @Test public void wholeRowOrderedDistinctUsesCanonicalIdentityNotHashCollisions()throws Exception{
  Kernel k=new Kernel(3,List.of(new PlanRows(0,new int[]{0,1,2})),new Emit(new int[]{0,2},true,new OutputMods(new int[]{0},null,false,-1,0)));
  Hooks hooks=new Hooks(){public long canonicalTermKey(long v){return v<0?v:v%1000;}public long termHashKey(long v){return 7;}};
  long[][] input=new long[2000][3];for(int i=0;i<1000;i++){input[i]=new long[]{1000+i,0,i%17};input[1000+i]=new long[]{i,0,i%17};}
  List<String> expected=new ArrayList<>();for(int i=0;i<1000;i++)expected.add("["+(1000+i)+", "+(i%17)+"]");
  budget("4096",()->{for(boolean compiled:new boolean[]{false,true})eq(expected,run(k,input,compiled,hooks,13));});
 }
 @Test public void unsupportedIdentityProofFallsBackWithoutCallingCanonicalizer()throws Exception{
  Kernel k=countKernel(new int[]{0},new AggregateOutput[]{AggregateOutput.countStar()},new OutputMods(null,null,false,-1,0));
  KernelHooks hooks=new KernelOrderSinkContractTest.Hooks();
  for(boolean compiled:new boolean[]{false,true})eq(List.of("[9, 2]","[2, 1]"),run(k,new long[][]{{9,0,0},{2,0,0},{9,0,0}},compiled,hooks,1));
 }
 @Test public void weightedPlanCountsRemainWeightedWithoutProductExpansion()throws Exception{
  Kernel k=countKernel(new int[]{0},new AggregateOutput[]{AggregateOutput.countStar(),AggregateOutput.count(1)},new OutputMods(null,null,false,-1,0));
  check(k.pipeline.get(0) instanceof PlanRows,"not plan input");
  for(boolean compiled:new boolean[]{false,true}){
   LmdbQueryMemoryManager manager=NativeCountGroupStoreTest.manager();Plan p=new Plan(new long[][]{{7,8,0},{7,-1,0},{9,1,0}},new long[]{1000,2000,3});
   try(var q=manager.openQuery()){JaninoKernel kernel=create(k,compiled);try{kernel.bind(context(p,new Hooks(),q));eq(List.of("[7, 3000, 1000]","[9, 3, 3]"),BoundedOrderKernelContractTest.drain(kernel,k,1));}finally{kernel.close();}}
   eq(0L,manager.usedBytes());
  }
 }
 @Test public void zeroInputGlobalDistinctAndGroupedEmptyAreCorrect()throws Exception{
  for(int[] group:new int[][]{new int[0],new int[]{0}}){Kernel k=countKernel(group,new AggregateOutput[]{AggregateOutput.countDistinct(1)},new OutputMods(null,null,false,-1,0));
   for(boolean compiled:new boolean[]{false,true})eq(group.length==0?List.of("[0]"):List.of(),run(k,new long[0][3],compiled,new Hooks(),1));
  }
 }
 @Test public void unorderedGroupSlicePreservesFirstEncounterAndUsesLongOffsets()throws Exception{
  long[][] input=new long[500][3];for(int i=0;i<500;i++)input[i]=new long[]{499-i,0,0};
  budget("4096",()->{
   Kernel k=countKernel(new int[]{0},new AggregateOutput[]{AggregateOutput.countStar()},new OutputMods(null,null,false,3,2));
   for(boolean compiled:new boolean[]{false,true})eq(List.of("[497, 1]","[496, 1]","[495, 1]"),run(k,input,compiled,new Hooks(),1));
   Kernel huge=countKernel(new int[]{0},new AggregateOutput[]{AggregateOutput.countStar()},new OutputMods(null,null,false,5,Long.MAX_VALUE-1));
   for(boolean compiled:new boolean[]{false,true})eq(List.of(),run(huge,input,compiled,new Hooks(),7));
  });
 }
 @Test public void zeroLimitNeverOpensProducerOrClaimsArena()throws Exception{
  Kernel k=countKernel(new int[]{0},new AggregateOutput[]{AggregateOutput.countStar()},new OutputMods(null,null,false,0,100));
  for(boolean compiled:new boolean[]{false,true}){
   LmdbQueryMemoryManager m=NativeCountGroupStoreTest.manager();Plan p=new Plan(new long[][]{{1,2,3}},null);
   try(var q=m.openQuery()){JaninoKernel kernel=create(k,compiled);try{kernel.bind(context(p,new Hooks(),q));eq(0,kernel.fill(new long[2],1));eq(0,p.opens);eq(0L,m.usedBytes());}finally{kernel.close();}}
  }
 }
 @Test public void unsupportedAggregateKindsAndOrderedDistinctKeepTheirEstablishedPath(){
  Kernel sum=countKernel(new int[]{0},new AggregateOutput[]{AggregateOutput.sum(1)},new OutputMods(null,null,false,-1,0));check(!sum.boundedGroups,"numeric states cannot be merged as counts");
  Kernel rows=new Kernel(3,List.of(new PlanRows(0,new int[]{0,1,2})),new Emit(new int[]{0,1},true,new OutputMods(null,null,false,1,0)));check(!rows.boundedGroups,"changed effectful limit demand");
 }
 @Test public void rolloutAndCanonicalIdentityAreDistinctContracts()throws Exception{
  String key=NativeCountGroupStore.ENABLED_PROPERTY,old=System.getProperty(key);
  try{System.setProperty(key,"true");Kernel on=countKernel(new int[]{0},new AggregateOutput[]{AggregateOutput.countStar()},OutputMods.none());
   System.setProperty(key,"false");Kernel off=countKernel(new int[]{0},new AggregateOutput[]{AggregateOutput.countStar()},OutputMods.none());
   check(on.boundedGroups&&!off.boundedGroups&&!on.shapeKey().equals(off.shapeKey()),"cache shape omits state representation");
  }finally{if(old==null)System.clearProperty(key);else System.setProperty(key,old);}
 }

 @Test public void comparatorFailureAfterSpillClosesGroupReservationsAndFiles()throws Exception{
  var before=NativeCountGroupStoreTest.files();long[][] input=new long[600][3];for(int i=0;i<input.length;i++)input[i]=new long[]{i,0,0};
  Kernel ir=countKernel(new int[]{0},new AggregateOutput[]{AggregateOutput.countStar()},new OutputMods(new int[]{0},null,true,20,0));
  budget("4096",()->{for(boolean compiled:new boolean[]{false,true}){
   LmdbQueryMemoryManager m=NativeCountGroupStoreTest.manager();Plan p=new Plan(input,null);var failure=new IllegalStateException("semantic order");
   try(var q=m.openQuery()){JaninoKernel k=create(ir,compiled);try{
    k.bind(context(p,new Hooks(){public int compareValues(long a,long b){throw failure;}},q));
    try{k.fill(new long[2],1);throw new AssertionError("missing failure");}catch(RuntimeException actual){check(actual==failure,"primary replaced");}
    eq(0L,m.usedBytes());eq(p.opens,p.closes);
   }finally{k.close();}}
  }});eq(before,NativeCountGroupStoreTest.files());
 }
 @Test public void queryMemoryIsAcquiredLazilyAndClosedLedgerDoesNotBecomeUnbounded()throws Exception{
  Kernel ir=countKernel(new int[]{0},new AggregateOutput[]{AggregateOutput.countStar()},OutputMods.none());
  for(boolean compiled:new boolean[]{false,true}){
   LmdbQueryMemoryManager m=NativeCountGroupStoreTest.manager();var q=m.openQuery();q.close();
   Plan p=new Plan(new long[][]{{7,0,0}},null);JaninoKernel k=create(ir,compiled);
   try{k.bind(context(p,new Hooks(),q));try{k.fill(new long[2],1);throw new AssertionError("closed ledger accepted");}catch(IllegalStateException expected){}eq(p.opens,p.closes);eq(0L,m.usedBytes());}finally{k.close();}
  }
 }

 @Test public void stableOrderTiesRetainFirstEncounterAfterCanonicalMergeReordering()throws Exception{
  long[][] input=new long[1000][3];for(int i=0;i<1000;i++)input[i]=new long[]{999-i,0,0};
  Kernel ir=countKernel(new int[]{0},new AggregateOutput[]{AggregateOutput.countStar()},new OutputMods(new int[]{1},null,false,17,3));
  List<String> expected=new ArrayList<>();for(int i=3;i<20;i++)expected.add("["+(999-i)+", 1]");
  budget("4096",()->{for(boolean compiled:new boolean[]{false,true})eq(expected,run(ir,input,compiled,new Hooks(),5));});
 }
}
