package org.eclipse.rdf4j.sail.shacl.ast.targets;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.wrapper.shape.ShapeSource;

public class SparqlTarget extends Target {

	SparqlTarget(Resource id, ShaclSail shaclSail, ShapeSource shapeSource, Resource sparqlTarget) {

	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {

	}

	@Override
	public SparqlFragment getTargetQueryFragment(StatementMatcher.Variable subject, StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider, Set<String> inheritedVarNames) {
		return null;
	}

	@Override
	public IRI getPredicate() {
		return null;
	}

	@Override
	public PlanNode getAdded(ConnectionsGroup connectionsGroup, Resource[] dataGraph, ConstraintComponent.Scope scope) {
		return null;
	}

	@Override
	public PlanNode getTargetFilter(ConnectionsGroup connectionsGroup, Resource[] dataGraph, PlanNode parent) {
		return null;
	}
}
