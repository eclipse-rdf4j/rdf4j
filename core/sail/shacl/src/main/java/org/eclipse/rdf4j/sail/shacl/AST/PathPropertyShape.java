/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.eclipse.rdf4j.sail.shacl.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnorderedSelect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The AST (Abstract Syntax Tree) node that represents the sh:path on a property nodeShape.
 *
 * @author Heshan Jayasinghe
 * @author Håvard M. Ottestad
 */
public abstract class PathPropertyShape extends PropertyShape {

	private Path path;

	private static final Logger logger = LoggerFactory.getLogger(ShaclSailConnection.class);

	private final static Set<IRI> complexPathPredicates = new HashSet<>(
			Arrays.asList(
					RDF.FIRST,
					RDF.REST,
					SHACL.ALTERNATIVE_PATH,
					SHACL.INVERSE_PATH,
					SHACL.ZERO_OR_MORE_PATH,
					SHACL.ONE_OR_MORE_PATH,
					SHACL.ZERO_OR_ONE_PATH));

	PathPropertyShape(Resource id, SailRepositoryConnection connection, NodeShape nodeShape, boolean deactivated,
			PathPropertyShape parent, Resource path) {
		super(id, nodeShape, deactivated ? deactivated : !validPath(path, connection), parent);

		// Only simple path is supported. Use of complex paths will make the property shape deactivated and log a
		// warning
		if (path != null) {
			if (validPath(path, connection)) {
				this.path = new SimplePath((IRI) path);
			} else {
				logger.warn(
						"Unsupported SHACL feature with complex path. Only single predicate paths are supported. <{}> shape has been deactivated! \n{}",
						id,
						describe(connection, path));
			}

		}

	}

	private static boolean validPath(Resource path, SailRepositoryConnection connection) {

		if (path != null) {
			if (!(path instanceof IRI)) {
				return false;
			} else {

				try (Stream<Statement> stream = connection.getStatements(path, null, null).stream()) {

					boolean complexPath = stream
							.map(Statement::getPredicate)
							.anyMatch(complexPathPredicates::contains);

					if (complexPath) {
						return false;
					}
				}
			}
		}

		return true;
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
