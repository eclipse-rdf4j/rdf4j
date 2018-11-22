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
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.EqualsJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.InnerJoin;
import org.eclipse.rdf4j.sail.shacl.planNodes.IteratorData;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Håvard Ottestad
 */
public class OrPropertyShape extends PathPropertyShape {

	private final List<PropertyShape> or;

	private static final Logger logger = LoggerFactory.getLogger(OrPropertyShape.class);


	OrPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape) {
		super(id, connection, nodeShape);

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(id, SHACL.OR, null, true))) {
			Resource orList = stream.map(Statement::getObject).map(v -> (Resource) v).findAny().orElseThrow(() -> new RuntimeException("Expected to find sh:or on " + id));
			or = toList(connection, orList).stream().map(v -> PropertyShape.Factory.getPropertyShapesInner(connection, nodeShape, (Resource) v).get(0)).collect(Collectors.toList());
		}

	}

	private static List<Value> toList(SailRepositoryConnection connection, Resource orList) {
		List<Value> ret = new ArrayList<>();
		while (!orList.equals(RDF.NIL)) {
			try (Stream<Statement> stream = Iterations.stream(connection.getStatements(orList, RDF.FIRST, null))) {
				Value value = stream.map(Statement::getObject).findAny().get();
				ret.add(value);
			}

			try (Stream<Statement> stream = Iterations.stream(connection.getStatements(orList, RDF.REST, null))) {
				orList = stream.map(Statement::getObject).map(v -> (Resource) v).findAny().get();
			}

		}


		return ret;


	}


	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans, boolean assumeBaseSailValid) {

		List<PlanNode> plannodes = or.stream().map(shape -> shape.getPlan(shaclSailConnection, nodeShape, false, false)).collect(Collectors.toList());

		List<IteratorData> iteratorDataTypes = plannodes.stream().map(PlanNode::getIteratorDataType).distinct().collect(Collectors.toList());

		if (iteratorDataTypes.size() > 1) {
			throw new UnsupportedOperationException("No support for OR shape with mix between aggregate and raw triples");
		}


		PlanNode ret = null;


		if (iteratorDataTypes.get(0) == IteratorData.tripleBased) {

			EqualsJoin equalsJoin = new EqualsJoin(plannodes.get(0), plannodes.get(1), true);

			for (int i = 2; i < or.size(); i++) {
				equalsJoin = new EqualsJoin(equalsJoin, plannodes.get(i), true);
			}

			ret =  new LoggingNode(equalsJoin);
		}

		else if (iteratorDataTypes.get(0) == IteratorData.aggregated) {

			PlanNode equalsJoin = new LoggingNode(new InnerJoin(plannodes.get(0), plannodes.get(1), null, null));

			for (int i = 2; i < or.size(); i++) {
				equalsJoin = new LoggingNode(new InnerJoin(equalsJoin, plannodes.get(i), null, null));
			}

			ret =  new LoggingNode(equalsJoin);
		}else{
			throw new IllegalStateException("Should not get here!");

		}

		if(printPlans){
			String planAsGraphiz = getPlanAsGraphvizDot(ret, shaclSailConnection);
			logger.info(planAsGraphiz);
		}

		return ret;


	}

	@Override
	public boolean requiresEvaluation(Repository addedStatements, Repository removedStatements) {
		return true;
	}
}
