/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb.evaluation;

import static org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeKernelIr.*;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;
import org.eclipse.rdf4j.sail.lmdb.evaluation.codegen.*;
import org.eclipse.rdf4j.sail.lmdb.factor.*;
import org.junit.jupiter.api.Test;

/** Executed opcode registry. Adding a concrete IR instruction without a parity fixture fails the registry check. */
public class KernelInstructionCoverageTest {
    private static Operand c(int i) {return Operand.constant(i);}
    private static Operand v(int i) {return Operand.col(i);}
    private static Emit out(int... cols) {return new Emit(cols,false,OutputMods.none());}
    private static final long[] CONSTANTS={10,101,20,21,1,0,9,30,-1};
    private static final long[][] QUADS={{10,101,20,1},{10,101,21,2},{11,101,20,1},{20,101,30,0},
            {10,102,21,1},{11,102,30,1}};

    private static final class Graph {
        final Map<Long,long[][]> runs=new HashMap<>();
        final long[][] data;int keyOpens,keyCloses;
        Graph(){this(QUADS);}
        Graph(long[][] data) {
            this.data=data;
            for(long[] q:data) {
                long h=q[1]*100+q[0];
                long[][] a=runs.getOrDefault(h,new long[0][]), b=Arrays.copyOf(a,a.length+1);
                b[a.length]=new long[]{q[2],q[3]};runs.put(h,b);
            }
        }
        long handle(long key,long pred){long h=pred*100+key;return runs.containsKey(h)?h:-1;}
        long size(long h){return runs.get(h).length;}
        long neighbor(long h,long i){return runs.get(h)[Math.toIntExact(i)][0];}
        long context(long h,long i){return runs.get(h)[Math.toIntExact(i)][1];}
        long[] keys(long pred){return Arrays.stream(data).filter(q->q[1]==pred).mapToLong(q->q[0]).distinct().sorted().toArray();}
        NativeLmdbQuerySource.NativeAdjacency adjacency(long pred){
            long[] keys=keys(pred);
            return new NativeLmdbQuerySource.NativeAdjacency(){
                @Override public long find(long key){return handle(key,pred);}
                @Override public long size(long h){return Graph.this.size(h);}
                @Override public long neighborAt(long h,long i){return neighbor(h,i);}
                @Override public long contextAt(long h,long i){return context(h,i);}
                @Override public KeyRunCursor openKeyRunCursor(){
                    KeyRunCursor delegate=NativeLmdbQuerySource.NativeAdjacency.super.openKeyRunCursor();keyOpens++;
                    return new KeyRunCursor(){boolean closed;
                        @Override public boolean advance(){if(closed)throw new AssertionError("closed key cursor read");return delegate.advance();}
                        @Override public long key(){return delegate.key();}
                        @Override public long runSize(){return delegate.runSize();}
                        @Override public long runHandle(){return delegate.runHandle();}
                        @Override public long neighborAt(long i){return delegate.neighborAt(i);}
                        @Override public long contextAt(long i){return delegate.contextAt(i);}
                        @Override public int copyNeighbors(long from,int length,long[] target,int offset){return delegate.copyNeighbors(from,length,target,offset);}
                        @Override public int copyContexts(long from,int length,long[] target,int offset){return delegate.copyContexts(from,length,target,offset);}
                        @Override public void close(){if(closed)throw new AssertionError("key cursor closed twice");closed=true;keyCloses++;delegate.close();}
                    };
                }
                @Override public boolean supportsKeyEnumeration(){return true;}
                @Override public long keyCount(){return keys.length;}
                @Override public long keyAt(long i){return keys[Math.toIntExact(i)];}
                @Override public boolean runsNeighborOrdered(){return true;}
            };
        }
        NativeLmdbQuerySource.DynamicAdjacency dynamic(){
            return new NativeLmdbQuerySource.DynamicAdjacency(){
                @Override public long runFor(long key,long pred){return handle(key,pred);}
                @Override public long size(long h){return Graph.this.size(h);}
                @Override public long neighborAt(long h,long i){return neighbor(h,i);}
                @Override public long contextAt(long h,long i){return context(h,i);}
                @Override public boolean runsNeighborOrdered(){return true;}
            };
        }
        NativeLmdbQuerySource.NodePredicates predicates(){
            return new NativeLmdbQuerySource.NodePredicates(){
                long[] predicates(long key){return Arrays.stream(data).filter(q->q[0]==key).mapToLong(q->q[1]).distinct().sorted().toArray();}
                @Override public long find(long key){return predicates(key).length==0?-1:key;}
                @Override public long rowSize(long row){return predicates(row).length;}
                @Override public int copyRow(long node,long row,long from,int length,long[] pt,int po,long[] rt,int ro){
                    long[] ps=predicates(node);int n=(int)Math.min(length,ps.length-from);
                    for(int i=0;i<n;i++){long p=ps[(int)from+i];pt[po+i]=p;rt[ro+i]=handle(node,p);}return n;
                }
                @Override public long size(long h){return Graph.this.size(h);}
                @Override public long neighborAt(long h,long i){return neighbor(h,i);}
                @Override public long contextAt(long h,long i){return context(h,i);}
                @Override public boolean runsNeighborOrdered(){return true;}
            };
        }
        NativeLmdbQuerySource.WildcardAdjacency wildcard(){
            return new NativeLmdbQuerySource.WildcardAdjacency(){
                int bound;NativeLmdbQuerySource.NativeAdjacency current=adjacency(101);
                @Override public int predicateCount(){return 2;}
                @Override public long predicateAt(int ordinal){return 101+ordinal;}
                @Override public void bind(int ordinal){bound=ordinal;current=adjacency(predicateAt(ordinal));}
                @Override public int boundPredicateOrdinal(){return bound;}
                @Override public long keyCount(){return current.keyCount();}
                @Override public long maximumKey(){return current.keyAt(current.keyCount()-1);}
                @Override public long keyAt(long ordinal){return current.keyAt(ordinal);}
                @Override public int findBatch(long[] k,int ko,int n,long[] r,int ro){return current.findBatch(k,ko,n,r,ro);}
                @Override public NativeLmdbQuerySource.NativeAdjacency.KeyRunCursor openKeyRunCursor(){return current.openKeyRunCursor();}
                @Override public long size(long h){return Graph.this.size(h);}
                @Override public long neighborAt(long h,long i){return neighbor(h,i);}
                @Override public long contextAt(long h,long i){return context(h,i);}
                @Override public boolean runsNeighborOrdered(){return true;}
                @Override public void close(){}
            };
        }
    }
    private static class Hooks implements KernelHooks {
        long[] slots=new long[64],binds=new long[64]; int bindArity;
        final Map<Integer,List<Long>> numeric=new HashMap<>(),distinct=new HashMap<>();
        final List<Long> aggregateRows=new ArrayList<>();long rowValue;
        @Override public void setAggregateInput(int col,long value){rowValue=value;}
        @Override public void accumulateRow(int agg,int group){aggregateRows.add(rowValue);}
        @Override public boolean accumulateDistinct(int agg,int group,long value){List<Long> seen=distinct.computeIfAbsent(agg,k->new ArrayList<>());boolean fresh=!seen.contains(value);seen.add(value);return fresh;}
        @Override public boolean testFilter(int id,long a,long b,long c){return a==10;}
        @Override public long computeBind(int id,long a,long b){return b==-1?a:a+b;}
        @Override public int compareValues(long a,long b){return Long.compare(a,b);}
        @Override public boolean isNumeric(long id){return id!=-1;}
        @Override public double doubleValue(long id){return id;}
        @Override public void accumulateNumeric(int agg,int group,long id){numeric.computeIfAbsent(agg,k->new ArrayList<>()).add(id);}
        @Override public void residualSlot(int slot,long value){slots[slot]=value;}
        @Override public boolean testResidual(int residual){return slots[3]==10;}
        @Override public void setBindInput(int bindId,int slot,long value){if(slot==0)bindArity=0;binds[slot]=value;bindArity=Math.max(bindArity,slot+1);}
        @Override public long computeBindRow(int bindId){long sum=0;for(int i=0;i<bindArity;i++)sum+=binds[i];return sum;}
    }
    private static KernelPlan plan(){
        return new KernelPlan(){
            @Override public Cursor open(){return new Cursor(){
                int pos;
                @Override public int fill(long[] b,int n){int count=Math.min(2-pos,n);for(int i=0;i<count;i++){
                    b[2*i]=10+pos;b[2*i+1]=20+pos++;}return count;}
                @Override public void close(){}
            };}
            @Override public void close(){}
        };
    }
    private static KernelScanner scanner(){return (scan,s,p,o,c)->new KernelQuadCursor(){
        int pos;
        @Override public int fill(long[] b,int n){int count=Math.min(n,QUADS.length-pos);for(int i=0;i<count;i++)
            System.arraycopy(QUADS[pos++],0,b,i*4,4);return count;}
        @Override public void close(){}
    };}
    private static KernelContext context(){return context(new Graph());}
    private static KernelContext context(Graph graph){
        var ctx=new KernelContext(new NativeLmdbQuerySource.NativeAdjacency[]{graph.adjacency(101),graph.adjacency(102)},
                CONSTANTS.clone(),new long[]{10},new long[][]{{10,11,20},{10,10,11,-1},{1,2}},new Hooks(),scanner(),new KernelPlan[]{plan()},16,
                new NativeLmdbQuerySource.NodePredicates[]{graph.predicates()},new NativeLmdbQuerySource.DynamicAdjacency[]{graph.dynamic()},
                new NativeLmdbQuerySource.WildcardAdjacency[]{graph.wildcard()});
        ctx.withNodeDomainIntersections(new NativeLmdbQuerySource.NodeDomainIntersection[]{new NativeLmdbQuerySource.NodeDomainIntersection(){
            @Override public long estimatedWork(){return 2;}
            @Override public boolean contains(long id){return id==10||id==11;}
            @Override public int partitionCount(){return 2;}
            @Override public long countPartition(int p){return 1;}
            @Override public Cursor cursor(int p){return new Cursor(){boolean read;@Override public boolean next(){if(read)return false;return read=true;}
                @Override public long nodeId(){return 10+p;}};}
        }});
        return ctx;
    }
    private record Case(String name,Supplier<Kernel> shape,Supplier<KernelContext> context,long[][] expected){}
    private static long[][] tuples(long[]... rows){return rows;}
    private static long[] row(long... values){return values;}
    private static Case test(String name,List<Node> nodes,int[] output,long[][] expected){
        return new Case(name,()->new Kernel(6,nodes,out(output)),KernelInstructionCoverageTest::context,expected);
    }
    private static List<Node> afterDomain(Node... nodes){var list=new ArrayList<Node>();list.add(new EnumerateDomain(0,0));list.addAll(List.of(nodes));return list;}
    private static long[][] edges(boolean predicates,boolean contexts){
        return Arrays.stream(QUADS).filter(q->predicates||q[1]==101).map(q->contexts?q.clone():new long[]{q[0],q[2]}).toArray(long[][]::new);
    }
    private static List<Case> cases(){
        var a=new ArrayList<Case>();
        a.add(test("entry",List.of(new EnumerateEntry(),new BindAlias(Operand.entry(0),0)),new int[]{0},tuples(row(10))));
        a.add(test("domain",List.of(new EnumerateDomain(0,0)),new int[]{0},tuples(row(10),row(11),row(20))));
        a.add(test("intersection-domain",List.of(new EnumerateNodeDomainIntersection(0,0)),new int[]{0},tuples(row(10),row(11))));
        a.add(test("keys-only",List.of(new EnumerateAdjKeys(0,0,-1)),new int[]{0},tuples(row(10),row(11),row(20))));
        a.add(test("wildcard-keys-only",List.of(new EnumerateAdjKeys(0,true,c(1),0,-1,-1,null,false,false,null)),new int[]{0},tuples(row(10),row(11),row(20))));
        a.add(test("adjacency",List.of(new EnumerateAdjKeys(0,0,1)),new int[]{0,1},edges(false,false)));
        a.add(test("plan",List.of(new PlanRows(0,new int[]{0,1})),new int[]{0,1},tuples(row(10,20),row(11,21))));
        a.add(test("quads",List.of(new ScanQuad(0,new Operand[4],new int[]{0,1,2,3})),new int[]{0,1,2,3},QUADS));
        a.add(test("terms",List.of(new EnumerateTerms(0,0)),new int[]{0},tuples(row(10),row(20),row(21),row(11),row(30))));
        a.add(test("sip-domain",List.of(new SipDomainProbe(0,0,0,1,-1,null,false)),new int[]{0,1},edges(false,false)));
        a.add(test("sip-keys",List.of(new SipKeyProbe(1,0,0,1,-1,null,false)),new int[]{0,1},tuples(row(10,20),row(10,21),row(11,20))));
        a.add(test("sip-domain-wildcard",List.of(new SipDomainWildcard(0,0,true,LmdbWildcardPhysicalDemand.Demand.PAYLOAD,0,1,2,3,null,false)),new int[]{0,1,2,3},QUADS));
        a.add(test("sip-keys-wildcard",List.of(new SipKeyWildcard(0,0,true,LmdbWildcardPhysicalDemand.Demand.PAYLOAD,0,1,2,3,null,false)),new int[]{0,1,2,3},QUADS));
        a.add(test("probe",afterDomain(new Probe(0,v(0),1)),new int[]{0,1},edges(false,false)));
        a.add(test("node-predicates",afterDomain(new EnumeratePredicates(0,v(0),1,2,3,null,false)),new int[]{0,1,2,3},QUADS));
        a.add(test("wildcard-predicates",afterDomain(new EnumeratePredicates(0,true,v(0),1,2,3,null,false)),new int[]{0,1,2,3},QUADS));
        a.add(test("wildcard",List.of(new EnumerateWildcard(0,true,LmdbWildcardPhysicalDemand.Demand.PAYLOAD,0,1,2,3,null,false)),new int[]{0,1,2,3},QUADS));
        a.add(test("variable-probe",afterDomain(new ProbeVariable(0,v(0),c(1),1)),new int[]{0,1},edges(false,false)));
        a.add(test("close-probe",afterDomain(new ProbeClose(0,v(0),c(2),true,true)),new int[]{0},tuples(row(10),row(11))));
        a.add(test("intersect",afterDomain(new Intersect(new int[]{0,1},new Operand[]{v(0),v(0)},1)),new int[]{0,1},tuples(row(10,21))));
        a.add(test("residual",afterDomain(new FilterResidual(0,new Operand[]{v(0)},new int[]{3})),new int[]{0},tuples(row(10))));
        a.add(test("compare",afterDomain(new FilterCompareId(false,v(0),c(0))),new int[]{0},tuples(row(10))));
        a.add(test("compatible",List.of(new EnumerateDomain(1,0),new FilterEntryCompatible(v(0),0)),new int[]{0},tuples(row(10),row(10),row(-1))));
        a.add(test("in",afterDomain(new FilterInConstants(v(0),new int[]{0,2})),new int[]{0},tuples(row(10),row(20))));
        a.add(test("range",afterDomain(new FilterRangeUnsigned(v(0),0,2)),new int[]{0},tuples(row(10),row(11),row(20))));
        a.add(test("date",afterDomain(new FilterDateCompare(0,v(0),0,OP_EQ,false,true)),new int[]{0},tuples(row(10))));
        a.add(test("fragment",afterDomain(new FilterFragmentCompare(0,v(0),0,OP_EQ,false)),new int[]{0},tuples(row(10))));
        a.add(test("value",afterDomain(new FilterValue(0,new Operand[]{v(0)})),new int[]{0},tuples(row(10))));
        a.add(test("left-probe",afterDomain(new LeftProbe(1,v(0),1)),new int[]{0,1},tuples(row(10,21),row(11,30),row(20,-1))));
        a.add(test("exists",afterDomain(new Exists(false,List.of(new Probe(1,v(0),1)))),new int[]{0},tuples(row(10),row(11))));
        a.add(test("hash",List.of(new HashBuild(0,new int[]{4},new int[]{5},List.of(new EnumerateAdjKeys(0,4,5)),100),
                new EnumerateDomain(0,0),new HashProbe(0,new Operand[]{v(0)},new int[]{1})),new int[]{0,1},edges(false,false)));
        a.add(test("union",List.of(new Union(List.of(afterDomain(new FilterCompareId(false,v(0),c(0))),afterDomain(new FilterCompareId(false,v(0),c(2)))))),new int[]{0},tuples(row(10),row(20))));
        a.add(test("left-group",afterDomain(new LeftGroup(List.of(new Probe(1,v(0),1)))),new int[]{0,1},tuples(row(10,21),row(11,30),row(20,-1))));
        a.add(test("lexical",List.of(new BindAlias(Operand.entry(0),0),new LexicalFrameLeftJoin(List.of(new EnumerateDomain(0,1)),List.of(new BindAlias(Operand.entry(0),0)),new int[]{0})),
                new int[]{0,1},tuples(row(10,10),row(10,11),row(10,20))));
        a.add(test("alias",afterDomain(new BindAlias(v(0),1)),new int[]{0,1},tuples(row(10,10),row(11,11),row(20,20))));
        a.add(test("bind",afterDomain(new BindHook(0,new Operand[]{v(0),c(2)},1)),new int[]{1},tuples(row(30),row(31),row(40))));
        a.add(test("path",List.of(new PathExpand(0,c(0),0,0)),new int[]{0},tuples(row(10),row(20),row(21),row(30))));
        // Real grouped relation with no scalar replay; opens from a fresh source in each tier.
        a.add(new Case("factors",()->new Kernel(2,List.of(new PlanFactors(0,new int[]{0,1},new Operand[0],new int[]{0})),
                new Aggregate(new int[0],new AggregateOutput[]{AggregateOutput.countStar()},null,OutputMods.none())),()->{
            HeapFactorSource source=new HeapFactorSource(new Object());BorrowedFactorBatch batch=new BorrowedFactorBatch(source,1);
            batch.reset(1);batch.bindHeap(0,new long[]{20,21},0,2);FactorEnvironment env=new FactorEnvironment(2);env.bind(1,batch,0,1L);
            KernelPlan groups=new KernelPlan(){
                @Override public Cursor open(){throw new AssertionError("factor relation flattened");}
                @Override public FactorCursor openFactors(){return new FactorCursor(){boolean read;
                    @Override public boolean next(){if(read)return false;return read=true;}
                    @Override public int slot(int out){return out;}
                    @Override public long scalar(int out){if(out!=0)throw new AssertionError("deferred scalar read");return 10;}
                    @Override public long multiplicity(){return 3;}
                    @Override public FactorEnvironment factors(){return env;}
                    @Override public void close(){}
                };}
                @Override public void close(){source.close();}
            };
            return new KernelContext(null,new long[0],new long[0],null,new Hooks(),null,new KernelPlan[]{groups},16);
        },tuples(row(6))));
        return a;
    }
    private static List<String> bag(long[][] rows){List<String>b=new ArrayList<>();for(long[]r:rows)b.add(Arrays.toString(r));Collections.sort(b);return b;}
    private static List<String> run(Kernel ir,KernelContext ctx,boolean compiled,int batch) throws Exception {
        JaninoKernel kernel=compiled?KernelCompilationTestSupport.compile(ir):ir.terminal instanceof Emit?
                LmdbNativeKernelInterpreter.forRows(ir):LmdbNativeKernelInterpreter.forAggregate(ir);
        if(kernel==null)throw new AssertionError("tier declined "+ir.shapeKey());
        try(AutoCloseable close=kernel::close){
            kernel.bind(ctx);List<String>rows=new ArrayList<>();long[]b=new long[batch*ir.stride()];int n;
            for(int calls=0;;calls++){
                if(calls>10000)throw new AssertionError("nonterminating kernel");n=kernel.fill(b,batch);if(n==0)break;
                if(n<0||n>batch)throw new AssertionError("invalid fill count");
                for(int i=0;i<n;i++)rows.add(Arrays.toString(Arrays.copyOfRange(b,i*ir.stride(),(i+1)*ir.stride())));
            }
            Collections.sort(rows);return rows;
        }
    }
    // Independent walker: the registry must also run against the original emitter/interpreter sources.
    private static void visitFixture(List<Node> nodes,java.util.function.Consumer<Node> visitor){
        for(Node node:nodes){
            visitor.accept(node);
            if(node instanceof Exists n)visitFixture(n.pipeline,visitor);
            else if(node instanceof HashBuild n)visitFixture(n.pipeline,visitor);
            else if(node instanceof LeftGroup n)visitFixture(n.arm,visitor);
            else if(node instanceof LexicalFrameLeftJoin n){visitFixture(n.left,visitor);visitFixture(n.right,visitor);}
            else if(node instanceof Union n)for(List<Node> arm:n.branches)visitFixture(arm,visitor);
        }
    }
    @Test public void everyConcreteInstructionHasAnExecutedFixture() throws Exception { exerciseCases(cases()); }
    private static void exerciseCases(List<Case> inputs) throws Exception {
        Set<Class<?>> covered=new TreeSet<>(Comparator.comparing(Class::getName));
        List<String> failures=new ArrayList<>();int executions=0;
        for(Case c:inputs) {
            Kernel ir=c.shape.get(); visitFixture(ir.pipeline,node->covered.add(node.getClass()));
            try {
                for(int batch:new int[]{1,7})for(boolean compiled:new boolean[]{false,true}) {
                    List<String> actual=run(ir,c.context.get(),compiled,batch);executions++;
                    if(!bag(c.expected).equals(actual))throw new AssertionError(c.name+" "+(compiled?"compiled":"interpreted")+" "+actual+" != "+bag(c.expected));
                }
                if(ir.terminal instanceof Emit){
                    Kernel count=new Kernel(ir.columnCount,ir.pipeline,new Aggregate(new int[0],new AggregateOutput[]{AggregateOutput.countStar()},null,OutputMods.none()),ir.telemetryMode);
                    for(boolean compiled:new boolean[]{false,true}){
                        List<String> actual=run(count,c.context.get(),compiled,1);executions++;
                        if(!bag(tuples(row(c.expected.length))).equals(actual))throw new AssertionError(c.name+" COUNT "+actual);
                    }
                }
                System.out.println("COVERED "+c.name);
            }catch(Throwable t){failures.add(c.name+": "+t);t.printStackTrace(System.out);}
        }
        Set<Class<?>> declared=new TreeSet<>(Comparator.comparing(Class::getName));
        for(Class<?> type:LmdbNativeKernelIr.class.getDeclaredClasses())if(Node.class.isAssignableFrom(type)&&!Modifier.isAbstract(type.getModifiers()))declared.add(type);
        if(!declared.equals(covered)){declared.removeAll(covered);throw new AssertionError("missing instruction fixtures: "+declared);}
        System.out.println("INSTRUCTION_FAMILIES="+covered.size()+" CASES="+inputs.size()+" EXECUTIONS="+executions);
        if(!failures.isEmpty())throw new AssertionError(String.join("\n",failures));
    }

