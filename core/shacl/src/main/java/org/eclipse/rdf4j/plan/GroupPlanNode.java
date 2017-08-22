package org.eclipse.rdf4j.plan;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

import java.util.List;

/**
 * Created by heshanjayasinghe on 8/22/17.
 */
public interface GroupPlanNode {
    boolean validate();

    public CloseableIteration<List<Tuple>,SailException> iterator();
}
