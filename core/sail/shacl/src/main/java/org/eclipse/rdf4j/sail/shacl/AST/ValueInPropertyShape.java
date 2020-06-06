/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.EmptyNode;
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
public class ValueInPropertyShape extends PathPropertyShape {

	private final List<Value> valueIn;
	private static final Logger logger = LoggerFactory.getLogger(ValueInPropertyShape.class);

	ValueInPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, boolean deactivated,
			PathPropertyShape parent, Resource path,
			Resource valueIn) {
		super(id, connection, nodeShape, true, parent, path);

		this.valueIn = toList(connection, valueIn);

		assert (!this.valueIn.isEmpty());

	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {

		if (deactivated) {
			return null;
		}
		assert !negateSubPlans : "There are no subplans!";

		return new EmptyNode();

	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.ValueInConstraintComponent;
	}

	@Override
	public String buildSparqlValidNodes(String targetVar) {

		return valueIn
				.stream()
				.map(value -> {
					String objectVar = "?valueIn_" + UUID.randomUUID().toString().replace("-", "");

					if (value instanceof IRI) {
						return "BIND(<" + value + "> as " + objectVar + ")\n"
								+ getPath().getQuery(targetVar, objectVar, null);
					}
					if (value instanceof Literal) {
						return "BIND(" + value.toString() + " as " + objectVar + ")\n"
								+ getPath().getQuery(targetVar, objectVar, null);
					}

					throw new UnsupportedOperationException(
							"value was unsupported type: " + value.getClass().getSimpleName());
				})
				.collect(Collectors.joining("} UNION {#VALUES_INJECTION_POINT#", "{#VALUES_INJECTION_POINT#", "}"));

	}

	@Override
	public Stream<StatementPattern> getStatementPatterns() {
		return valueIn
				.stream()
				.flatMap(value -> getPath().getStatementsPatterns(new Var("?this"),
						new Var(UUID.randomUUID().toString(), value)));
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