    @Test public void keyAndWildcardWitnessesReleaseEveryActivation() throws Exception {
        List<Node> producers=List.of(new EnumerateAdjKeys(0,0,1),new EnumerateAdjKeys(0,0,-1),
            new SipKeyProbe(1,0,0,1,-1,null,false),
            new SipKeyWildcard(0,0,true,LmdbWildcardPhysicalDemand.Demand.PAYLOAD,0,1,2,3,null,false),
            new EnumerateWildcard(0,true,LmdbWildcardPhysicalDemand.Demand.PAYLOAD,0,1,2,3,null,false));
        List<String> errors=new ArrayList<>();
        for(Node node:producers)for(boolean compiled:new boolean[]{false,true}){
            Graph graph=new Graph();Kernel ir=new Kernel(6,List.of(new EnumerateDomain(0,5),new Exists(false,List.of(node))),out(5));
            JaninoKernel k=compiled?KernelCompilationTestSupport.compile(ir):LmdbNativeKernelInterpreter.forRows(ir);
            try(AutoCloseable owner=k::close){
                k.bind(context(graph));long[] row=new long[1];int rows=0;while(k.fill(row,1)>0)rows++;
                if(rows!=3||graph.keyOpens!=3||graph.keyCloses!=3)errors.add(node.getClass().getSimpleName()+" compiled="+compiled+" rows="+rows+" opens="+graph.keyOpens+" closes="+graph.keyCloses);
            }
        }
        if(!errors.isEmpty())throw new AssertionError(errors.toString());
    }

