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
import java.util.List;
import java.util.Objects;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;

public class ContextWithShapes {

	private final Resource[] dataGraph;
	private final Resource[] shapeGraph;
	private final List<Shape> shapes;

	public ContextWithShapes(Resource[] dataGraph, Resource[] shapeGraph, List<Shape> shapes) {
		this.shapeGraph = shapeGraph;
		this.dataGraph = dataGraph;
		this.shapes = shapes;
	}

	public Resource[] getShapeGraph() {
		return shapeGraph;
	}

	public List<Shape> getShapes() {
		return shapes;
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
		ContextWithShapes that = (ContextWithShapes) o;
		return Arrays.equals(dataGraph, that.dataGraph) && Arrays.equals(shapeGraph, that.shapeGraph)
				&& shapes.equals(that.shapes);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(shapes);
		result = 31 * result + Arrays.hashCode(dataGraph);
		result = 31 * result + Arrays.hashCode(shapeGraph);
		return result;
	}

	public void toModel(Model model) {
		DynamicModel emptyModel = new DynamicModelFactory().createEmptyModel();
		for (Shape shape : shapes) {
			shape.toModel(emptyModel);
		}
		for (Statement statement : emptyModel) {
			for (Resource context : shapeGraph) {
				model.add(statement.getSubject(), statement.getPredicate(), statement.getObject(), context);
			}
		}
	}
}
