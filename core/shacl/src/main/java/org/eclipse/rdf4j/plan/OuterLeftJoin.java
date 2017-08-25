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
    ArrayList<Tuple> tuplelist =new ArrayList<Tuple>();

    public OuterLeftJoin(PlanNode instancesOfTargetClass, PlanNode properties) {
        this.targetclass = instancesOfTargetClass;
        this.properties = properties;
    }


    @Override
    public CloseableIteration<Tuple, SailException> iterator() {
        CloseableIteration<Tuple, SailException> tagetclassIterator = targetclass.iterator();
         while (tagetclassIterator.hasNext()){
            Tuple targetclassNext = tagetclassIterator.next();
             CloseableIteration<Tuple, SailException> propertiesIterator = properties.iterator();
            if (propertiesIterator.hasNext()) {
                while (propertiesIterator.hasNext()) {
                    Tuple propertiesNext = propertiesIterator.next();
                    if (targetclassNext.line.get(0).stringValue().equals(propertiesNext.line.get(0).stringValue())) {
                        Tuple tuple = new Tuple();
                        tuple.line.addAll(targetclassNext.getlist());
                        tuple.line.addAll(propertiesNext.getlist());
                        tuplelist.add(tuple);
                    }
                }
            }
            else {
                Tuple tuple = new Tuple();
                tuple.line.addAll(targetclassNext.getlist());
                tuplelist.add(tuple);
            }
        }

            return new CloseableIteration<Tuple, SailException>() {
                int counter = 0;

                @Override
                public void close() throws SailException {

                }

                @Override
                public boolean hasNext() throws SailException {
                    return tuplelist.size() > counter;
                }

                @Override
                public Tuple next() throws SailException {
                    Tuple tuple = tuplelist.get(counter);
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
