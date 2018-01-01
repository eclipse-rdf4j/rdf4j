/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.plan.*;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.GroupByCount;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.MergeNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.MinCountFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;

import java.util.stream.Stream;

/**
 * @author Heshan Jayasinghe
 */
public class MinCountPropertyShape extends PathPropertyShape {

	public long minCount;

	public MinCountPropertyShape(Resource id, SailRepositoryConnection connection, Shape shape) {
		super(id, connection, shape);

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(id, SHACL.MIN_COUNT, null, true))) {
			minCount = stream.map(Statement::getObject).map(v -> (Literal) v).map(Literal::longValue).findAny().get();
		}

	}

	@Override
	public String toString() {
		return "MinCountPropertyShape{" + "minCount=" + minCount + '}';
	}

	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, Shape shape) {



		PlanNode planRemovedStatements = new LoggingNode(new TrimTuple(new LoggingNode(super.getPlanRemovedStatements(shaclSailConnection, shape)), 1));

		PlanNode filteredPlanRemovedStatements = planRemovedStatements;

		if(shape instanceof TargetClass){
			filteredPlanRemovedStatements = new LoggingNode(((TargetClass) shape).getTypeFilterPlan(shaclSailConnection, planRemovedStatements));
		}


		PlanNode planAddedStatements = new TrimTuple(new LoggingNode(shape.getPlanAddedStatements(shaclSailConnection, shape)), 1);

		PlanNode mergeNode = new LoggingNode(new MergeNode(planAddedStatements, filteredPlanRemovedStatements));

		PlanNode unique = new LoggingNode(new Unique(mergeNode));

		PlanNode bulkedExternalLeftOuterJoin = new LoggingNode(new BulkedExternalLeftOuterJoin(unique, shaclSailConnection.addedStatements, path.getQuery()));

		PlanNode groupBy = new LoggingNode(new GroupByCount(bulkedExternalLeftOuterJoin));

		PlanNode minCountFilter = new LoggingNode(new MinCountFilter(groupBy, minCount));

		PlanNode trimTuple = new LoggingNode(new TrimTuple(minCountFilter, 1));

		PlanNode bulkedExternalLeftOuterJoin2 = new LoggingNode(new BulkedExternalLeftOuterJoin(trimTuple, shaclSailConnection, path.getQuery()));

		PlanNode groupBy2 = new LoggingNode(new GroupByCount(bulkedExternalLeftOuterJoin2));

		PlanNode minCountFilter2 = new LoggingNode(new MinCountFilter(groupBy2, minCount));


		return minCountFilter2;

	}

	@Override
	public boolean requiresEvalutation(Repository addedStatements, Repository removedStatements) {

		boolean requiresEvalutation = false;
		if(shape instanceof TargetClass){
			Resource targetClass = ((TargetClass) shape).targetClass;
			try (RepositoryConnection addedStatementsConnection = addedStatements.getConnection()) {
				requiresEvalutation = addedStatementsConnection.hasStatement(null, RDF.TYPE, targetClass, false);
			}
		}

		return super.requiresEvalutation(addedStatements, removedStatements) | requiresEvalutation;
	}
}
