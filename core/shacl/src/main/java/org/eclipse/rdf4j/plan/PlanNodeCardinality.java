package org.eclipse.rdf4j.plan;

/**
 * Created by havardottestad on 27/08/2017.
 */
public interface  PlanNodeCardinality {

	int getCardinalityMin();
	int getCardinalityMax();

}
