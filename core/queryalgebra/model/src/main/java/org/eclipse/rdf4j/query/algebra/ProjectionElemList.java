/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 */
public class ProjectionElemList extends AbstractQueryModelNode {

	/*-----------*
	 * Variables *
	 *-----------*/

	private ProjectionElem[] elements = {};
	private List<ProjectionElem> elementsList = Collections.emptyList();

	/*--------------*
	 * Constructors *
	 *--------------*/

	public ProjectionElemList() {
	}

	public ProjectionElemList(ProjectionElem... elements) {
		addElements(elements);
	}

	public ProjectionElemList(Iterable<ProjectionElem> elements) {
		addElements(elements);
	}

	/*---------*
	 * Methods *
	 *---------*/

	public List<ProjectionElem> getElements() {
		return elementsList;
	}

	public void setElements(List<ProjectionElem> elements) {
		this.elementsList = Collections.unmodifiableList(elements);
		this.elements = this.elementsList.toArray(new ProjectionElem[0]);

	}

	public void addElements(ProjectionElem... elements) {
		addElements(List.of(elements));
	}

	public void addElements(Iterable<ProjectionElem> elements) {
		addElements(StreamSupport.stream(elements.spliterator(), false)
				.collect(Collectors.toList()));
	}

	public void addElements(List<ProjectionElem> elements) {
		if (elementsList.isEmpty()) {
			setElements(elements);
		} else {
			ArrayList<ProjectionElem> currentElementsList = new ArrayList<>(elementsList);
			for (ProjectionElem projectionElem : elements) {
				projectionElem.setParentNode(this);
			}
			currentElementsList.addAll(elements);
			setElements(currentElementsList);
		}

	}

	public void addElement(ProjectionElem pe) {
		assert pe != null : "pe must not be null";

		ArrayList<ProjectionElem> currentElementsList = new ArrayList<>(elementsList);
		currentElementsList.add(pe);
		setElements(currentElementsList);

		pe.setParentNode(this);
	}

	public Set<String> getTargetNames() {
		Set<String> targetNames = new LinkedHashSet<>(elementsList.size());

		for (ProjectionElem pe : elementsList) {
			targetNames.add(pe.getTargetName());
		}

		return targetNames;
	}

	public Set<String> getTargetNamesFor(Collection<String> sourceNames) {
		Set<String> targetNames = new LinkedHashSet<>(elementsList.size());

		for (ProjectionElem pe : elementsList) {
			if (sourceNames.contains(pe.getSourceName())) {
				targetNames.add(pe.getTargetName());
			}
		}

		return targetNames;
	}

	@Override
	public <X extends Exception> void visit(QueryModelVisitor<X> visitor) throws X {
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor) throws X {
		for (int i = 0; i < elements.length; i++) {
			elements[i].visit(visitor);
		}
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		ArrayList<ProjectionElem> currentElementsList = new ArrayList<>(elementsList);
		if (replaceNodeInList(currentElementsList, current, replacement)) {
			setElements(currentElementsList);
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ProjectionElemList) {
			ProjectionElemList o = (ProjectionElemList) other;
			return elementsList.equals(o.getElements());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return elementsList.hashCode();
	}

	@Override
	public ProjectionElemList clone() {
		ProjectionElemList clone = (ProjectionElemList) super.clone();

		clone.elements = new ProjectionElem[elements.length];

		for (int i = 0; i < elements.length; i++) {
			clone.elements[i] = elements[i].clone();
			clone.elements[i].setParentNode(clone);
		}

		clone.elementsList = List.of(clone.elements);

		return clone;
	}

}
