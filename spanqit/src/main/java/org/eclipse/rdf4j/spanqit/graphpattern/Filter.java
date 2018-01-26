package org.eclipse.rdf4j.spanqit.graphpattern;

import java.util.Optional;

import org.eclipse.rdf4j.spanqit.constraint.Expression;
import org.eclipse.rdf4j.spanqit.core.QueryElement;
import org.eclipse.rdf4j.spanqit.util.SpanqitUtils;

/**
 * A SPARQL Filter Clause
 * 
 * @see <a href="http://www.w3.org/TR/sparql11-query/#termConstraint"> SPARQL
 *      Filter</a>
 */
class Filter implements QueryElement {
	private static final String FILTER = "FILTER";
	private Optional<Expression<?>> constraint = Optional.empty();

	Filter() {
		this(null);
	}

	Filter(Expression<?> expression) {
		filter(expression);
	}

	/**
	 * Set the constraint for this Filter clause
	 * 
	 * @param expression
	 *            the constraint to set
	 * @return this
	 */
	public Filter filter(Expression<?> expression) {
		constraint = Optional.ofNullable(expression);

		return this;
	}

	@Override
	public String getQueryString() {
		StringBuilder filter = new StringBuilder();

		filter.append(FILTER).append(" ");
		String exp = constraint.map(QueryElement::getQueryString).orElse("");
		filter.append(SpanqitUtils.getParenthesizedString(exp));

		return filter.toString();
	}
}