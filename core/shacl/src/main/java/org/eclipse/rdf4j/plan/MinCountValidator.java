package org.eclipse.rdf4j.plan;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

import java.util.List;
import java.util.Map;

/**
 * Created by heshanjayasinghe on 7/20/17.
 */
public class MinCountValidator implements PlanNode{
    PlanNode groupby;
    int minCount;

    public MinCountValidator(PlanNode groupBy,int minCount) {
        this.groupby = groupBy;
        this.minCount = minCount;
    }

    @Override
    public boolean validate() {
        CloseableIteration<Tuple, SailException> groupByIterator =groupby.iterator();
        while (groupByIterator.hasNext()){
            Map.Entry<Value, List<Value>> map = (Map.Entry<Value, List<Value>>) groupByIterator.next();
            if(map.getValue().size()>minCount){
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
