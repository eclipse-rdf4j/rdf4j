package org.eclipse.rdf4j.plan;

/**
 * Created by heshanjayasinghe on 7/17/17.
 */
public interface PlanNode extends Iterable<Tuple> {
    boolean validate();
}
