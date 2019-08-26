/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnorderedSelect;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The AST (Abstract Syntax Tree) node that represents the sh:path on a property nodeShape.
 *
 * @author Heshan Jayasinghe
 */
public abstract class PathPropertyShape extends PropertyShape {

	private Path path;

	PathPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, boolean deactivated,
			PathPropertyShape parent, Resource path) {
		super(id, nodeShape, deactivated, parent);

		// only simple path is supported. There are also no checks. Any use of paths that are not single predicates is
		// undefined.
		if (path != null) {
			this.path = new SimplePath((IRI) path);
		}

	}

	PathPropertyShape(Resource id, NodeShape nodeShape, boolean deactivated, PathPropertyShape parent, Path path) {
		super(id, nodeShape, deactivated, parent);

		this.path = path;
	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {
		return connectionsGroup
				.getCachedNodeFor(new Sort(new UnorderedSelect(connectionsGroup.getBaseConnection(), null,
						(IRI) getPath().getId(), null, UnorderedSelect.OutputPattern.SubjectObject)));
	}

	@Override
	public PlanNode getPlanAddedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {

		PlanNode unorderedSelect = new UnorderedSelect(connectionsGroup.getAddedStatements(), null,
				(IRI) getPath().getId(), null, UnorderedSelect.OutputPattern.SubjectObject);
		if (planeNodeWrapper != null) {
			unorderedSelect = planeNodeWrapper.wrap(unorderedSelect);
		}
		return connectionsGroup.getCachedNodeFor(new Sort(unorderedSelect));
	}

	@Override
	public PlanNode getPlanRemovedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {
		PlanNode unorderedSelect = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null,
				(IRI) getPath().getId(), null, UnorderedSelect.OutputPattern.SubjectObject);
		if (planeNodeWrapper != null) {
			unorderedSelect = planeNodeWrapper.wrap(unorderedSelect);
		}
		return connectionsGroup.getCachedNodeFor(new Sort(unorderedSelect));
	}

	@Override
	public List<Path> getPaths() {
		return Collections.singletonList(getPath());
	}

	public boolean hasOwnPath() {
		return path != null;
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements) {
		if (deactivated) {
			return false;
		}

		if (path == null) {
			return super.requiresEvaluation(addedStatements, removedStatements);
		}

		return super.requiresEvaluation(addedStatements, removedStatements)
				|| path.requiresEvaluation(addedStatements, removedStatements);
	}

	public Path getPath() {
		if (path == null && parent != null) {
			return parent.getPath();
		}
		return path;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		PathPropertyShape that = (PathPropertyShape) o;
		return Objects.equals(path, that.path);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), path);
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, boolean negated) {
		Select select = new Select(connectionsGroup.getBaseConnection(), "?a ?b ?c", "?a");
		Unique unique = new Unique(select);
		return nodeShape.getTargetFilter(connectionsGroup.getBaseConnection(), unique);
	}
}
