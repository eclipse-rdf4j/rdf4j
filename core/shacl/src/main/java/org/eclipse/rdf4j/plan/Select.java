package org.eclipse.rdf4j.plan;

import java.util.Collection;
import java.util.Iterator;

/**
 * Created by heshanjayasinghe on 7/18/17.
 */
public class Select implements PlanNode {
    PlanNode planNode;

    public Select(PlanNode planNode) {
        this.planNode = planNode;
    }


    @Override
    public Iterator<Tuple> iterator() {

        return new Iterator<Tuple>() {

            int counter = 0;

            @Override
            public boolean hasNext() {

               return ((Collection<?>)planNode).size() >counter;
            }

            @Override
            public Tuple next() {

//                String string = dataSource.strings[counter];
//                PlanNode elements = ((Collection<?>)planNode).
               counter++;
                Tuple tuple = new Tuple();
//                tuple.line.add(string);
                return tuple;
            }
        };
    }

    @Override
    public boolean validate() {
        return false;
    }
}
