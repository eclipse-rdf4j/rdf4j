/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.ExternalFilterByQuery;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.TupleMapper;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.planNodes.ValueInFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class HasValuePropertyShape extends PathPropertyShape {

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

		// TODO - these plans are generally slow because they generate a lot of SPARQL queries and also they don't
		// optimize for the case when everything already valid in the added statements.

		if (getPath() == null) {
			PlanNode addedTargets = nodeShape.getPlanAddedStatements(connectionsGroup, null);
			if (overrideTargetNode != null) {
				addedTargets = overrideTargetNode.getPlanNode();
			}

			PlanNode invalidTargets = new TupleMapper(addedTargets, t -> {
				List<Value> line = t.getLine();
				t.getLine().add(line.get(0));
				return t;
			});

			if (negateThisPlan) {
				invalidTargets = new ValueInFilter(invalidTargets, new HashSet<>(Collections.singletonList(hasValue)))
						.getTrueNode(UnBufferedPlanNode.class);
			} else {
				invalidTargets = new ValueInFilter(invalidTargets, new HashSet<>(Collections.singletonList(hasValue)))
						.getFalseNode(UnBufferedPlanNode.class);
			}

			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(invalidTargets, connectionsGroup);
				logger.info(planAsGraphvizDot);
			}

			return new EnrichWithShape(invalidTargets, this);

		}

		if (overrideTargetNode != null) {
			PlanNode planNode = overrideTargetNode.getPlanNode();

			ExternalFilterByQuery externalFilterByQuery = new ExternalFilterByQuery(
					connectionsGroup.getBaseConnection(), planNode, 0, buildSparqlValidNodes("?this"), "?this");

			if (negateThisPlan) {
				planNode = externalFilterByQuery.getTrueNode(UnBufferedPlanNode.class);
			} else {
				planNode = externalFilterByQuery.getFalseNode(UnBufferedPlanNode.class);
			}
			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(planNode, connectionsGroup);
				logger.info(planAsGraphvizDot);
			}

			return new EnrichWithShape(planNode, this);
		}

		PlanNode planAddedStatements = nodeShape.getPlanAddedStatements(connectionsGroup, null);

		ExternalFilterByQuery externalFilterByQuery = new ExternalFilterByQuery(connectionsGroup.getBaseConnection(),
				planAddedStatements, 0,
				buildSparqlValidNodes("?this"), "?this");

		PlanNode invalidValues;

		if (negateThisPlan) {
			invalidValues = externalFilterByQuery.getTrueNode(UnBufferedPlanNode.class);
		} else {
			invalidValues = externalFilterByQuery.getFalseNode(UnBufferedPlanNode.class);
		}

		if (negateThisPlan && connectionsGroup.getStats().hasAdded()) {

			PlaneNodeWrapper planeNodeWrapper = planNode -> {
				PlanNode targetFilter = nodeShape.getTargetFilter(connectionsGroup, planNode);
				return new ExternalFilterByQuery(connectionsGroup.getBaseConnection(), targetFilter, 0,
						buildSparqlValidNodes("?this"), "?this").getTrueNode(UnBufferedPlanNode.class);
			};

			invalidValues = new UnionNode(invalidValues,
					getPlanAddedStatements(connectionsGroup, planeNodeWrapper));
		}

		if (!negateThisPlan && connectionsGroup.getStats().hasRemoved()) {

			PlaneNodeWrapper planeNodeWrapper = planNode -> {
				PlanNode targetFilter = nodeShape.getTargetFilter(connectionsGroup, planNode);
				return new ExternalFilterByQuery(connectionsGroup.getBaseConnection(), targetFilter, 0,
						buildSparqlValidNodes("?this"), "?this").getFalseNode(UnBufferedPlanNode.class);
			};

			invalidValues = new UnionNode(invalidValues,
					getPlanRemovedStatements(connectionsGroup, planeNodeWrapper));
		}

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(invalidValues, connectionsGroup);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(invalidValues, this);

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

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, boolean negated) {
		PlanNode plan = nodeShape.getPlanAddedStatements(connectionsGroup, null);
		plan = new UnionNode(plan, nodeShape.getPlanRemovedStatements(connectionsGroup, null));

		Path path = getPath();
		if (path != null) {
			plan = new UnionNode(plan, getPlanAddedStatements(connectionsGroup, null));
			plan = new UnionNode(plan, getPlanRemovedStatements(connectionsGroup, null));
		}

		plan = new Unique(new TrimTuple(plan, 0, 1));

		return nodeShape.getTargetFilter(connectionsGroup, plan);
	}
}
