package org.eclipse.rdf4j.plan;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by heshanjayasinghe on 7/17/17.
 */
public class GroupBy implements GroupPlanNode{
    PlanNode leftjoinnode;
    HashMap<Value,List<List<Value>>> hashMap = new LinkedHashMap<>();

    public GroupBy(PlanNode outerLeftJoin){
        leftjoinnode = outerLeftJoin;


    }

    @Override
    public CloseableIteration<List<Tuple>, SailException> iterator() {

        CloseableIteration<Tuple, SailException> leftJoinIterator = leftjoinnode.iterator();
        while (leftJoinIterator.hasNext()){
            Tuple leftjointuple = leftJoinIterator.next();
            boolean status = true;
            List<List<Value>> values1 = hashMap.computeIfAbsent(leftjointuple.line.get(0), k -> new ArrayList<List<Value>>());
            values1.add(leftjointuple.line);
//            for( Map.Entry<Value, List<Value>> entry : hashMap.entrySet() )
//            {
//                Value key = entry.getKey();
//                List<Value> values = entry.getValue();
//                if(key.stringValue().equals(leftjointuple.line.get(0))){
//                    values.add(leftjointuple.line.get(2));
//                    hashMap.put(leftjointuple.line.get(0), values);
//                    status = false;
//                }
//            }
//            if(status){
//
//            }

        }


        return new CloseableIteration<List<Tuple>, SailException>()  {
            Iterator<Map.Entry<Value, List<List<Value>>>> hashmapiterator = hashMap.entrySet().iterator();

          //  int counter = 0;
            @Override
            public void close() throws SailException {

            }

            @Override
            public boolean hasNext() throws SailException {
                return hashmapiterator.hasNext();
            }

            @Override
            public List<Tuple> next() throws SailException {
                return hashmapiterator.next().getValue().stream().map(Tuple::new).collect(Collectors.toList());
            }

            @Override
            public void remove() throws SailException {

            }
        };

    }

    @Override
    public boolean validate() {
        return false;
    }
}
