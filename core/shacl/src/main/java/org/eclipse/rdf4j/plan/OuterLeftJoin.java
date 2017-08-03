package org.eclipse.rdf4j.plan;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
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
        ArrayList<Tuple> list = new ArrayList<Tuple>();
        while (targetclass.iterator().hasNext()){
            Tuple targetclasstuple = targetclass.iterator().next();
            Tuple properytuple = properties.iterator().next();
             //Tuple propertyTuple = ((Select) properties).shaclSailConnection.sail.

        }
       // CloseableIteration<String[], SailException> leftJointuple = (CloseableIteration<String[], SailException>) list.listIterator();
//        return new CloseableIteration<Tuple,SailException>() {
//
//            @Override
//            public void close() throws SailException {
//                leftJointuple.close();
//            }
//
//            int counter = 0;
//
//            @Override
//            public boolean hasNext() {
//                return leftJointuple.hasNext();
//            }
//
//            @Override
//            public Tuple next() {
//
//                Statement next = leftJointuple.next();
//                Tuple tuple = new Tuple();
//                tuple.line.add(next.getSubject());
//
//                counter++;
//
//                return tuple;
//            }
//
//            @Override
//            public void remove() throws SailException {
//
//            }
//        };
        return null;
   }

    @Override
    public boolean validate() {
        return false;
    }
}
