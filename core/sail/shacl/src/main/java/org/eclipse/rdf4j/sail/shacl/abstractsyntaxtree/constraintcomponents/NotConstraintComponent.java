package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.AST.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Cache;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.NodeShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Shape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.TargetChain;

public class NotConstraintComponent extends AbstractConstraintComponent {
	final Shape not;

	public NotConstraintComponent(Resource id, RepositoryConnection connection,
			Cache cache) {
		super(id);

		ShaclProperties p = new ShaclProperties(id, connection);

		if (p.getType() == SHACL.NODE_SHAPE) {
			not = NodeShape.getInstance(p, connection, cache);
		} else if (p.getType() == SHACL.PROPERTY_SHAPE) {
			not = PropertyShape.getInstance(p, connection, cache);
		} else {
			throw new IllegalStateException("Unknown shape type for " + p.getId());
		}

	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.NOT, getId());

		not.toModel(null, null, model, exported);

	}

	@Override
	public void setTargetChain(TargetChain targetChain) {
		super.setTargetChain(targetChain);
		not.setTargetChain(targetChain.setOptimizable(false));
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.NotConstraintComponent;
	}

	@Override
	public PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			boolean negatePlan, boolean negateChildren, Scope scope) {
		return not.generateSparqlValidationPlan(connectionsGroup, logValidationPlans, !negatePlan, false, Scope.not);
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			PlanNodeProvider overrideTargetNode, boolean negatePlan, boolean negateChildren, Scope scope) {
		return not.generateTransactionalValidationPlan(connectionsGroup, logValidationPlans, overrideTargetNode,
				!negatePlan, false, Scope.not);
	}
}
