package org.eclipse.rdf4j.spanqit.core;

import org.eclipse.rdf4j.spanqit.util.SpanqitUtils;

/**
 * An ascending or descending order condition
 * 
 * @see <a
 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOrderBy">
 *      SPARQL Order By Clause</a>
 */
public class OrderCondition implements Orderable {
	private static final String ASC = "ASC";
	private static final String DESC = "DESC";
	private Orderable orderOn;
	private boolean isAscending;

	OrderCondition(Orderable orderOn) {
		this(orderOn, true);
	}

	OrderCondition(Orderable orderOn, boolean ascending) {
		this.orderOn = orderOn;
		if (ascending) {
			asc();
		} else {
			desc();
		}
	}

	/**
	 * Set this order condition to be ascending
	 * 
	 * @return this
	 */
	public OrderCondition asc() {
		this.isAscending = true;

		return this;
	}

	/**
	 * Set this order condition to be descending
	 * 
	 * @return this
	 */
	public OrderCondition desc() {
		this.isAscending = false;

		return this;
	}

	@Override
	public String getQueryString() {
		StringBuilder condition = new StringBuilder();

		if (orderOn != null) {
			if (isAscending) {
				condition.append(ASC);
			} else {
				condition.append(DESC);
			}

			condition.append(SpanqitUtils.getParenthesizedString(orderOn.getQueryString()));
		}

		return condition.toString();
	}
}