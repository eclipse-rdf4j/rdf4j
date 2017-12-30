package org.eclipse.rdf4j.sail.shacl.plan;

/**
 * @author Heshan Jayasinghe
 */
public interface PlanNodeCardinality {

	int getCardinalityMin();

	int getCardinalityMax();

}
