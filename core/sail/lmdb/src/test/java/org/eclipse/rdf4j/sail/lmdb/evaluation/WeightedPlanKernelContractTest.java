/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.GeneratedBorrowedFtreeContractTest;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.JaninoKernel;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelContext;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.KernelPlan;

/** Executes Java emitted by the actual IR emitter, not an imitation of its weighted update loop. */
public class WeightedPlanKernelContractTest {
 private static final class Bags implements KernelPlan {
  final long[][] rows;final long[] weights;boolean weightedRead,closed;
  Bags(long[][]rows,long[]weights){this.rows=rows;this.weights=weights;}
  @Override public Cursor open() {return new Cursor(){int at;long remaining;
   @Override public int fill(long[] out,int maximum){int n=0;while(n<maximum&&at<rows.length){
    if(remaining==0)remaining=weights[at];System.arraycopy(rows[at],0,out,n*rows[at].length,rows[at].length);n++;
    if(--remaining==0)at++;
   }return n;}
   @Override public int fillWeighted(long[]out,long[]w,int maximum){weightedRead=true;int n=0;
    while(n<maximum&&at<rows.length){System.arraycopy(rows[at],0,out,n*rows[at].length,rows[at].length);w[n++]=weights[at++];}return n;}
   @Override public void close(){closed=true;}
  };}
  @Override public void close(){closed=true;}
 }
 private static LmdbNativeKernelIr.Kernel shape(int width,int[] groups,boolean counted,boolean distinct,boolean filter){
  var outputs=counted?new LmdbNativeKernelIr.AggregateOutput[]{LmdbNativeKernelIr.AggregateOutput.countStar(),
    distinct?LmdbNativeKernelIr.AggregateOutput.countDistinct(1):LmdbNativeKernelIr.AggregateOutput.count(1)}
    :new LmdbNativeKernelIr.AggregateOutput[]{LmdbNativeKernelIr.AggregateOutput.countStar()};
  int[] cols=new int[width];for(int i=0;i<width;i++)cols[i]=i;
  List<LmdbNativeKernelIr.Node> nodes=new ArrayList<>();nodes.add(new LmdbNativeKernelIr.PlanRows(0,cols));
  if(filter)nodes.add(new LmdbNativeKernelIr.FilterCompareId(false,LmdbNativeKernelIr.Operand.col(0),LmdbNativeKernelIr.Operand.col(1)));
  return new LmdbNativeKernelIr.Kernel(Math.max(1,width),nodes,
    new LmdbNativeKernelIr.Aggregate(groups,outputs,null,LmdbNativeKernelIr.OutputMods.none()));
 }
 private static long[][] run(LmdbNativeKernelIr.Kernel shape,KernelPlan plan,int width) throws Exception {
  Path dir=Files.createTempDirectory("weighted-ir-");
  try {
   JaninoKernel kernel=GeneratedBorrowedFtreeContractTest.compile(shape.className(),LmdbNativeKernelEmitter.emit(shape),dir);
   try {
    kernel.bind(new KernelContext(null,new long[0],new long[0],null,null,null,new KernelPlan[]{plan},16));
    List<long[]> rows=new ArrayList<>();long[] buffer=new long[width*2];int n;
    while((n=kernel.fill(buffer,2))>0)for(int i=0;i<n;i++)rows.add(Arrays.copyOfRange(buffer,i*width,(i+1)*width));
    rows.sort((a,b)->Long.compareUnsigned(a[0],b[0]));return rows.toArray(long[][]::new);
   }finally{kernel.close();}
  }finally{GeneratedBorrowedFtreeContractTest.remove(dir);}
 }
 private static void equal(long[][]a,long[][]b){if(!Arrays.deepEquals(a,b))throw new AssertionError(Arrays.deepToString(a)+" != "+Arrays.deepToString(b));}
 @Test public void zeroColumnCountTransfersMoreThanIntMaxRowsWithoutEnumeration() throws Exception {
  Bags plan=new Bags(new long[][]{{},{},{}},new long[]{3_000_000_000L,5,7});
  equal(new long[][]{{3_000_000_012L}},run(shape(0,new int[0],false,false,false),plan,1));
  if(!plan.weightedRead||!plan.closed)throw new AssertionError("weighted transfer or close missing");
 }
 @Test public void groupedCountPreservesUnboundTermsAndDuplicateParents() throws Exception {
  Bags plan=new Bags(new long[][]{{11,101},{47,-1},{11,102},{47,103}},new long[]{2,3,7,5});
  equal(new long[][]{{11,9,9},{47,8,5}},run(shape(2,new int[]{0},true,false,false),plan,3));
  if(!plan.weightedRead)throw new AssertionError("bags expanded");
 }
 @Test public void ordinaryPlanUsesDefaultUnitWeights() throws Exception {
  KernelPlan plan=new KernelPlan(){public Cursor open(){return new Cursor(){boolean done;public int fill(long[]o,int n){if(done)return 0;done=true;return 2;}public void close(){}};}public void close(){}};
  equal(new long[][]{{2}},run(shape(0,new int[0],false,false,false),plan,1));
 }
 @Test public void distinctDeclinesAndIdFiltersRetainExactWeights() throws Exception {
  var distinct=shape(2,new int[0],true,true,false);
  Bags d=new Bags(new long[][]{{11,101},{11,101},{11,102}},new long[]{2,3,5});
  // COUNT(DISTINCT) uses RDF-term hooks in production; legality can be checked without inventing value semantics.
  if(LmdbNativeKernelIr.weightedPlanCount(distinct)||LmdbNativeKernelEmitter.emit(distinct).contains(".fillWeighted("))throw new AssertionError("distinct admitted");
  Bags filtered=new Bags(new long[][]{{11,11},{11,47}},new long[]{2,3});
  equal(new long[][]{{2}},run(shape(2,new int[0],false,false,true),filtered,1));
  if(!filtered.weightedRead)throw new AssertionError("factor fallback failed to retain weights");
 }
 @Test public void weightedOverflowDoesNotWrap() throws Exception {
  Bags plan=new Bags(new long[][]{{},{}},new long[]{Long.MAX_VALUE,1});
  try{run(shape(0,new int[0],false,false,false),plan,1);throw new AssertionError("overflow wrapped");}
  catch(ArithmeticException expected){}
  if(!plan.closed)throw new AssertionError("failed kernel leaked its plan cursor");
 }
}
