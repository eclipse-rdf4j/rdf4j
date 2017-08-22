package org.eclipse.rdf4j.plan;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

import java.util.List;

/**
 * Created by heshanjayasinghe on 7/20/17.
 */
public class MinCountValidator implements PlanNode{
    GroupPlanNode groupby;
    int minCount;

    public MinCountValidator(GroupPlanNode groupBy,int minCount) {
        this.groupby = groupBy;
        this.minCount = minCount;
    }

    @Override
    public boolean validate() {
        CloseableIteration<List<Tuple>, SailException> groupByIterator =groupby.iterator();
        while (groupByIterator.hasNext()){
            List<Tuple> tuple = groupByIterator.next();
            if(tuple.size()>minCount){
                return false;
            }
        }
        return true;
    }

    @Override
    public CloseableIteration<Tuple, SailException> iterator() {
        return null;
    }



}
