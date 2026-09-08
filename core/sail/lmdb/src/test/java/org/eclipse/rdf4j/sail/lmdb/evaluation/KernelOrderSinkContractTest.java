/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.*;
import org.junit.jupiter.api.Test;

/** Actual shared native sorting primitives; independent stable collection oracle. */
public class KernelOrderSinkContractTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static void eq(Object a,Object b){if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 static List<String> drain(KernelOrderSink sink,int width,int batch){
  List<String> out=new ArrayList<>(); long[] buf=new long[width*batch];
  int n;while((n=sink.fill(buf,batch))>0)for(int i=0;i<n;i++)out.add(Arrays.toString(Arrays.copyOfRange(buf,i*width,(i+1)*width)));
  return out;
 }
 static Set<Path> files()throws IOException{try(var paths=Files.list(Path.of(System.getProperty("java.io.tmpdir")))){return paths.filter(p->p.getFileName().toString().startsWith("rdf4j-lmdb-native-sort-")).collect(java.util.stream.Collectors.toSet());}}
 static void property(String value,Runnable action){String key=NativeSpillSort.MAX_BYTES_PROPERTY,old=System.getProperty(key);System.setProperty(key,value);try{action.run();}finally{if(old==null)System.clearProperty(key);else System.setProperty(key,old);}}
 static List<String> oracle(List<long[]> input,int[] keys,boolean[] desc,long offset,long limit){
  List<long[]> sorted=new ArrayList<>(input);sorted.sort((a,b)->{for(int i=0;i<keys.length;i++){int c=Long.compareUnsigned(a[keys[i]],b[keys[i]]);if(c!=0)return desc!=null&&desc[i]?-c:c;}return 0;});
  return sorted.stream().skip(offset).limit(limit<0?Long.MAX_VALUE:limit).map(Arrays::toString).toList();
 }
 @Test public void randomizedTopKAndExternalOrderMatchStableOracle(){
  Random random=new Random(18418);
  for(int trial=0;trial<100;trial++){
   int n=trial*9,width=4; List<long[]> rows=new ArrayList<>();
   for(int i=0;i<n;i++)rows.add(new long[]{random.nextInt(19)-9,random.nextInt(7),i,random.nextLong()});
   int[] keys=trial%2==0?new int[]{0}:new int[]{1,0};boolean[] desc=trial%3==0?new boolean[keys.length]:null;
   if(desc!=null)desc[0]=true;long offset=trial%23,limit=trial%4==0?-1:trial%47;
   property(trial%2==0?"4096":"1048576",()->{
    try(KernelOrderSink sink=new KernelOrderSink(width,keys,desc,null,null,offset,limit)){
     for(long[] row:rows)sink.add(row);sink.finish();eq(oracle(rows,keys,desc,offset,limit),drain(sink,width,7));
    }
   });
  }
 }
 @Test public void millionInputRowsKeepOnlyTheRequestedPrefix(){
  property("1048576",()->{
   try(KernelOrderSink sink=new KernelOrderSink(3,new int[]{0},null,null,null,10,20)){
    long[] row=new long[3];for(int i=1_000_000;i>0;i--){row[0]=i;row[1]=-i;row[2]=7;sink.add(row);}
    check(sink.usedTopK(),"bounded top-K absent");check(sink.peakPayloadBytes()<=30L*36,"payload grew with input");
    sink.finish();List<String> actual=drain(sink,3,1);eq(20,actual.size());eq("[11, -11, 7]",actual.get(0));eq(1_000_000L,sink.inputRows());
   }
  });
 }
 @Test public void stableTiesAcrossEvictionAndDiskRuns(){
  property("1024",()->{
   List<long[]> rows=new ArrayList<>();for(int i=0;i<1000;i++)rows.add(new long[]{i%4,i});
   for(long limit:new long[]{20,200,-1})try(KernelOrderSink sink=new KernelOrderSink(2,new int[]{0},null,null,null,3,limit)){
    rows.forEach(sink::add);sink.finish();eq(oracle(rows,new int[]{0},null,3,limit),drain(sink,2,3));
   }
  });
 }
 @Test public void spillRunsAreCompactedAndDeletedAfterPartialConsumption()throws Exception{
  Set<Path> before=files();
  property("256",()->{try(KernelOrderSink sink=new KernelOrderSink(2,new int[]{0},null,null,null,0,-1)){
   for(int i=4095;i>=0;i--)sink.add(new long[]{i,i+1});
   check(sink.spilledRows()>0,"no spills");check(sink.peakRunCount()<=48,"linear run metadata");
   sink.finish();long[] row=new long[2];eq(1,sink.fill(row,1));eq(0L,row[0]);
  }});eq(before,files());
 }
 @Test public void largeOffsetAndLimitDoNotClampToIntOrOverflow(){
  property("1024",()->{try(KernelOrderSink sink=new KernelOrderSink(1,new int[]{0},null,null,null,Long.MAX_VALUE-1,100)){
   sink.add(new long[]{4});sink.finish();eq(List.of(),drain(sink,1,2));
  }});
 }
 @Test public void zeroLimitDoesNotAllocateOrInvokeComparator(){
  try(KernelOrderSink sink=new KernelOrderSink(1,new int[]{0},null,new Hooks(){public int compareValues(long a,long b){throw new AssertionError();}},null,Long.MAX_VALUE,0)){
   sink.add(new long[]{3});sink.finish();eq(List.of(),drain(sink,1,1));eq(0L,sink.peakPayloadBytes());
  }
 }
 @Test public void zeroWidthMappingsStillHaveMultiplicity(){
  try(KernelOrderSink sink=new KernelOrderSink(0,new int[0],null,null,null,3,5)){
   for(int i=0;i<20;i++)sink.add(new long[0]);sink.finish();eq(5,drain(sink,0,2).size());
  }
 }
 static class Hooks implements KernelHooks {
  public boolean testFilter(int id,long a,long b,long c){return true;}public long computeBind(int id,long a,long b){return a;}
  public int compareValues(long a,long b){return Long.compare(a,b);}public boolean isNumeric(long v){return true;}
  public double doubleValue(long v){return v;}public void accumulateNumeric(int id,int group,long v){}
 }
 @Test public void semanticComparatorAndDescendingMinValueAreNotRawIdOrder(){
  Hooks hooks=new Hooks(){public int compareValues(long a,long b){int c=Long.compare(a%10,b%10);return c<0?Integer.MIN_VALUE:c>0?Integer.MAX_VALUE:0;}};
  try(KernelOrderSink sink=new KernelOrderSink(1,new int[]{0},new boolean[]{true},hooks,null,0,3)){
   for(long value:new long[]{31,22,19,48})sink.add(new long[]{value});sink.finish();eq(List.of("[19]","[48]","[22]"),drain(sink,1,2));
  }
 }
 @Test public void cancellationDuringDiskDrainClosesAllOwnedRuns()throws Exception{
  Set<Path> before=files();KernelCancellation cancel=new KernelCancellation(System.nanoTime()+1_000_000_000_000L);
  property("2048",()->{try(KernelOrderSink sink=new KernelOrderSink(1,new int[]{0},null,null,cancel,0,-1)){
   for(int i=2000;i>0;i--)sink.add(new long[]{i});sink.finish();cancel.cancel();
   try{sink.fill(new long[1],1);throw new AssertionError("expected cancellation");}catch(KernelCancelledException expected){}
  }});eq(before,files());
 }
 @Test public void comparatorFailureReleasesAlreadySpilledRuns()throws Exception{
  Set<Path> before=files();RuntimeException boom=new IllegalStateException("comparator");boolean[] fail={false};
  Hooks hooks=new Hooks(){public int compareValues(long a,long b){if(fail[0])throw boom;return Long.compare(a,b);}};
  property("2048",()->{try(KernelOrderSink sink=new KernelOrderSink(2,new int[]{0},null,hooks,null,0,-1)){
   for(int i=0;i<300;i++)sink.add(new long[]{300-i,i});fail[0]=true;
   try{sink.finish();throw new AssertionError("expected failure");}catch(RuntimeException actual){check(actual==boom,"failure changed");}
  }});eq(before,files());
 }
 @Test public void probeCapacityStillBoundsWorkIncludingSpilledRows(){
  KernelCancellation cancel=new KernelCancellation(System.nanoTime()+1_000_000_000_000L,null,null,4);
  try(KernelOrderSink sink=new KernelOrderSink(1,new int[]{0},null,null,cancel,0,1)){
   for(int i=0;i<4;i++)sink.add(new long[]{i});
   try{sink.add(new long[]{5});throw new AssertionError("expected cap");}catch(KernelCancelledException expected){}
  }
 }
 @Test public void splitKeyPayloadRunFormatSurvivesLeveledCompaction()throws Exception{
  Set<Path> before=files();
  property("2048",()->{try(NativeSpillSort sort=new NativeSpillSort(3,1,(a,ao,b,bo)->Long.compare(a[ao],b[bo]))){
   try{for(int i=999;i>=0;i--)sort.add(new long[]{i,2L*i,3L*i},999-i);
    try(NativeSortedRows result=sort.sortedRows()){
     long[] row=new long[3];for(int i=0;i<1000;i++){check(result.next(row),"short");check(Arrays.equals(row,new long[]{i,2L*i,3L*i}),"payload mismatch");}check(!result.next(row),"long");
    }
   }catch(IOException ex){throw new UncheckedIOException(ex);}
  }});eq(before,files());
 }
 @Test public void nativeArenaGrowthNeverExceedsRunCapacity(){
  NativeSortBuffer buffer=new NativeSortBuffer(4,2,LmdbNativeAttemptMetrics.direct(),37);
  try{for(int i=0;i<37;i++)buffer.append(new long[]{i,1,2,3},i);eq(37,buffer.ordinals.length);eq(37*4,buffer.rows.length);}finally{buffer.close();}
 }
 @Test public void malformedRunLengthsFailWithoutLeakingFiles()throws Exception{
  Path p=Files.createTempFile("rdf4j-lmdb-native-sort-", ".run");
  try(DataOutputStream out=new DataOutputStream(Files.newOutputStream(p))){out.writeInt(RunReader.ROW_MAJOR_MAGIC);out.writeInt(1);out.writeInt(1);out.writeLong(-1);}
  try{new RunReader(p,1,1);throw new AssertionError("expected malformed run failure");}catch(IOException expected){}
  check(!Files.exists(p),"bad file retained");
 }
 @Test public void cancellationIsPolledDuringRunWritingWithoutComparatorCalls()throws Exception{
  Set<Path> before=files();RuntimeException stop=new IllegalStateException("write cancelled");int[] calls={0};
  try(NativeSpillSort sort=new NativeSpillSort(1,1,(a,ao,b,bo)->0,-1,LmdbNativeAttemptMetrics.direct(),65536,
          ()->{if(++calls[0]==3)throw stop;})){
   try{for(int n=0;n<10000;n++)sort.add(new long[]{n},n);sort.sortedRows();throw new AssertionError("no cancellation");}
   catch(RuntimeException actual){check(actual==stop,"primary failure changed");}
  }
  eq(3,calls[0]);eq(before,files());
 }
 @Test public void sinkCapturesItsBudgetBeforeGlobalPropertyChanges(){
  property("1024",()->{try(KernelOrderSink sink=new KernelOrderSink(2,new int[]{0},null,null,null,0,-1)){
   String old=System.getProperty(NativeSpillSort.MAX_BYTES_PROPERTY);
   try{System.setProperty(NativeSpillSort.MAX_BYTES_PROPERTY,"67108864");
    for(int i=0;i<1000;i++)sink.add(new long[]{1000-i,i});
    check(sink.spilledRows()>0,"budget read again after binding");check(sink.peakPayloadBytes()<=1024,"captured arena budget exceeded");
   }finally{System.setProperty(NativeSpillSort.MAX_BYTES_PROPERTY,old);}
  }});
 }
 @Test public void compactionDoesNotChangeInputSpillCounterSemantics()throws Exception{
  long in=NativeSpillSort.SPILL_RUNS.get(),deleted=NativeSpillSort.DELETED_RUNS.get(),rows=NativeSpillSort.SPILLED_ROWS.get();
  long merged=NativeSpillSort.MERGED_RUNS.get(),deletedMerged=NativeSpillSort.DELETED_MERGED_RUNS.get();
  property("256",()->{try(KernelOrderSink sink=new KernelOrderSink(2,new int[]{0},null,null,null,0,-1)){
   for(int n=0;n<1000;n++)sink.add(new long[]{1000-n,n});sink.finish();drain(sink,2,19);
  }});
  eq(1000L,NativeSpillSort.SPILLED_ROWS.get()-rows);
  eq(NativeSpillSort.SPILL_RUNS.get()-in,NativeSpillSort.DELETED_RUNS.get()-deleted);
  check(NativeSpillSort.MERGED_RUNS.get()>merged,"compaction not exercised");
  eq(NativeSpillSort.MERGED_RUNS.get()-merged,NativeSpillSort.DELETED_MERGED_RUNS.get()-deletedMerged);
 }
}
