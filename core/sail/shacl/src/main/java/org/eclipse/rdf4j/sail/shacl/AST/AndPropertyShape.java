/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.Stats;
import org.eclipse.rdf4j.sail.shacl.planNodes.AggregateIteratorTypeOverride;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.IteratorData;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Håvard Ottestad
 */
public class AndPropertyShape extends PathPropertyShape {

	private final List<List<PathPropertyShape>> and;

	private static final Logger logger = LoggerFactory.getLogger(AndPropertyShape.class);

	AndPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, boolean deactivated,
			PathPropertyShape parent, Resource path, Resource and, ShaclSail shaclSail) {
		super(id, connection, nodeShape, deactivated, parent, path);
		this.and = getPropertyShapes(connection, nodeShape, shaclSail, and);

		if (!this.and.stream().flatMap(Collection::stream).findAny().isPresent()) {
			logger.warn("sh:and contained no supported shapes: " + id);
			this.deactivated = true;
		}

	}

	private List<List<PathPropertyShape>> getPropertyShapes(SailRepositoryConnection connection, NodeShape nodeShape,
			ShaclSail shaclSail,
			Resource and) {
		return toList(connection, and).stream()
				.map(v -> Factory.getPropertyShapesInner(connection, nodeShape, (Resource) v, this, shaclSail)
						.stream()
						.filter(s -> !s.deactivated)
						.collect(Collectors.toList()))
				.collect(Collectors.toList());
	}

	public AndPropertyShape(Resource id, NodeShape nodeShape, boolean deactivated, PathPropertyShape parent, Path path,
			List<List<PathPropertyShape>> and) {
		super(id, nodeShape, deactivated, parent, path);
		this.and = and;

		if (!this.and.stream().flatMap(Collection::stream).findAny().isPresent()) {
			logger.warn("sh:and contained no supported shapes: " + id);
			this.deactivated = true;
		}
	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {

		if (deactivated) {
			return null;
		}

		if (negateThisPlan) { // De Morgan's laws
			OrPropertyShape orPropertyShape = new OrPropertyShape(getId(), nodeShape, deactivated, this, null,
					and);

			EnrichWithShape plan = (EnrichWithShape) orPropertyShape.getPlan(connectionsGroup, false,
					overrideTargetNode, false, true);

			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(plan,
						connectionsGroup);
				logger.info(planAsGraphvizDot);
			}

			return new EnrichWithShape(plan.getParent(), this);

		}

		if (and.stream().mapToLong(List::size).sum() == 1) {
			PlanNode plan = and.get(0)
					.get(0)
					.getPlan(connectionsGroup, false, overrideTargetNode, negateSubPlans, false);
			if (printPlans) {
				String planAsGraphvizDot = getPlanAsGraphvizDot(plan,
						connectionsGroup);
				logger.info(planAsGraphvizDot);
			}
			return new EnrichWithShape(plan, this);
		}

		List<PlanNode> plans = and
				.stream()
				.flatMap(List::stream)
				.map(shape -> shape.getPlan(connectionsGroup, false, overrideTargetNode, negateSubPlans,
						false))
				.collect(Collectors.toList());

		PlanNode unionPlan = unionAll(plans);

		List<IteratorData> iteratorDataTypes = plans.stream()
				.map(PlanNode::getIteratorDataType)
				.distinct()
				.collect(Collectors.toList());

		IteratorData iteratorData = iteratorDataTypes.get(0);

		if (iteratorDataTypes.size() > 1) {
			iteratorData = IteratorData.aggregated;
		}

		if (iteratorData == IteratorData.tripleBased) {

			if (childrenHasOwnPath()) {
				iteratorData = IteratorData.aggregated;
			}

		}

		if (iteratorData == IteratorData.aggregated) {
			unionPlan = new AggregateIteratorTypeOverride(new Unique(new TrimTuple(unionPlan, 0, 1)));
		}

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(unionPlan,
					connectionsGroup);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(unionPlan, this);

	}

	static private PlanNode unionAll(List<PlanNode> planNodes) {
		return new Unique(new UnionNode(planNodes.toArray(new PlanNode[0])));
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements, Stats stats) {
		if (deactivated) {
			return false;
		}

		return super.requiresEvaluation(addedStatements, removedStatements, stats) || and.stream()
				.flatMap(Collection::stream)
				.map(p -> p.requiresEvaluation(addedStatements, removedStatements, stats))
				.reduce((a, b) -> a || b)
				.orElse(false);
	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.AndConstraintComponent;
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
		AndPropertyShape that = (AndPropertyShape) o;
		return and.equals(that.and);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), and);
	}

	@Override
	public String toString() {
		return "AndPropertyShape{" +
				"and=" + toString(and) +
				", id=" + id +
				'}';
	}

	public boolean childrenHasOwnPath() {
		return and
				.stream()
				.flatMap(a -> a
						.stream()
						.map(PathPropertyShape::hasOwnPath))
				.anyMatch(a -> a);
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, boolean negated) {
		Optional<PlanNode> reduce = and
				.stream()
				.flatMap(Collection::stream)
				.map(a -> a.getAllTargetsPlan(connectionsGroup, negated))
				.reduce((a, b) -> new UnionNode(a, b));

		return new Unique(reduce.get());
	}

	@Override
	public String buildSparqlValidNodes(String targetVar) {
		return and.stream()
				.map(propertyShapes -> propertyShapes
						.stream()
						.map(propertyShape -> propertyShape.buildSparqlValidNodes(targetVar))
						.reduce((a, b) -> a + "\n" + b))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.reduce((a, b) -> a + "\n" + b)
				.orElse("");
	}

	@Override
	public Stream<StatementPattern> getStatementPatterns() {
		return and
				.stream()
				.flatMap(Collection::stream)
				.flatMap(PropertyShape::getStatementPatterns);
	}
}
