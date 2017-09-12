package org.eclipse.rdf4j.plan;

/**
 * @author Heshan Jayasinghe
 */
public interface PlanNodeCardinality {

	int getCardinalityMin();

	int getCardinalityMax();

}
