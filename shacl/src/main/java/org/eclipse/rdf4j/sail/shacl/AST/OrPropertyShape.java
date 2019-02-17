/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;


import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.EqualsJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.IteratorData;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Håvard Ottestad
 */
public class OrPropertyShape extends PropertyShape {

	private final List<List<PropertyShape>> or;

	private static final Logger logger = LoggerFactory.getLogger(OrPropertyShape.class);


	OrPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, Resource or) {
		super(id, nodeShape);
		this.or = toList(connection, or).stream().map(v -> PropertyShape.Factory.getPropertyShapesInner(connection, nodeShape, (Resource) v)).collect(Collectors.toList());

	}


	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans, PlanNode overrideTargetNode) {

		List<List<PlanNode>> initialPlanNodes =
			or
				.stream()
				.map(shapes -> shapes.stream().map(shape -> shape.getPlan(shaclSailConnection, nodeShape, false, null)).collect(Collectors.toList()))
				.collect(Collectors.toList());

		BufferedSplitter targetNodesToValidate;
		if(overrideTargetNode == null) {
			targetNodesToValidate = new BufferedSplitter(unionAll(
				initialPlanNodes
					.stream()
					.flatMap(Collection::stream)
					.map(p -> new TrimTuple(p, 0, 1)) // we only want the targets
					.collect(Collectors.toList())));

		}else{
			targetNodesToValidate = new BufferedSplitter(overrideTargetNode);
		}

		List<List<PlanNode>> plannodes =
			or
				.stream()
				.map(shapes -> shapes.stream().map(shape ->
					{
						if(shaclSailConnection.stats.baseSailEmpty){
							return shape.getPlan(shaclSailConnection, nodeShape, false, null);
						}
						return shape.getPlan(shaclSailConnection, nodeShape, false, new LoggingNode(targetNodesToValidate.getPlanNode(), ""));
					}
				).collect(Collectors.toList()))
				.collect(Collectors.toList());

		List<IteratorData> iteratorDataTypes =
			plannodes
				.stream()
				.flatMap(shapes -> shapes.stream().map(PlanNode::getIteratorDataType))
				.distinct().collect(Collectors.toList());


		if (iteratorDataTypes.size() > 1) {
			throw new UnsupportedOperationException("No support for OR shape with mix between aggregate and raw triples");
		}


		IteratorData iteratorData = iteratorDataTypes.get(0);

		if (iteratorData == IteratorData.tripleBased) {

			List<Path> collect = getPaths().stream().distinct().collect(Collectors.toList());
			if (collect.size() > 1) {
				iteratorData = IteratorData.aggregated;
			}
		}

		PlanNode ret;


		if (iteratorData == IteratorData.tripleBased) {

			EqualsJoin equalsJoin = new EqualsJoin(unionAll(plannodes.get(0)), unionAll(plannodes.get(1)), true);

			for (int i = 2; i < or.size(); i++) {
				equalsJoin = new EqualsJoin(equalsJoin, unionAll(plannodes.get(i)), true);
			}

			ret = new LoggingNode(equalsJoin, "");
		} else if (iteratorData == IteratorData.aggregated) {

			PlanNode innerJoin = new LoggingNode(new InnerJoin(unionAll(plannodes.get(0)), unionAll(plannodes.get(1)), null, null), "");

			for (int i = 2; i < or.size(); i++) {
				innerJoin = new LoggingNode(new InnerJoin(innerJoin, unionAll(plannodes.get(i)), null, null), "");
			}

			ret = new LoggingNode(innerJoin, "");
		} else {
			throw new IllegalStateException("Should not get here!");

		}

		if (printPlans) {
			String planAsGraphiz = getPlanAsGraphvizDot(ret, shaclSailConnection);
			logger.info(planAsGraphiz);
		}

		return new EnrichWithShape(ret, this);


	}

	@Override
	public List<Path> getPaths() {
		return or.stream().flatMap(a -> a.stream().flatMap(b -> b.getPaths().stream())).collect(Collectors.toList());
	}

	private PlanNode unionAll(List<PlanNode> planNodes) {
		return new Unique(new UnionNode(planNodes.toArray(new PlanNode[0])));
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements) {
		return super.requiresEvaluation(addedStatements, removedStatements) ||
			or
				.stream()
				.flatMap(Collection::stream)
				.map(p -> p.requiresEvaluation(addedStatements, removedStatements))
				.reduce((a, b) -> a || b)
				.orElse(false);
	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.OrConstraintComponent;
	}
}
