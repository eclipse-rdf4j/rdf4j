package org.eclipse.rdf4j.plan;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

import java.util.*;

/**
 * Created by heshanjayasinghe on 7/17/17.
 */
public class GroupBy implements PlanNode{
    PlanNode leftjoinnode;

    public GroupBy(PlanNode outerLeftJoin){
        leftjoinnode = outerLeftJoin;


    }

    @Override
    public CloseableIteration<Tuple, SailException> iterator() {

        HashMap<Value,List<Value>> hashMap = new LinkedHashMap<Value,List<Value>>();

        while (leftjoinnode.iterator().hasNext()){
            Tuple leftjointuple = leftjoinnode.iterator().next();
            boolean status = true;

            for( Map.Entry<Value, List<Value>> entry : hashMap.entrySet() )
            {
                Value key = entry.getKey();
                List<Value> values = entry.getValue();
                if(key.stringValue().equals(leftjointuple.line.get(0))){
                    values.add(leftjointuple.line.get(2));
                    hashMap.put(leftjointuple.line.get(0), values);
                    status = false;
                }
            }
            if(status){
                List<Value> element= new ArrayList<Value>();
                element.add(leftjointuple.line.get(2));
                hashMap.put(leftjointuple.line.get(0), element);
            }

        }

        return new CloseableIteration<Tuple, SailException>()  {
            Iterator<Map.Entry<Value, List<Value>>> hashmapiterator = hashMap.entrySet().iterator();

            int counter = 0;
            @Override
            public void close() throws SailException {

            }

            @Override
            public boolean hasNext() throws SailException {
                return hashmapiterator.hasNext();
            }

            @Override
            public Tuple next() throws SailException {
                return (Tuple) hashmapiterator.next();
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
