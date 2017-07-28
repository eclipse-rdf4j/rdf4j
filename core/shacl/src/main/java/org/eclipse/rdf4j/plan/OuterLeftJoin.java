package org.eclipse.rdf4j.plan;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

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
//        return new Iterator<Tuple>() {
//
//            int counter = 0;
//
//            @Override
//            public boolean hasNext() {
//
//                return Iterators.size((Iterator<?>) properties) > counter;
////                return dataSource.strings.length>counter;
//            }
//
//            @Override
//            public Tuple next() {
//            //   Tuple property =  properties.iterator().next();
//
//                counter++;
//                Tuple tuple = new Tuple();
//            //    tuple.line.add(property);
//                return tuple;
//            }
//        };
        return null;
   }

    @Override
    public boolean validate() {
        return false;
    }
}
