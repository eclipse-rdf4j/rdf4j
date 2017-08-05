package org.eclipse.rdf4j.plan;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

import java.util.Iterator;

/**
 * Created by heshanjayasinghe on 7/17/17.
 */
public class GroupBy implements PlanNode{
    PlanNode leftjoinnode;

    public GroupBy(PlanNode outerLeftJoin){
        leftjoinnode = outerLeftJoin;
        Iterator<Tuple> aboveIterator = (Iterator<Tuple>) leftjoinnode.iterator();
    }

    @Override
    public CloseableIteration<Tuple, SailException> iterator() {
//        return new Iterator<Tuple>() {
//
//            Iterator<Tuple> aboveIterator = above.iterator();
//
//            Tuple next = null;
//
//            @Override
//            public boolean hasNext() {
//
//                if(next == null){
//                    getNext();
//                }
//
//                return next != null;
//            }
//
//            private void getNext() {
//                while(aboveIterator.hasNext()){
//                    Tuple tempNExt = aboveIterator.next();
//                    if(condition.condition(tempNExt)){
//                        next = tempNExt;
//                        return;
//                    }
//                }
//            }
//
//            @Override
//            public Tuple next() {
//                if(next == null){
//                    getNext();
//                }
//
//                Tuple tempNext = next;
//
//                next = null;
//
//                return tempNext;
//            }
//        };
        return null;
    }

    @Override
    public boolean validate() {
        return false;
    }
}
