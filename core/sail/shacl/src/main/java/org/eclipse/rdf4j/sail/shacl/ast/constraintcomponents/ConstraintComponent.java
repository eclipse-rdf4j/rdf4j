package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.Set;

import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.Exportable;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.TargetChainInterface;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;

public interface ConstraintComponent extends Exportable, TargetChainInterface {

	PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			boolean negatePlan, boolean negateChildren, Scope scope);

	PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, PlanNodeProvider overrideTargetNode,
			Scope scope);

	ValidationApproach getPreferedValidationApproach();

	Set<ValidationApproach> getSupportedValidationApproaches();

	boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope);

	SourceConstraintComponent getConstraintComponent();

	PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Scope scope);

	SparqlFragment buildSparqlValidNodes_rsx_targetShape(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			Scope scope);

	enum Scope {
		none,
		nodeShape,
		propertyShape
	}

	ConstraintComponent deepClone();

}