    @Test public void wildcardCompilationIsDefaultOnButExplicitDisableRemainsAvailable() {
        String property=WILDCARD_PREDICATES_PROPERTY,old=System.getProperty(property);
        try{
            System.clearProperty(property);if(!wildcardPredicatesEnabled())throw new AssertionError("supported wildcard IR excluded from compiler");
            System.setProperty(property,"false");if(wildcardPredicatesEnabled())throw new AssertionError("explicit compiler disable ignored");
        }finally{if(old==null)System.clearProperty(property);else System.setProperty(property,old);}
    }

    @Test public void typeMatrixUsageCompilesAndMatchesInterpreterAtBothKeyPositions() throws Exception {
        for(int position:new int[]{0,1})for(int width:new int[]{1,2})for(int batch:new int[]{1,7}){
            Kernel ir=new Kernel(0,List.of(),new TypeMatrixAggregate(0,false,position,width));
            long[][] expected=new long[2][2+width];expected[0][position]=20;expected[1][position]=21;
            expected[0][1-position]=102;expected[1][1-position]=102;
            Arrays.fill(expected[0],2,2+width,2);Arrays.fill(expected[1],2,2+width,1);
            for(boolean compiled:new boolean[]{false,true}){
                Graph graph=new Graph();KernelContext context=context(graph);
                context.withTypeMatrices(new TypeMatrixContext[]{new TypeMatrixContext(graph.adjacency(101),null,null,
                    new NativeLmdbQuerySource.NativeAdjacency[]{graph.adjacency(102)},new long[]{102})});
                List<String> actual=run(ir,context,compiled,batch);
                if(!bag(expected).equals(actual))throw new AssertionError("type matrix "+actual+" != "+bag(expected));
                if(graph.keyOpens!=graph.keyCloses)throw new AssertionError("type-matrix cursor leak");
            }
        }
    }

