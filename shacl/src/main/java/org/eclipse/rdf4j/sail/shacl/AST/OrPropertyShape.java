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
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Håvard Ottestad
 */
public class OrPropertyShape extends PathPropertyShape {

	private final List<PropertyShape> or;

	OrPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape) {
		super(id, connection, nodeShape);

		try (Stream<Statement> stream = Iterations.stream(connection.getStatements(id, SHACL.OR, null, true))) {
			Resource orList = stream.map(Statement::getObject).map(v -> (Resource) v).findAny().orElseThrow(() -> new RuntimeException("Expected to find sh:or on " + id));
			or = toList(connection, orList).stream().map(v -> PropertyShape.Factory.getPropertyShapesInner(connection, nodeShape, (Resource) v).get(0)).collect(Collectors.toList());
		}

	}

	private static List<Value> toList(SailRepositoryConnection connection, Resource orList) {
		List<Value> ret = new ArrayList<>();
		while(!orList.equals(RDF.NIL)){
			try (Stream<Statement> stream = Iterations.stream(connection.getStatements(orList, RDF.FIRST, null))) {
				Value value = stream.map(Statement::getObject).findAny().get();
				ret.add(value);
			}

			try (Stream<Statement> stream = Iterations.stream(connection.getStatements(orList, RDF.REST, null))) {
				orList =  stream.map(Statement::getObject).map(v -> (Resource) v).findAny().get();
			}

		}


		return ret;


	}


	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape) {

		EqualsJoin equalsJoin = new EqualsJoin(or.get(0).getPlan(shaclSailConnection, nodeShape), or.get(1).getPlan(shaclSailConnection, nodeShape));

		for (int i = 2; i < or.size(); i++) {
			equalsJoin = new EqualsJoin(equalsJoin, or.get(i).getPlan(shaclSailConnection, nodeShape));
		}

		return new LoggingNode(equalsJoin);

	}

	@Override
	public boolean requiresEvaluation(Repository addedStatements, Repository removedStatements) {
		return true;
	}
}
