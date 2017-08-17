package org.eclipse.rdf4j.shacl;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.Test;

public class OuterleftjoinTest {

    @Test
    public void testOuterleftjoin() {
        SimpleValueFactory simpleValueFactory = SimpleValueFactory.getInstance();

//        String[][] targetclass = { {"Rebecca","foaf:Person"}, {"Håvard","foaf:Person"}, {"Heshan","foaf:Person"}, {"Anna","foaf:Person"},{"Peter","foaf:Person"},{"Håvard","foaf:Agent"},{"Lucy","foaf:Person"}};
//        String[][] properties = { {"Håvard","hå"}, {"Peter","p"}, {"Håvard","hå2"}, {"Peter","p2"},{"Heshan","he"},{"Heshan","he2"},{"Peter","p3"}};
//
//        OuterLeftJoin outerLeftJoin = new OuterLeftJoin(new PlanNode() {
//            @Override
//            public boolean validate() {
//                return false;
//            }
//
//            @Override
//            public CloseableIteration<Tuple, SailException> iterator() {
//                return new CloseableIteration<Tuple, SailException>() {
//                    int counter = 0;
//                    @Override
//                    public void close() throws SailException {
//
//                    }
//
//                    @Override
//                    public boolean hasNext() throws SailException {
//                        return targetclass.length>=counter;
//                    }
//
//                    @Override
//                    public Tuple next() throws SailException {
//                        Tuple tuple = new Tuple();
//                        tuple.line.add(targetclass[counter][0]);
//
//                        counter++ ;
//                        return  tuple;
//                    }
//
//                    @Override
//                    public void remove() throws SailException {
//
//                    }
//
//                };
//            }
//        },new PlanNode() {
//            @Override
//            public boolean validate() {
//                return false;
//            }
//
//            @Override
//            public CloseableIteration<Tuple, SailException> iterator() {
//                return new CloseableIteration<Tuple, SailException>() {
//                    int counter = 0;
//                    @Override
//                    public void close() throws SailException {
//
//                    }
//
//                    @Override
//                    public boolean hasNext() throws SailException {
//                        return targetclass.length>=counter;
//                    }
//
//                    @Override
//                    public Tuple next() throws SailException {
//                        Tuple tuple = new Tuple();
//                        tuple.line.add(targetclass[counter][0]);
//
//                        counter++ ;
//                        return  tuple;
//                    }
//
//                    @Override
//                    public void remove() throws SailException {
//
//                    }
//
//                };
//            }
//        });
//
//        outerLeftJoin.iterator();


    }
}