    @Test public void disablingStreamingAndVectorTailRetainsTheSameInstructionCoverage() throws Exception {
        String resumable=System.getProperty(RESUMABLE_PROPERTY),vector=System.getProperty(VECTOR_TAIL_PROPERTY);
        try{
            System.setProperty(RESUMABLE_PROPERTY,"false");System.setProperty(VECTOR_TAIL_PROPERTY,"false");
            everyConcreteInstructionHasAnExecutedFixture();
        }finally{
            if(resumable==null)System.clearProperty(RESUMABLE_PROPERTY);else System.setProperty(RESUMABLE_PROPERTY,resumable);
            if(vector==null)System.clearProperty(VECTOR_TAIL_PROPERTY);else System.setProperty(VECTOR_TAIL_PROPERTY,vector);
        }
    }

    @Test public void telemetryEnabledKeepsAllInstructionsCompilableAndEquivalent() throws Exception {
        exerciseCases(cases().stream().map(c->new Case(c.name,()->c.shape.get().withTelemetry(Kernel.TelemetryMode.FULL),c.context,c.expected)).toList());
    }

    @Test public void everyAggregateKindAndDistinctHookModeMatchesItsOracle() throws Exception {
        AggregateOutput[] outputs={AggregateOutput.countStar(),AggregateOutput.count(0),AggregateOutput.countDistinct(0),
            AggregateOutput.sum(0),AggregateOutput.min(0),AggregateOutput.max(0),AggregateOutput.avg(0),
            AggregateOutput.minId(0),AggregateOutput.maxId(0),AggregateOutput.sumDistinct(0),AggregateOutput.avgDistinct(0),AggregateOutput.rowState(new int[]{0})};
        Set<Integer> kinds=new TreeSet<>();for(AggregateOutput out:outputs)kinds.add(out.kind);
        Set<Integer> declared=new TreeSet<>();for(var field:LmdbNativeKernelIr.class.getDeclaredFields())if(field.getName().startsWith("AGG_")){field.setAccessible(true);declared.add(field.getInt(null));}
        if(!declared.equals(kinds))throw new AssertionError("aggregate fixture registry mismatch");
        long[][] expected=tuples(row(4,3,2,0,Double.doubleToLongBits(10),Double.doubleToLongBits(11),0,10,11,0,0,0));
        Kernel ir=new Kernel(1,List.of(new EnumerateDomain(1,0)),new Aggregate(new int[0],outputs,null,OutputMods.none()));
        for(boolean compiled:new boolean[]{false,true}){
            KernelContext ctx=context();Hooks hooks=(Hooks)ctx.hooks;
            if(!bag(expected).equals(run(ir,ctx,compiled,1)))throw new AssertionError("aggregate outputs");
            for(int kind:new int[]{3,6,9,10})if(!hooks.numeric.get(kind).equals(kind<9?List.of(10L,10L,11L):List.of(10L,11L)))throw new AssertionError("numeric sidecar "+kind);
            if(!hooks.aggregateRows.equals(List.of(10L,10L,11L,-1L)))throw new AssertionError("row sidecar");
        }
        for(AggregateOutput output:List.of(AggregateOutput.countDistinct(0).asHookDistinct(),AggregateOutput.sumDistinct(0).asHookDistinct(),AggregateOutput.avgDistinct(0).asHookDistinct())){
            Kernel hooked=new Kernel(1,List.of(new EnumerateDomain(1,0)),new Aggregate(new int[0],new AggregateOutput[]{output},null,OutputMods.none()));
            for(boolean compiled:new boolean[]{false,true}){
                KernelContext ctx=context();Hooks hooks=(Hooks)ctx.hooks;
                if(!bag(tuples(row(0))).equals(run(hooked,ctx,compiled,1))||!hooks.distinct.get(0).equals(List.of(10L,10L,11L)))throw new AssertionError("hook DISTINCT");
            }
        }
        Kernel ordered=new Kernel(1,List.of(new EnumerateDomain(1,0)),new Aggregate(new int[0],new AggregateOutput[]{AggregateOutput.countDistinctOrdered(0,1)},null,OutputMods.none()));
        for(boolean compiled:new boolean[]{false,true})if(!bag(tuples(row(2))).equals(run(ordered,context(),compiled,1)))throw new AssertionError("ordered DISTINCT");
        System.out.println("AGGREGATE_KINDS="+kinds.size()+" HOOK_DISTINCT_VARIANTS=3");
    }

