/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.sail.shacl.AST.PropertyShape;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Heshan Jayasinghe, Håvard Mikkelsen Ottestad
 */
public class Tuple implements Comparable<Tuple> {

	private Deque<PropertyShape> causedByPropertyShapes = new ArrayDeque<>(1);

	private List<Tuple> history = new ArrayList<>(1);

	public List<Value> line = new ArrayList<>(3);

	public Tuple(List<Value> list) {
		line = list;
	}

	public Tuple(List<Value> list, Tuple historyTuple) {
		line = list;
		addHistory(historyTuple);
	}

	public Tuple() {
	}

	public Tuple(BindingSet bindingset) {
		bindingset.getBindingNames().stream().sorted().forEach(name -> line.add(bindingset.getValue(name)));

	}

	public Tuple(Tuple tuple) {
		line = new ArrayList<>(tuple.line);
		history = new ArrayList<>(tuple.history);
		causedByPropertyShapes = new ArrayDeque<>(causedByPropertyShapes);
	}

	public List<Value> getlist() {
		return line;
	}

	@Override
	public String toString() {
		String propertyShapeDescrption = "";
		if (causedByPropertyShapes != null) {

			String join = String.join(" , ",
					causedByPropertyShapes.stream()
							.map(p -> p.getClass().getSimpleName() + " <" + p.getId() + ">")
							.collect(Collectors.toList()));

			propertyShapeDescrption = ", propertyShapes= " + join;
		}

		return "Tuple{" + "line=" + Arrays.toString(line.toArray()) + propertyShapeDescrption + "}";
	}

	public void addCausedByPropertyShape(PropertyShape propertyShape) {
		if (causedByPropertyShapes == null) {
			causedByPropertyShapes = new ArrayDeque<>();
		}
		causedByPropertyShapes.addFirst(propertyShape);
	}

	public Deque<PropertyShape> getCausedByPropertyShapes() {
		return causedByPropertyShapes;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Tuple tuple = (Tuple) o;

		if (tuple.line.size() != line.size()) {
			return false;
		}

		for (int i = 0; i < line.size(); i++) {
			if (!(line.get(i) == tuple.line.get(i) || line.get(i).equals(tuple.line.get(i)))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {

		return Objects.hash(line);
	}

	@Override
	public int compareTo(Tuple o) {

		for (int i = 0; i < Math.min(o.line.size(), line.size()); i++) {
			int compareTo = line.get(i).toString().compareTo(o.line.get(i).toString());

			if (compareTo != 0) {
				return compareTo;
			}

		}

		return 0;
	}

	public String getCause() {
		return " [ "
				+ String.join(" , ", history.stream().distinct().map(Object::toString).collect(Collectors.toList()))
				+ " ]";
	}

	public void addHistory(Tuple tuple) {
		history.addAll(tuple.history);
		history.add(tuple);
	}

	public void addAllCausedByPropertyShape(Deque<PropertyShape> causedByPropertyShapes) {
		if (causedByPropertyShapes != null) {

			this.causedByPropertyShapes.addAll(causedByPropertyShapes);
		}
	}
}
