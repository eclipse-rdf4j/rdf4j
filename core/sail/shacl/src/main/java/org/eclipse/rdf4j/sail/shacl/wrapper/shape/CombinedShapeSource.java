/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Distribution License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.wrapper.shape;

import java.util.Arrays;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;

public class CombinedShapeSource implements ShapeSource {

	private final ForwardChainingShapeSource rdf4jShapesGraph;
	private final BackwardChainingShapeSource baseSail;
	private final Resource[] context;

	public CombinedShapeSource(RepositoryConnection shapesForForwardChainingConnection,
			SailConnection sailConnection) {
		this(shapesForForwardChainingConnection, sailConnection, null);
	}

	private CombinedShapeSource(RepositoryConnection shapesForForwardChainingConnection,
			SailConnection sailConnection,
			Resource[] context) {
		this.rdf4jShapesGraph = new ForwardChainingShapeSource(shapesForForwardChainingConnection);
		this.baseSail = new BackwardChainingShapeSource(sailConnection);
		this.context = context;
	}

	private CombinedShapeSource(ForwardChainingShapeSource rdf4jShapesGraph, BackwardChainingShapeSource baseSail,
			Resource[] context) {
		this.rdf4jShapesGraph = rdf4jShapesGraph;
		this.baseSail = baseSail;
		this.context = context;
	}

	public ShapeSource withContext(Resource[] context) {
		boolean contextContainRdf4jShapeGraph = false;
		boolean contextContainsOtherShapesGraph = false;
		for (Resource resource : context) {
			if (RDF4J.SHACL_SHAPE_GRAPH.equals(resource)) {
				contextContainRdf4jShapeGraph = true;
			} else {
				contextContainsOtherShapesGraph = true;
			}
		}

		if (contextContainRdf4jShapeGraph && !contextContainsOtherShapesGraph) {
			return rdf4jShapesGraph.withContext(context);
		} else if (!contextContainRdf4jShapeGraph && contextContainsOtherShapesGraph) {
			return baseSail.withContext(context);
		} else {
			Resource[] context1 = { RDF4J.SHACL_SHAPE_GRAPH };
			Resource[] context2 = Arrays.stream(context)
					.filter(c -> !RDF4J.SHACL_SHAPE_GRAPH.equals(c))
					.toArray(Resource[]::new);
			return new CombinedShapeSource(rdf4jShapesGraph.withContext(context1), baseSail.withContext(context2),
					context);
		}

	}

	@Override
	public Resource[] getActiveContexts() {
		return context;
	}

	public Stream<ShapesGraph> getAllShapeContexts() {
		return Stream.concat(rdf4jShapesGraph.getAllShapeContexts(), baseSail.getAllShapeContexts());
	}

	public Stream<Resource> getTargetableShape() {
		assert context != null;
		return Stream.concat(rdf4jShapesGraph.getTargetableShape(), baseSail.getTargetableShape()).distinct();
	}

	public boolean isType(Resource subject, IRI type) {
		assert context != null;
		return rdf4jShapesGraph.isType(subject, type) || baseSail.isType(subject, type);
	}

	public Stream<Resource> getSubjects(Predicates predicate) {
		assert context != null;
		return Stream.concat(rdf4jShapesGraph.getSubjects(predicate), baseSail.getSubjects(predicate)).distinct();
	}

	public Stream<Value> getObjects(Resource subject, Predicates predicate) {
		assert context != null;
		return Stream.concat(rdf4jShapesGraph.getObjects(subject, predicate), baseSail.getObjects(subject, predicate))
				.distinct();
	}

	public Stream<Statement> getAllStatements(Resource id) {
		assert context != null;
		return Stream.concat(rdf4jShapesGraph.getAllStatements(id), baseSail.getAllStatements(id)).distinct();
	}

	public Value getRdfFirst(Resource subject) {
		assert context != null;
		Value rdfFirst1 = rdf4jShapesGraph.getRdfFirst(subject);
		return rdfFirst1 != null ? rdfFirst1 : baseSail.getRdfFirst(subject);
	}

	public Resource getRdfRest(Resource subject) {
		assert context != null;
		Value rdfRest1 = rdf4jShapesGraph.getRdfRest(subject);
		return (Resource) (rdfRest1 != null ? rdfRest1 : baseSail.getRdfRest(subject));
	}

	@Override
	public void close() {
		rdf4jShapesGraph.close();
		baseSail.close();
	}

}
