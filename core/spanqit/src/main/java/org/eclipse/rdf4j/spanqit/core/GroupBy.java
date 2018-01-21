package org.eclipse.rdf4j.spanqit.core;

import java.util.ArrayList;

/**
 * A SPARQL Group By clause
 * 
 * @see <a
 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#groupby">
 *      SPARQL Group By Clause</a>
 */
public class GroupBy extends StandardQueryElementCollection<Groupable> {
	private static final String GROUP_BY = "GROUP BY";
	private static final String DELIMETER = " ";

	GroupBy() {
		super(GROUP_BY, DELIMETER, new ArrayList<Groupable>());
		printNameIfEmpty(false);
	}

	/**
	 * Add group conditions
	 * 
	 * @param groupables
	 *            the group conditions
	 * @return this
	 */
	public GroupBy by(Groupable... groupables) {
		addElements(groupables);
		
		return this;
	}
}