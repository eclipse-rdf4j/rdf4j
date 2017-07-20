package org.eclipse.rdf4j.plan;

import java.util.Iterator;

/**
 * Created by heshanjayasinghe on 7/17/17.
 */
public class Count implements PlanNode{
    PlanNode above;

    public Count(PlanNode planNode) {
    }

    @Override
    public boolean validate() {
        return false;
    }

    @Override
    public Iterator<Tuple> iterator() {
        return new Iterator<Tuple>() {

            Iterator<Tuple> aboveIterator = above.iterator();

            Tuple next = null;

            @Override
            public boolean hasNext() {

                if(next == null){
                    getNext();
                }

                return next != null;
            }

            private void getNext() {
                while(aboveIterator.hasNext()){
                    Tuple tempNExt = aboveIterator.next();
                        next = tempNExt;
                }
            }

            @Override
            public Tuple next() {
                if(next == null){
                    getNext();
                }

                Tuple tempNext = next;

                next = null;

                return tempNext;
            }
        };
    }
}
