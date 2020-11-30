package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Exportable;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.TargetChainInterface;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNodeProvider;

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

	Stream<StatementMatcher> getStatementMatchers_rsx_targetShape(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope);

	SparqlFragment buildSparqlValidNodes_rsx_targetShape(StatementMatcher.Variable subject,
			StatementMatcher.Variable object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			Scope scope);

	enum Scope {
		none,
		nodeShape,
		propertyShape,
		not
	}

	ConstraintComponent deepClone();

}
