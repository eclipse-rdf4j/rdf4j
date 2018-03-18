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
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.BufferedSplitter;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.DirectTupleFromFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.GroupByCount;
import org.eclipse.rdf4j.sail.shacl.planNodes.LeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.MergeNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.MinCountFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;

import java.util.stream.Stream;

/**
 * The AST (Abstract Syntax Tree) node that represents a sh:minCount property shape restriction.
 *
 * @author Heshan Jayasinghe
 */
public class MinCountPropertyShape extends PathPropertyShape {

	private long minCount;

	// toggle for switching on and off the optimization used when no statements have been removed in a transaction
	private boolean optimizeWhenNoStatementsRemoved = true;


	MinCountPropertyShape(Resource id, SailRepositoryConnection connection, Shape shape) {
		super(id, connection, shape);

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(id, SHACL.MIN_COUNT, null, true))) {
			minCount = stream.map(Statement::getObject).map(v -> (Literal) v).map(Literal::longValue).findAny().orElseThrow(() -> new RuntimeException("Expect to find sh:minCount on " + id));
		}

	}

	@Override
	public String toString() {
		return "MinCountPropertyShape{" + "maxCount=" + minCount + '}';
	}

	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, Shape shape) {

		PlanNode topNode;


		if (!optimizeWhenNoStatementsRemoved || shaclSailConnection.stats.hasRemoved()) {
			PlanNode planRemovedStatements = new LoggingNode(new TrimTuple(new LoggingNode(super.getPlanRemovedStatements(shaclSailConnection, shape)), 1));

			PlanNode filteredPlanRemovedStatements = planRemovedStatements;

			if (shape instanceof TargetClass) {
				filteredPlanRemovedStatements = new LoggingNode(((TargetClass) shape).getTypeFilterPlan(shaclSailConnection, planRemovedStatements));
			}

			PlanNode planAddedStatements = new TrimTuple(new LoggingNode(shape.getPlanAddedStatements(shaclSailConnection, shape)), 1);

			PlanNode mergeNode = new LoggingNode(new MergeNode(planAddedStatements, filteredPlanRemovedStatements));

			PlanNode unique = new LoggingNode(new Unique(mergeNode));

			topNode = new LoggingNode(new LeftOuterJoin(unique, super.getPlanAddedStatements(shaclSailConnection, shape)));

			// BulkedExternalLeftOuterJoin is slower, at least when the super.getPlanAddedStatements only returns statements that have the correct type.
			// Persumably BulkedExternalLeftOuterJoin will be high if super.getPlanAddedStatements has a high number of statements for other subjects that in "unique"
			//topNode = new LoggingNode(new BulkedExternalLeftOuterJoin(unique, shaclSailConnection.addedStatements, path.getQuery()));

		} else {

			PlanNode planAddedForShape = new LoggingNode(shape.getPlanAddedStatements(shaclSailConnection, shape));

			PlanNode select = new LoggingNode(new Select(shaclSailConnection.addedStatements, path.getQuery()));

			topNode = new LoggingNode(new LeftOuterJoin(planAddedForShape, select));

		}


		PlanNode groupBy = new LoggingNode(new GroupByCount(topNode));

		DirectTupleFromFilter filteredStatements = new DirectTupleFromFilter();
		new MinCountFilter(groupBy, null, filteredStatements, minCount);

		PlanNode minCountFilter = new LoggingNode(filteredStatements);

		PlanNode trimTuple = new LoggingNode(new TrimTuple(minCountFilter, 1));

		PlanNode bulkedExternalLeftOuterJoin2 = new LoggingNode(new BulkedExternalLeftOuterJoin(trimTuple, shaclSailConnection, path.getQuery()));

		PlanNode groupBy2 = new LoggingNode(new GroupByCount(bulkedExternalLeftOuterJoin2));

		DirectTupleFromFilter filteredStatements2 = new DirectTupleFromFilter();
		new MinCountFilter(groupBy2, null, filteredStatements2, minCount);

		return new LoggingNode(filteredStatements2);

	}

	@Override
	public boolean requiresEvalutation(Repository addedStatements, Repository removedStatements) {

		boolean requiresEvalutation = false;
		if (shape instanceof TargetClass) {
			Resource targetClass = ((TargetClass) shape).targetClass;
			try (RepositoryConnection addedStatementsConnection = addedStatements.getConnection()) {
				requiresEvalutation = addedStatementsConnection.hasStatement(null, RDF.TYPE, targetClass, false);
			}
		} else {
			requiresEvalutation = true;
		}

		return super.requiresEvalutation(addedStatements, removedStatements) | requiresEvalutation;
	}
}
