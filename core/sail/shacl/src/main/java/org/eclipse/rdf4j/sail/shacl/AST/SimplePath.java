/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.AST;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.SailConnection;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.Stats;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.planNodes.Sort;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnorderedSelect;

/**
 * The AST (Abstract Syntax Tree) node that represents a simple path for exactly one predicate. Currently there is no
 * support for complex paths.
 *
 * @author Heshan Jayasinghe
 * @author Håvard M. Ottestad
 */
public class SimplePath extends Path {

	private final IRI path;

	public SimplePath(IRI id) {
		super(id);
		this.path = id;

	}

	@Override
	public PlanNode getPlan(ConnectionsGroup connectionsGroup, boolean printPlans,
			PlanNodeProvider overrideTargetNode, boolean negateThisPlan, boolean negateSubPlans) {
		return connectionsGroup
				.getCachedNodeFor(new Sort(new UnorderedSelect(connectionsGroup.getBaseConnection(), null,
						path, null, UnorderedSelect.OutputPattern.SubjectObject)));
	}

	@Override
	public PlanNode getPlanAddedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {

		PlanNode unorderedSelect = new UnorderedSelect(connectionsGroup.getAddedStatements(), null,
				path, null, UnorderedSelect.OutputPattern.SubjectObject);
		if (planeNodeWrapper != null) {
			unorderedSelect = planeNodeWrapper.wrap(unorderedSelect);
		}
		return connectionsGroup.getCachedNodeFor(new Sort(unorderedSelect));
	}

	@Override
	public PlanNode getPlanRemovedStatements(ConnectionsGroup connectionsGroup,
			PlaneNodeWrapper planeNodeWrapper) {
		PlanNode unorderedSelect = new UnorderedSelect(connectionsGroup.getRemovedStatements(), null,
				path, null, UnorderedSelect.OutputPattern.SubjectObject);
		if (planeNodeWrapper != null) {
			unorderedSelect = planeNodeWrapper.wrap(unorderedSelect);
		}
		return connectionsGroup.getCachedNodeFor(new Sort(unorderedSelect));
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, boolean negated) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Path> getPaths() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean requiresEvaluation(SailConnection addedStatements, SailConnection removedStatements, Stats stats) {

		if (stats.isEmpty()) {
			return false;
		}

		return addedStatements.hasStatement(null, path, null, false)
				|| removedStatements.hasStatement(null, path, null, false);
	}

	@Override
	public String getQuery(String subjectVariable, String objectVariable,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner) {

		return subjectVariable + " <" + path + "> " + objectVariable + " . \n";

	}

	public IRI getPath() {
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
		SimplePath that = (SimplePath) o;
		return Objects.equals(path, that.path);
	}

	@Override
	public int hashCode() {
		return Objects.hash(path);
	}

	@Override
	public String toString() {
		return path.toString();
	}

	@Override
	public Stream<StatementPattern> getStatementsPatterns(Var start, Var end) {
		return Stream
				.of(new StatementPattern(start, new Var(UUID.randomUUID().toString(), path), end));

	}
}
