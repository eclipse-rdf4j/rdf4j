package org.eclipse.rdf4j.plan;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

import java.util.ArrayList;

/**
 * Created by heshanjayasinghe on 7/15/17.
 */
public class OuterLeftJoin implements PlanNode {
    PlanNode targetclass;
    PlanNode properties;

    public OuterLeftJoin(PlanNode instancesOfTargetClass, PlanNode properties) {
        this.targetclass = instancesOfTargetClass;
        this.properties = properties;
    }


    @Override
    public CloseableIteration<Tuple, SailException> iterator() {
        ArrayList<Tuple> tuplelist = new ArrayList<Tuple>();
        while (targetclass.iterator().hasNext()){
            Tuple targetclassNext = targetclass.iterator().next();
            while (properties.iterator().hasNext()){
                Tuple next = properties.iterator().next();
                if (targetclassNext.line.get(0).stringValue().equals(next.line.get(0).stringValue())){
                    Tuple tuple = new Tuple();
                    tuple.line.add((Value) targetclassNext.getlist().get(0));
                    tuple.line.add((Value) next.getlist().get(0));
                    tuple.line.add((Value) next.getlist().get(2));
                    tuplelist.add(tuple);
                }
            }
        }

        CloseableIteration<Tuple, SailException> leftJointuple = (CloseableIteration<Tuple, SailException>) tuplelist.listIterator();
        return new CloseableIteration<Tuple,SailException>() {

            @Override
            public void close() throws SailException {
                leftJointuple.close();
            }

            int counter = 0;

            @Override
            public boolean hasNext() {
                return leftJointuple.hasNext();
            }

            @Override
            public Tuple next() {

                Tuple tuple = leftJointuple.next();
                counter++;

                return tuple;
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
