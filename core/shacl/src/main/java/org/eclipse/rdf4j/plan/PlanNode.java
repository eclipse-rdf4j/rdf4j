package org.eclipse.rdf4j.plan;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Created by heshanjayasinghe on 7/17/17.
 */
public interface PlanNode {
    boolean validate();

    public CloseableIteration<Tuple,SailException> iterator();
}
