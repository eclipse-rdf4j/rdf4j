/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.AST;


import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.EnrichWithShape;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PatternFilter;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Håvard Ottestad
 */
public class PatternPropertyShape extends PathPropertyShape {

	private final String pattern;
	private final Optional<String> flags;
	private static final Logger logger = LoggerFactory.getLogger(PatternPropertyShape.class);

	PatternPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape) {
		super(id, connection, nodeShape);

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(id, SHACL.PATTERN, null, true))) {
			pattern = stream.map(Statement::getObject).map(Value::stringValue).findAny().orElseThrow(() -> new RuntimeException("Expected to find sh:pattern on " + id));
		}

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(id, SHACL.FLAGS, null, true))) {
			flags = stream.map(Statement::getObject).map(Value::stringValue).findAny();
		}

	}


	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans, boolean assumeBaseSailValid) {

		PlanNode invalidValues =  StandardisedPlanHelper.getGenericSingleObjectPlan(
			shaclSailConnection,
			nodeShape,
			(parent, trueNode, falseNode) -> new PatternFilter(parent, trueNode, falseNode, pattern, flags),
			this
		);

		if (printPlans) {
			String planAsGraphvizDot = getPlanAsGraphvizDot(invalidValues, shaclSailConnection);
			logger.info(planAsGraphvizDot);
		}

		return new EnrichWithShape(invalidValues, this);

	}

	@Override
	public boolean requiresEvaluation(Repository addedStatements, Repository removedStatements) {
		boolean requiresEvalutation = false;
		if (nodeShape instanceof TargetClass) {
			Resource targetClass = ((TargetClass) nodeShape).targetClass;
			try (RepositoryConnection addedStatementsConnection = addedStatements.getConnection()) {
				requiresEvalutation = addedStatementsConnection.hasStatement(null, RDF.TYPE, targetClass, false);
			}
		} else {
			requiresEvalutation = true;
		}

		return super.requiresEvaluation(addedStatements, removedStatements) | requiresEvalutation;	}

	@Override
	public SourceConstraintComponent getSourceConstraintComponent() {
		return SourceConstraintComponent.PatternConstraintComponent;
	}
}
