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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 */
public class ProjectionElemList extends AbstractQueryModelNode {

	/*-----------*
	 * Variables *
	 *-----------*/

	private List<ProjectionElem> elements = new ArrayList<ProjectionElem>();

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
		return elements;
	}

	public void setElements(List<ProjectionElem> elements) {
		this.elements = elements;
	}

	public void addElements(ProjectionElem... elements) {
		for (ProjectionElem pe : elements) {
			addElement(pe);
		}
	}

	public void addElements(Iterable<ProjectionElem> elements) {
		for (ProjectionElem pe : elements) {
			addElement(pe);
		}
	}

	public void addElement(ProjectionElem pe) {
		assert pe != null : "pe must not be null";
		elements.add(pe);
		pe.setParentNode(this);
	}

	public Set<String> getTargetNames() {
		Set<String> targetNames = new LinkedHashSet<String>(elements.size());

		for (ProjectionElem pe : elements) {
			targetNames.add(pe.getTargetName());
		}

		return targetNames;
	}

	public Set<String> getTargetNamesFor(Collection<String> sourceNames) {
		Set<String> targetNames = new LinkedHashSet<String>(elements.size());

		for (ProjectionElem pe : elements) {
			if (sourceNames.contains(pe.getSourceName())) {
				targetNames.add(pe.getTargetName());
			}
		}

		return targetNames;
	}

	public <X extends Exception> void visit(QueryModelVisitor<X> visitor)
		throws X
	{
		visitor.meet(this);
	}

	@Override
	public <X extends Exception> void visitChildren(QueryModelVisitor<X> visitor)
		throws X
	{
		for (ProjectionElem pe : elements) {
			pe.visit(visitor);
		}

		super.visitChildren(visitor);
	}

	@Override
	public void replaceChildNode(QueryModelNode current, QueryModelNode replacement) {
		if (replaceNodeInList(elements, current, replacement)) {
			return;
		}
		super.replaceChildNode(current, replacement);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof ProjectionElemList) {
			ProjectionElemList o = (ProjectionElemList)other;
			return elements.equals(o.getElements());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return elements.hashCode();
	}

	@Override
	public ProjectionElemList clone() {
		ProjectionElemList clone = (ProjectionElemList)super.clone();

		clone.elements = new ArrayList<ProjectionElem>(getElements().size());
		for (ProjectionElem pe : getElements()) {
			clone.addElement(pe.clone());
		}

		return clone;
	}
}
