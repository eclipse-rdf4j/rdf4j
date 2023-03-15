/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.ast.paths;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.shacl.ast.Exportable;
import org.eclipse.rdf4j.sail.shacl.ast.Identifiable;
import org.eclipse.rdf4j.sail.shacl.ast.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.Targetable;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeWrapper;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

public abstract class Path implements Identifiable, Exportable, Targetable {

	Resource id;

	public Path(Resource id) {
		this.id = id;
	}

	@Override
	public Resource getId() {
		return id;
	}

	static public Path buildPath(ShapeSource shapeSource, Resource id) {
		if (id == null) {
			return null;
		}

		if (id.isBNode()) {
			List<Statement> collect = shapeSource.getAllStatements(id)
					.collect(Collectors.toList());

			for (Statement statement : collect) {
				IRI pathType = statement.getPredicate();

				switch (pathType.toString()) {
				case "http://www.w3.org/ns/shacl#inversePath":
					return new InversePath(id, (Resource) statement.getObject(), shapeSource);
				case "http://www.w3.org/ns/shacl#alternativePath":
					return new AlternativePath(id, (Resource) statement.getObject(), shapeSource);
				case "http://www.w3.org/ns/shacl#zeroOrMorePath":
					return new ZeroOrMorePath(id, (Resource) statement.getObject(), shapeSource);
				case "http://www.w3.org/ns/shacl#oneOrMorePath":
					return new OneOrMorePath(id, (Resource) statement.getObject(), shapeSource);
				case "http://www.w3.org/ns/shacl#zeroOrOnePath":
					return new ZeroOrOnePath(id, (Resource) statement.getObject(), shapeSource);
				case "http://www.w3.org/1999/02/22-rdf-syntax-ns#first":
					return new SequencePath(id, shapeSource);
				default:
					break;
				}

			}

			throw new ShaclUnsupportedException();

		} else {
			return new SimplePath((IRI) id);
		}

	}

	/**
	 * Get all values added in this transaction.
	 */
	public abstract PlanNode getAllAdded(ConnectionsGroup connectionsGroup,
			Resource[] dataGraph, PlanNodeWrapper planNodeWrapper);

	/**
	 * Get values added in this transaction. Validation performance may improve if more value are retrieved. Validation
	 * correctness and completeness will not be affected if fewer values are retrieved, or if no items are retrieved.
	 */
	public abstract PlanNode getAnyAdded(ConnectionsGroup connectionsGroup,
			Resource[] dataGraph, PlanNodeWrapper planNodeWrapper);

	public String getVariablePrefix(StatementMatcher.Variable subject, StatementMatcher.Variable object) {
		String className = this.getClass().getSimpleName();
		return subject.getName() + "_" + (object != null ? object.getName() : "null") + "_" + className + "_";
	}

	abstract public boolean isSupported();

	public abstract String toSparqlPathString();

	@Override
	public final Set<Namespace> getNamespaces() {
		return Set.of();
	}

}