    @Test public void typeMatrixLinkagePreservesContextMultiplicities() throws Exception {
        long subject=org.eclipse.rdf4j.sail.lmdb.ValueIds.createId(org.eclipse.rdf4j.sail.lmdb.ValueIds.T_URI,1);
        long target=org.eclipse.rdf4j.sail.lmdb.ValueIds.createId(org.eclipse.rdf4j.sail.lmdb.ValueIds.T_URI,2);
        for(int position:new int[]{0,1})for(boolean compiled:new boolean[]{false,true}){
            Graph graph=new Graph(tuples(row(subject,101,501,1),row(subject,101,501,2),row(subject,102,target,1),row(subject,102,target,2),row(target,101,601,1)));
            KernelContext ctx=context(graph);ctx.withTypeMatrices(new TypeMatrixContext[]{new TypeMatrixContext(graph.adjacency(101),graph.adjacency(101),null,
                new NativeLmdbQuerySource.NativeAdjacency[]{graph.adjacency(102)},new long[]{102})});
            Kernel ir=new Kernel(0,List.of(),new TypeMatrixAggregate(0,true,position,1));
            long[] expected=position==0?row(501,601,4):row(601,501,4);
            if(!bag(tuples(expected)).equals(run(ir,ctx,compiled,1)))throw new AssertionError("type linkage");
            if(graph.keyOpens!=graph.keyCloses)throw new AssertionError("type linkage leak");
        }
        Set<Class<?>> terminals=new HashSet<>();
        for(Class<?> type:LmdbNativeKernelIr.class.getDeclaredClasses())if(Terminal.class.isAssignableFrom(type)&&!Modifier.isAbstract(type.getModifiers()))terminals.add(type);
        if(!terminals.equals(Set.of(Emit.class,Aggregate.class,TypeMatrixAggregate.class)))throw new AssertionError("new terminal requires fixture: "+terminals);
    }
}
