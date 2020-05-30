/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class HasValuePropertyShape extends AbstractSimplePropertyShape {

	private final Value hasValue;
	private static final Logger logger = LoggerFactory.getLogger(HasValuePropertyShape.class);

	HasValuePropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, boolean deactivated,
			PathPropertyShape parent, Resource path,
			Value hasValue) {
		super(id, connection, nodeShape, deactivated, parent, path);

		if (hasValue instanceof BNode) {
			throw new UnsupportedOperationException("sh:hasValue does not currently support blank nodes");
		}

		this.hasValue = hasValue;

	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {

		if (deactivated) {
			return null;
		}
		assert !negateSubPlans : "There are no subplans!";

//		PlanNode invalidValues = getGenericSingleObjectPlan(connectionsGroup, nodeShape,
//			(parent) -> new ValueInFilter(parent, hasValue), this, overrideTargetNode, negateThisPlan);
//
//		if (printPlans) {
//			String planAsGraphvizDot = getPlanAsGraphvizDot(invalidValues, connectionsGroup);
//			logger.info(planAsGraphvizDot);
//		}
//
//		return new EnrichWithShape(invalidValues, this);

		return null;

	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.HasValueConstraintComponent;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		HasValuePropertyShape that = (HasValuePropertyShape) o;
		return hasValue.equals(that.hasValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), hasValue);
	}

	@Override
	public String toString() {
		return "HasValuePropertyShape{" +
				"hasValue=" + hasValue +
				", path=" + getPath() +
				", id=" + id +

				'}';
	}

	public Value getHasValue() {
		return hasValue;
	}

	@Override
	public String buildSparqlValidNodes(String targetVar) {
		String objectVar = "?hasValue_" + UUID.randomUUID().toString().replace("-", "");

		if (hasValue instanceof IRI) {
			return "BIND(<" + hasValue + "> as " + objectVar + ")\n" + getPath().getQuery(targetVar, objectVar, null);
		}
		if (hasValue instanceof Literal) {
			return "BIND(" + hasValue.toString() + " as " + objectVar + ")\n"
					+ getPath().getQuery(targetVar, objectVar, null);
		}

		throw new UnsupportedOperationException(
				"hasValue was unsupported type: " + hasValue.getClass().getSimpleName());

	}

	@Override
	public Stream<StatementPattern> getStatementPatterns() {
		return getPath().getStatementsPatterns(new Var("?this"), new Var(UUID.randomUUID().toString(), hasValue));

	}

}
