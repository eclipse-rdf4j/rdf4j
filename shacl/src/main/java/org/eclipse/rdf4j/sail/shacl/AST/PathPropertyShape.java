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
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnorderedSelect;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The AST (Abstract Syntax Tree) node that represents the sh:path on a property nodeShape.
 *
 * @author Heshan Jayasinghe
 */
public class PathPropertyShape extends PropertyShape {

	Path path;

	PathPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, boolean deactivated,
			Resource path) {
		super(id, nodeShape, deactivated);

		// only simple path is supported. There are also no checks. Any use of paths that are not single predicates is
		// undefined.
		if (path != null) {
			this.path = new SimplePath((IRI) path, connection);
		}

	}

	@Override
	public PlanNode getPlan(ShaclSailConnection shaclSailConnection, NodeShape nodeShape, boolean printPlans,
			PlanNodeProvider overrideTargetNode) {
		return shaclSailConnection.getCachedNodeFor(new Sort(new UnorderedSelect(shaclSailConnection, null,
				(IRI) path.getId(), null, UnorderedSelect.OutputPattern.SubjectObject)));
	}

	@Override
	public PlanNode getPlanAddedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape,
			PlaneNodeWrapper planeNodeWrapper) {

		PlanNode unorderedSelect = new UnorderedSelect(shaclSailConnection.getAddedStatements(), null,
				(IRI) path.getId(), null, UnorderedSelect.OutputPattern.SubjectObject);
		if (planeNodeWrapper != null) {
			unorderedSelect = planeNodeWrapper.wrap(unorderedSelect);
		}
		return shaclSailConnection.getCachedNodeFor(new Sort(unorderedSelect));
	}

	@Override
	public PlanNode getPlanRemovedStatements(ShaclSailConnection shaclSailConnection, NodeShape nodeShape,
			PlaneNodeWrapper planeNodeWrapper) {
		PlanNode unorderedSelect = new UnorderedSelect(shaclSailConnection.getRemovedStatements(), null,
				(IRI) path.getId(), null, UnorderedSelect.OutputPattern.SubjectObject);
		if (planeNodeWrapper != null) {
			unorderedSelect = planeNodeWrapper.wrap(unorderedSelect);
		}
		return shaclSailConnection.getCachedNodeFor(new Sort(unorderedSelect));
	}

	@Override
	public List<Path> getPaths() {
		return Collections.singletonList(path);
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
}
