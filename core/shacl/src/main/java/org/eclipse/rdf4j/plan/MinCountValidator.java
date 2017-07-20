package org.eclipse.rdf4j.plan;

import java.util.Iterator;

/**
 * Created by heshanjayasinghe on 7/20/17.
 */
public class MinCountValidator implements PlanNode{
    PlanNode count;
    int minCount;

    public MinCountValidator(PlanNode count,int minCount) {
        this.count = count;
        this.minCount = minCount;
    }

    @Override
    public boolean validate() {
        return false;
    }

    @Override
    public Iterator<Tuple> iterator() {
        return null;
    }



}
