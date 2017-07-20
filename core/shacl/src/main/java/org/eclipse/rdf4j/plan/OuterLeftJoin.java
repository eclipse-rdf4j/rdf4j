package org.eclipse.rdf4j.plan;

import com.google.common.collect.Iterators;

import java.util.Iterator;

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
    public Iterator<Tuple> iterator() {
        return new Iterator<Tuple>() {

            int counter = 0;

            @Override
            public boolean hasNext() {

                return Iterators.size((Iterator<?>) properties) > counter;
//                return dataSource.strings.length>counter;
            }

            @Override
            public Tuple next() {
            //   Tuple property =  properties.iterator().next();

                counter++;
                Tuple tuple = new Tuple();
            //    tuple.line.add(property);
                return tuple;
            }
        };
   }

    @Override
    public boolean validate() {
        return false;
    }
}
