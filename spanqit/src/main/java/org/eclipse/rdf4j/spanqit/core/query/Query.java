package org.eclipse.rdf4j.spanqit.core.query;

import java.util.Optional;

import org.eclipse.rdf4j.spanqit.constraint.Expression;
import org.eclipse.rdf4j.spanqit.core.GroupBy;
import org.eclipse.rdf4j.spanqit.core.Groupable;
import org.eclipse.rdf4j.spanqit.core.Having;
import org.eclipse.rdf4j.spanqit.core.OrderBy;
import org.eclipse.rdf4j.spanqit.core.Orderable;
import org.eclipse.rdf4j.spanqit.core.QueryElement;
import org.eclipse.rdf4j.spanqit.core.QueryPattern;
import org.eclipse.rdf4j.spanqit.core.Spanqit;
import org.eclipse.rdf4j.spanqit.core.Variable;
import org.eclipse.rdf4j.spanqit.graphpattern.GraphPattern;
import org.eclipse.rdf4j.spanqit.rdf.Rdf;
import org.eclipse.rdf4j.spanqit.rdf.RdfBlankNode;
import org.eclipse.rdf4j.spanqit.util.SpanqitUtils;

/**
 * The base class for all SPARQL Queries. Contains elements and methods common
 * to all queries.
 * 
 * @param <T>
 *            They type of query. Used to support fluency.
 */
@SuppressWarnings("unchecked")
public abstract class Query<T extends Query<T>> implements QueryElement {
	protected static final String LIMIT = "LIMIT";
	protected static final String OFFSET = "OFFSET";

	protected QueryPattern where = Spanqit.where();
	protected Optional<GroupBy> groupBy = Optional.empty();
	protected Optional<OrderBy> orderBy = Optional.empty();
	protected Optional<Having> having = Optional.empty();
	protected int limit = -1, offset = -1, varCount = -1, bnodeCount = -1;

	/**
	 * Add graph patterns to this query's query pattern
	 * 
	 * @param queryPatterns
	 *            the patterns to add
	 * @return this
	 * 
	 * @see QueryPattern
	 */
	public T where(GraphPattern... queryPatterns) {
		where.where(queryPatterns);

		return (T) this;
	}

	/**
	 * Set the query pattern of this query
	 * 
	 * @param where
	 *            the query pattern to set
	 * @return this
	 */
	public T where(QueryPattern where) {
		this.where = where;

		return (T) this;
	}

	/**
	 * Add grouping specifiers for the query results.
	 * 
	 * @param groupables
	 *            the objects to group on, in order (appended to the end of any
	 *            existing grouping specifiers)
	 * @return this
	 * 
	 * @see GroupBy
	 */
	public T groupBy(Groupable... groupables) {
		groupBy = SpanqitUtils.getOrCreateAndModifyOptional(groupBy, Spanqit::groupBy, gb -> gb.by(groupables));

		return (T) this;
	}

	/**
	 * Set this query's Group By clause
	 * 
	 * @param groupBy
	 *            the {@link GroupBy} clause to set
	 * @return this
	 */
	public T groupBy(GroupBy groupBy) {
		this.groupBy = Optional.of(groupBy);

		return (T) this;
	}

	/**
	 * Specify orderings for the query results
	 * 
	 * @param conditions
	 *            the objects to order on, in order
	 * @return this
	 * 
	 * @see OrderBy
	 */
	public T orderBy(Orderable... conditions) {
		orderBy = SpanqitUtils.getOrCreateAndModifyOptional(orderBy, Spanqit::orderBy, ob -> ob.by(conditions));
		
		return (T) this;
	}

	/**
	 * Set this query's Order By clause
	 * 
	 * @param orderBy
	 *            the {@link OrderBy} clause to set
	 * @return this
	 */
	public T orderBy(OrderBy orderBy) {
		this.orderBy = Optional.of(orderBy);

		return (T) this;
	}

	/**
	 * Specify constraints for this query's Having clause.
	 * 
	 * @param constraints
	 *            the constraints to add to the clause
	 * @return this
	 * 
	 * @see Having
	 */
	public T having(Expression<?>... constraints) {
		having = SpanqitUtils.getOrCreateAndModifyOptional(having, Spanqit::having, h -> h.having(constraints));

		return (T) this;
	}

	/**
	 * Set this query's Having clause
	 * 
	 * @param having
	 *            the Having clause to set
	 * @return this
	 */
	public T having(Having having) {
		this.having = Optional.of(having);

		return (T) this;
	}

	/**
	 * Set a limit on the number of results returned by this query.
	 * 
	 * @param limit
	 * @return this
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modResultLimit">
	 *      Limits in SPARQL Queries</a>
	 */
	public T limit(int limit) {
		this.limit = limit;

		return (T) this;
	}

	/**
	 * Specify an offset in query results.
	 * 
	 * @param offset
	 * @return this
	 * 
	 * @see <a
	 *      href="http://www.w3.org/TR/2013/REC-sparql11-query-20130321/#modOffset">Offsets
	 *      in SPARQL Queries</a>
	 */
	public T offset(int offset) {
		this.offset = offset;

		return (T) this;
	}

	/**
	 * A shortcut. Each call to this method returns a new {@link Variable}
	 * that is unique (i.e., has a unique alias) to this query instance.
	 * 
	 * @return a {@link Variable} object that is unique to this query instance
	 */
	public Variable var() {
		return Spanqit.var("x" + ++varCount);
	}

    /**
     * A shortcut. Each call to this method returns a new {@link RdfBlankNode.LabeledBlankNode}
     * that is unique (i.e., has a unique alias) to this query instance.
     *
     * @return a {@link RdfBlankNode.LabeledBlankNode} object that is unique to this query instance
     */
	public RdfBlankNode.LabeledBlankNode bNode() {
	    return Rdf.bNode("b" + ++bnodeCount);
    }

	protected abstract String getQueryActionString();

	@Override
	public String getQueryString() {
		StringBuilder query = new StringBuilder();

		query.append(getQueryActionString()).append("\n");
		query.append(where.getQueryString()).append("\n");

		SpanqitUtils.appendAndNewlineIfPresent(groupBy, query);
		SpanqitUtils.appendAndNewlineIfPresent(having, query);
		SpanqitUtils.appendAndNewlineIfPresent(orderBy, query);

		if (limit >= 0) {
			query.append(LIMIT + " ").append(limit).append("\n");
		}

		if (offset >= 0) {
			query.append(OFFSET + " ").append(offset).append("\n");
		}

		return query.toString();
	}
}