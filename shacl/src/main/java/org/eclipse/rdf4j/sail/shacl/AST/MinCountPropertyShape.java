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

import java.util.stream.Stream;

/**
 * @author Heshan Jayasinghe
 */
public class MinCountPropertyShape extends PathPropertyShape {

	public int minCount;

	public MinCountPropertyShape(Resource id, SailRepositoryConnection connection, Shape shape) {
		super(id, connection, shape);

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(id, SHACL.MIN_COUNT, null, true))) {
			minCount = stream.map(Statement::getObject).map(v -> (Literal) v).map(Literal::intValue).findAny().get();
		}

	}

	@Override
	public String toString() {
		return "MinCountPropertyShape{" + "minCount=" + minCount + '}';
	}

	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, Shape shape) {

		PlanNode instancesOfTargetClass = shape.getPlan(shaclSailConnection, shape);
		PlanNode properties = super.getPlan(shaclSailConnection, shape);
		PlanNode join = new OuterLeftJoin(instancesOfTargetClass, properties);
		GroupPlanNode groupBy = new GroupBy(join, instancesOfTargetClass.getCardinalityMin());
		return new MinCountValidator(groupBy, minCount);
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
