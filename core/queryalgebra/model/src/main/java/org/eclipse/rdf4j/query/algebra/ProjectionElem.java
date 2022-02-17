/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

public class ProjectionElem extends AbstractQueryModelNode {

	/*-----------*
	 * Variables *
	 *-----------*/

	private String sourceName;

	private String targetName;

	private boolean aggregateOperatorInExpression;

	private ExtensionElem sourceExpression;

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ProjectionElem() {
	}

	public ProjectionElem(String name) {
		this(name, name);
	}

	public ProjectionElem(String sourceName, String targetName) {
		setSourceName(sourceName);
		setTargetName(targetName);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public String getSourceName() {
		return sourceName;
	}

	public void setSourceName(String sourceName) {
		assert sourceName != null : "sourceName must not be null";
		this.sourceName = sourceName;
	}

	public String getTargetName() {
		return targetName;
	}

	public void setTargetName(String targetName) {
		assert targetName != null : "targetName must not be null";
		this.targetName = targetName;
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
		sb.append(sourceName);
		sb.append("\"");

		if (!sourceName.equals(targetName)) {
			sb.append(" AS \"").append(targetName).append("\"");
		}

		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ProjectionElem) {
			ProjectionElem o = (ProjectionElem) other;
			return sourceName.equals(o.getSourceName()) && targetName.equals(o.getTargetName());
		}
		return false;
	}

	@Override
	public int hashCode() {
		// Note: don't xor source and target since they will often be equal
		return targetName.hashCode();
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
