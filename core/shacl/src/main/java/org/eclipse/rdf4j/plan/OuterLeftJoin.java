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
            while (properties.iterator().hasNext()){
               // int statementlength = ((MemIRI) properties.iterator().next().line.listIterator().next()).subjectStatements.statements.length;
                if (targetclass.iterator().next().line.get(0).stringValue().equals(properties.iterator().next().line.get(0).stringValue())){
                    Tuple tuple = new Tuple();
                    tuple.line.add((Value) targetclass.iterator().next().getlist().get(0));
                    tuple.line.add((Value) properties.iterator().next().getlist().get(0));
                    tuple.line.add((Value) properties.iterator().next().getlist().get(2));
                }


//                targetclass.iterator().next().line;
//                properties.iterator().next();
//                if (targetclass.iterator())
            }
        }
//        while (targetclass.iterator().hasNext()){
//          //  while(properties.iterator().next()) {
//                Tuple targetclasstuple = targetclass.iterator().next();
//                Tuple properytuple = properties.iterator().next();
//                //Tuple propertyTuple = ((Select) properties).shaclSailConnection.sail.
//          //  }
//
//        }
       // CloseableIteration<String[], SailException> leftJointuple = (CloseableIteration<String[], SailException>) list.listIterator();
        return new CloseableIteration<Tuple,SailException>() {

            @Override
            public void close() throws SailException {
                tuplelist.close();
            }

            int counter = 0;

            @Override
            public boolean hasNext() {
                return tuplelist.hasNext();
            }

            @Override
            public Tuple next() {

                Statement next = leftJointuple.next();
                Tuple tuple = new Tuple();
                tuple.line.add(next.getSubject());

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
