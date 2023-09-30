/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.ast;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;

public class ContextWithShape {

	private final Resource[] dataGraph;
	private final Resource[] shapeGraph;
	private final Shape shape;

	public ContextWithShape(Resource[] dataGraph, Resource[] shapeGraph, Shape shape) {
		this.shapeGraph = shapeGraph;
		this.dataGraph = dataGraph;
		Arrays.sort(this.dataGraph, Comparator.comparing(v -> v != null ? v.stringValue() : "null"));
		Arrays.sort(this.shapeGraph, Comparator.comparing(v -> v != null ? v.stringValue() : "null"));
		this.shape = shape;
	}

	public Resource[] getShapeGraph() {
		return shapeGraph;
	}

	public Shape getShape() {
		return shape;
	}

	public boolean hasShape() {
		return shape != null;
	}

	public Resource[] getDataGraph() {
		return dataGraph;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ContextWithShape that = (ContextWithShape) o;
		return Arrays.equals(dataGraph, that.dataGraph)
				&& shape.equals(that.shape);
	}

	@Override
	public int hashCode() {
		int result = shape.hashCode();
		result = 31 * result + Arrays.hashCode(dataGraph);
		return result;
	}

	public void toModel(Model model, Set<Resource> cycleDetection) {
		DynamicModel emptyModel = new DynamicModelFactory().createEmptyModel();
		shape.toModel(null, null, emptyModel, cycleDetection);

		for (Statement statement : emptyModel) {
			for (Resource context : shapeGraph) {
				model.add(statement.getSubject(), statement.getPredicate(), statement.getObject(), context);
			}
		}
	}

}
