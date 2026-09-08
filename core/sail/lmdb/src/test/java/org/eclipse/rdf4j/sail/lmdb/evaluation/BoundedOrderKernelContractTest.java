/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;
import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeKernelIr.*;
import java.util.*;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.*;
import org.junit.jupiter.api.Test;

public class BoundedOrderKernelContractTest {
 static final class Plan implements KernelPlan {
  final long[][] rows;int opens,closes,ownerCloses;
  Plan(long[][] rows){this.rows=rows;}
  public Cursor open(){opens++;return new Cursor(){int at;boolean closed;
   public int fill(long[] out,int max){int n=Math.min(max,rows.length-at);for(int i=0;i<n;i++)System.arraycopy(rows[at++],0,out,i*rows[0].length,rows[0].length);return n;}
   public void close(){if(!closed){closed=true;closes++;}}
  };}
  public void close(){ownerCloses++;}
 }
 static KernelContext context(Plan plan,KernelHooks hooks){return new KernelContext(new NativeLmdbQuerySource.NativeAdjacency[0],new long[0],new long[0],new long[0][],new int[0],new int[0],hooks,null,new KernelPlan[]{plan},16,null,null);}
 static JaninoKernel create(Kernel ir,boolean compiled)throws Exception{return compiled?KernelCompilationTestSupport.compile(ir):(ir.terminal instanceof Emit?LmdbNativeKernelInterpreter.forRows(ir):LmdbNativeKernelInterpreter.forAggregate(ir));}
 static List<String> drain(JaninoKernel k,Kernel ir,int batch){long[] buf=new long[batch*ir.stride()];List<String> rows=new ArrayList<>();int n;while((n=k.fill(buf,batch))>0)for(int i=0;i<n;i++)rows.add(Arrays.toString(Arrays.copyOfRange(buf,i*ir.stride(),(i+1)*ir.stride())));return rows;}
 static List<String> run(Kernel ir,long[][] input,boolean compiled,int batch,KernelHooks hooks)throws Exception{
  Plan plan=new Plan(input);JaninoKernel k=create(ir,compiled);
  try{k.bind(context(plan,hooks));List<String> result=drain(k,ir,batch);
   if(ir.boundedOrder){var f=k.getClass().getDeclaredField("out");f.setAccessible(true);KernelOrderSinkContractTest.eq(0,((long[])f.get(k)).length);}
   return result;
  }finally{k.close();KernelOrderSinkContractTest.eq(plan.opens,plan.closes);}
 }
 static Kernel rows(OutputMods mods,boolean distinct){return new Kernel(2,List.of(new PlanRows(0,new int[]{0,1})),new Emit(new int[]{0,1},distinct,mods));}
 @Test public void bothTiersUseBoundedTopKInsteadOfOutputMaterialization()throws Exception{
  long[][] input=new long[10000][];for(int i=0;i<input.length;i++)input[i]=new long[]{10000-i,i};
  Kernel ir=rows(new OutputMods(new int[]{0},null,false,20,10),false);
  KernelOrderSinkContractTest.check(ir.boundedOrder,"default disabled");
  List<String> expected=KernelOrderSinkContractTest.oracle(Arrays.asList(input),new int[]{0},null,10,20);
  for(boolean compiled:new boolean[]{false,true})KernelOrderSinkContractTest.eq(expected,run(ir,input,compiled,3,null));
 }
 @Test public void spillAndTopKParityOverRandomRowsAndTiedKeys()throws Exception{
  Random random=new Random(98761);Kernel ir=rows(new OutputMods(new int[]{0},new boolean[]{true},false,57,13),false);
  for(int round=0;round<25;round++){
   long[][] input=new long[200+round][];for(int i=0;i<input.length;i++)input[i]=new long[]{random.nextInt(13)-6,i};
   List<String> expected=KernelOrderSinkContractTest.oracle(Arrays.asList(input),new int[]{0},new boolean[]{true},13,57);
   String before=System.getProperty(NativeSpillSort.MAX_BYTES_PROPERTY);System.setProperty(NativeSpillSort.MAX_BYTES_PROPERTY,round%2==0?"2048":"1048576");
   try{for(boolean compiled:new boolean[]{false,true})KernelOrderSinkContractTest.eq(expected,run(ir,input,compiled,1+(round%7),null));}
   finally{if(before==null)System.clearProperty(NativeSpillSort.MAX_BYTES_PROPERTY);else System.setProperty(NativeSpillSort.MAX_BYTES_PROPERTY,before);}
  }
 }
 @Test public void fullSortDistinctAndSemanticOrderingRemainExact()throws Exception{
  long[][] input={{7,2},{7,2},{-1,9},{2,3},{7,1},{2,3},{5,1}};
  Kernel ir=rows(new OutputMods(new int[]{0,1},new boolean[]{false,true},true,-1,1),true);
  List<String> expected=List.of("[2, 3]","[5, 1]","[7, 2]","[7, 1]");
  for(boolean compiled:new boolean[]{false,true})KernelOrderSinkContractTest.eq(expected,run(ir,input,compiled,2,new KernelOrderSinkContractTest.Hooks()));
 }
 @Test public void groupedAggregateOrdersCountsNotEveryInputRow()throws Exception{
  Kernel ir=new Kernel(2,List.of(new PlanRows(0,new int[]{0,1})),new Aggregate(new int[]{0},new AggregateOutput[]{AggregateOutput.countStar()},null,new OutputMods(new int[]{1,0},new boolean[]{true,false},false,2,0)));
  long[][] input={{7,2},{7,1},{7,9},{2,3},{2,3},{5,1}};
  for(boolean compiled:new boolean[]{false,true})KernelOrderSinkContractTest.eq(List.of("[7, 3]","[2, 2]"),run(ir,input,compiled,1,null));
 }
 @Test public void limitZeroNeverOpensOrderedInputEvenWithHugeOffset()throws Exception{
  Kernel ir=rows(new OutputMods(new int[]{0},null,false,0,Long.MAX_VALUE),false);
  for(boolean compiled:new boolean[]{false,true}){Plan plan=new Plan(new long[][]{{1,2}});JaninoKernel k=create(ir,compiled);
   try{k.bind(context(plan,null));KernelOrderSinkContractTest.eq(List.of(),drain(k,ir,1));KernelOrderSinkContractTest.eq(0,plan.opens);}finally{k.close();}
  }
 }
 @Test public void largeSlicesDoNotBecomeNegativeOrIntTruncated()throws Exception{
  Kernel ir=rows(new OutputMods(new int[]{0},null,false,Long.MAX_VALUE,Long.MAX_VALUE-3),false);
  for(boolean compiled:new boolean[]{false,true})KernelOrderSinkContractTest.eq(List.of(),run(ir,new long[][]{{1,2},{3,4}},compiled,1,null));
 }
 @Test public void rolloutChoiceIsPartOfTheKernelShape(){
  String key="rdf4j.lmdb.janinoCodegen.boundedOrder",old=System.getProperty(key);
  try{System.setProperty(key,"true");Kernel fast=rows(new OutputMods(new int[]{0},null,false,3,0),false);
   System.setProperty(key,"false");Kernel legacy=rows(new OutputMods(new int[]{0},null,false,3,0),false);
   KernelOrderSinkContractTest.check(!fast.shapeKey().equals(legacy.shapeKey()),"cache identity missing");
   KernelOrderSinkContractTest.check(fast.boundedOrder&&!legacy.boundedOrder,"choice not captured");
  }finally{if(old==null)System.clearProperty(key);else System.setProperty(key,old);}
 }
 @Test public void badComparatorReleasesAllKernelOwners()throws Exception{
  Kernel ir=rows(new OutputMods(new int[]{0},null,true,1,0),false);RuntimeException error=new IllegalStateException("comparison");
  for(boolean compiled:new boolean[]{false,true}){Plan plan=new Plan(new long[][]{{1,2},{3,4}});JaninoKernel k=create(ir,compiled);
   try{k.bind(context(plan,new KernelOrderSinkContractTest.Hooks(){public int compareValues(long a,long b){throw error;}}));
    try{k.fill(new long[2],1);throw new AssertionError("expected failure");}catch(RuntimeException actual){KernelOrderSinkContractTest.check(actual==error,"primary changed");}
    KernelOrderSinkContractTest.eq(plan.opens,plan.closes);KernelOrderSinkContractTest.eq(1,plan.ownerCloses);
   }finally{k.close();}
  }
 }
}
