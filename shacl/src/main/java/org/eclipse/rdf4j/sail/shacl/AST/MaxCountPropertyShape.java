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
import org.eclipse.rdf4j.sail.shacl.plan.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.BulkedExternalLeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.ExternalTypeFilterNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.GroupByCount;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.MaxCountFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.MergeNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.MinCountFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.TrimTuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;

import java.util.stream.Stream;


public class MaxCountPropertyShape extends PathPropertyShape {

	public long maxCount;

	public MaxCountPropertyShape(Resource id, SailRepositoryConnection connection, Shape shape) {
		super(id, connection, shape);

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(id, SHACL.MAX_COUNT, null, true))) {
			maxCount = stream.map(Statement::getObject).map(v -> (Literal) v).map(Literal::longValue).findAny().get();
		}

	}

	@Override
	public String toString() {
		return "MaxCountPropertyShape{" + "maxCount=" + maxCount + '}';
	}

	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, Shape shape) {



		PlanNode planAddedStatements = new LoggingNode(new TrimTuple(shape.getPlanAddedStatements(shaclSailConnection, shape),1));

		PlanNode planAddedStatements1 = new LoggingNode(new TrimTuple(super.getPlanAddedStatements(shaclSailConnection, shape), 1));

		if(shape instanceof TargetClass){
			planAddedStatements1 = new LoggingNode(((TargetClass) shape).getTypeFilterPlan(shaclSailConnection, planAddedStatements1));
		}

		PlanNode mergeNode = new LoggingNode(new MergeNode(planAddedStatements, planAddedStatements1));

		PlanNode unique = new LoggingNode(new Unique(mergeNode));

		PlanNode bulkedExternalLeftOuterJoin = new LoggingNode(new BulkedExternalLeftOuterJoin(unique, shaclSailConnection, path.getQuery()));

		PlanNode groupByCount = new LoggingNode(new GroupByCount(bulkedExternalLeftOuterJoin));

		PlanNode maxCountFilter = new LoggingNode(new MaxCountFilter(groupByCount, maxCount));

		return maxCountFilter;

	}

	@Override
	public boolean requiresEvalutation(Repository addedStatements, Repository removedStatements) {
		return true;
	}
}
