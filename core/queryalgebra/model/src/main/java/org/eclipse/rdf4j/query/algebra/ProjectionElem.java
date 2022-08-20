/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.Objects;
import java.util.Optional;

/**
 * Projection elements control which of the selected expressions (produced by the WHERE clause of a query) are returned
 * in the solution, and the order in which they appear.
 * <p>
 * In SPARQL SELECT queries, projection elements are the variables determined by the algorithm for finding SELECT
 * expressions (see <a href="https://www.w3.org/TR/sparql11-query/#sparqlSelectExpressions">SPARQL 1.1 Query Language
 * Recommendation, section 18.2.4.4</a>). Each projection element will be a single variable name (any aliasing is
 * handled by the use of {@link Extension}s).
 * <p>
 * In SPARQL CONSTRUCT queries, the projection elements are used to map the variables obtained from the SELECT
 * expressions to the required statement patterns. In this case, each projection element will have an additional
 * {@link #getProjectionAlias() target name} that maps each projection variable name to one of {@code subject},
 * {@code predicate}, {@code object} or {@code context}.
 * 
 * @author Jeen Broekstra
 */
public class ProjectionElem extends AbstractQueryModelNode {

	private static final long serialVersionUID = -8129811335486478066L;

	private String name;

	private String projectionAlias;

	private boolean aggregateOperatorInExpression;

	private ExtensionElem sourceExpression;

	/**
	 * Create a new empty {@link ProjectionElem}.
	 */
	public ProjectionElem() {
	}

	/**
	 * Create a new {@link ProjectionElem} with a variable name.
	 * 
	 * @param name The name of the projection element (typically the name of the variable in the select expressions).
	 *             May not be <code>null</code>.
	 */
	public ProjectionElem(String name) {
		this(name, null);
	}

	/**
	 * Create a new {@link ProjectionElem} with a variable name and an additional mapped {@link #getProjectionAlias()
	 * target name}
	 * 
	 * @param name       The name of the projection element (typically the name of the variable in the select
	 *                   expressions). May not be <code>null</code>.
	 * @param targetName The name of the variable the projection element value should be mapped to, to produce the
	 *                   projection. Used in CONSTRUCT queries for mapping select expressions to statement patterns. May
	 *                   be <code>null</code>.
	 */
	public ProjectionElem(String name, String targetName) {
		setName(name);
		setProjectionAlias(targetName);
	}

	/**
	 * Get the name of the projection element (typically the name of the variable in the select expressions)
	 * 
	 */
	public String getName() {
		return name;
	}

	/**
	 * @deprecated since 4.1.1. Use {@link #getName()} instead.
	 */
	@Deprecated(since = "4.1.1", forRemoval = true)
	public String getSourceName() {
		return getName();
	}

	/**
	 * @deprecated since 4.1.1. Use {@link #setName(String)} instead.
	 */
	@Deprecated(since = "4.1.1", forRemoval = true)
	public void setSourceName(String sourceName) {
		setName(sourceName);
	}

	/**
	 * Set the name of the projection element (typically the name of the variable in the select expressions)
	 * 
	 * @param name the projection variable name. May not be {@code null}.
	 */
	public void setName(String name) {
		this.name = Objects.requireNonNull(name);
	}

	/**
	 * Get the alias the projection element value should be mapped to. Used in CONSTRUCT queries for mapping select
	 * expressions to statement patterns.
	 * 
	 * @return an optionally empty projection alias.
	 */
	public Optional<String> getProjectionAlias() {
		return Optional.ofNullable(projectionAlias);
	}

	/**
	 * Set the alias the projection element value should be mapped to. Used in CONSTRUCT queries for mapping select
	 * expressions to statement patterns.
	 * 
	 * @param alias the projection alias.
	 */
	public void setProjectionAlias(String alias) {
		this.projectionAlias = alias;
	}

	/**
	 * @deprecated since 4.1.1. Use {@link #setProjectionAlias(String)} instead.
	 */
	@Deprecated(since = "4.1.1", forRemoval = true)
	public void setTargetName(String targetName) {
		setProjectionAlias(targetName);
	}

	/**
	 * @deprecated since 4.1.1. Use {@link #getProjectionAlias()} instead.
	 */
	@Deprecated(since = "4.1.1", forRemoval = true)
	public String getTargetName() {
		return getProjectionAlias().orElse(null);
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		// no-op
	}

	@Override
	public String getSignature() {
		StringBuilder sb = new StringBuilder(32);
		sb.append(super.getSignature());

		sb.append(" \"");
		sb.append(name);
		sb.append("\"");

		if (projectionAlias != null) {
			sb.append(" AS \"").append(projectionAlias).append("\"");
		}

		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ProjectionElem) {
			ProjectionElem o = (ProjectionElem) other;
			return name.equals(o.getName()) && Objects.equals(getProjectionAlias(), o.getProjectionAlias());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, projectionAlias);
	}

	@Override
	public ProjectionElem clone() {
		return (ProjectionElem) super.clone();
	}

	/**
	 * @return Returns the aggregateOperatorInExpression.
	 */
	public boolean hasAggregateOperatorInExpression() {
		return aggregateOperatorInExpression;
	}

	/**
	 * @param aggregateOperatorInExpression The aggregateOperatorInExpression to set.
	 */
	public void setAggregateOperatorInExpression(boolean aggregateOperatorInExpression) {
		this.aggregateOperatorInExpression = aggregateOperatorInExpression;
	}

	/**
	 * @return Returns the sourceExpression.
	 */
	public ExtensionElem getSourceExpression() {
		return sourceExpression;
	}

	/**
	 * @param sourceExpression The sourceExpression to set.
	 */
	public void setSourceExpression(ExtensionElem sourceExpression) {
		this.sourceExpression = sourceExpression;
	}

}
