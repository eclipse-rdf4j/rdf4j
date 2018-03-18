/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedTupleFromFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.DirectTupleFromFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.GroupByCount;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.MaxCountFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;

import java.util.stream.Stream;

/**
 * The AST (Abstract Syntax Tree) node that represents a sh:maxCount property shape restriction.
 *
 * @author Håvard Ottestad
 */
public class MaxCountPropertyShape extends PathPropertyShape {

	private long maxCount;

	MaxCountPropertyShape(Resource id, SailRepositoryConnection connection, Shape shape) {
		super(id, connection, shape);

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(id, SHACL.MAX_COUNT, null, true))) {
			maxCount = stream.map(Statement::getObject).map(v -> (Literal) v).map(Literal::longValue).findAny().orElseThrow(() -> new RuntimeException("Expected to find sh:maxCount on " + id));
		}

	}

	@Override
	public String toString() {
		return "MaxCountPropertyShape{" + "maxCount=" + maxCount + '}';
	}

	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, Shape shape) {


		PlanNode planAddedStatements = new LoggingNode(shape.getPlanAddedStatements(shaclSailConnection, shape));

		PlanNode planAddedStatements1 = new LoggingNode(super.getPlanAddedStatements(shaclSailConnection, shape));

		if (shape instanceof TargetClass) {
			planAddedStatements1 = new LoggingNode(((TargetClass) shape).getTypeFilterPlan(shaclSailConnection, planAddedStatements1));
		}

		PlanNode mergeNode = new LoggingNode(new UnionNode(planAddedStatements, planAddedStatements1));

		PlanNode groupByCount1 = new LoggingNode(new GroupByCount(mergeNode));

		BufferedTupleFromFilter validValues = new BufferedTupleFromFilter();
		BufferedTupleFromFilter invalidValues = new BufferedTupleFromFilter();

		new MaxCountFilter(groupByCount1, validValues, invalidValues, maxCount);

		PlanNode trimmed = new LoggingNode(new TrimTuple(validValues, 1));

		PlanNode unique = new LoggingNode(new Unique(trimmed));

		PlanNode bulkedExternalLeftOuterJoin = new LoggingNode(new BulkedExternalLeftOuterJoin(unique, shaclSailConnection, path.getQuery()));

		PlanNode groupByCount = new LoggingNode(new GroupByCount(bulkedExternalLeftOuterJoin));

		DirectTupleFromFilter directTupleFromFilter = new DirectTupleFromFilter();

		new MaxCountFilter(groupByCount, null, directTupleFromFilter, maxCount);

		PlanNode mergeNode1 = new UnionNode(new LoggingNode(directTupleFromFilter), new LoggingNode(invalidValues));

		return new LoggingNode(mergeNode1);

	}

	@Override
	public boolean requiresEvalutation(Repository addedStatements, Repository removedStatements) {
		return true;
	}
}
