package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Exportable;
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

	Stream<? extends StatementPattern> getStatementPatterns_rsx_targetShape(Var subject, Var object,
			RdfsSubClassOfReasoner rdfsSubClassOfReasoner, Scope scope);

	String buildSparqlValidNodes_rsx_targetShape(Var subject, Var object, RdfsSubClassOfReasoner rdfsSubClassOfReasoner,
			Scope scope);

	enum Scope {
		none,
		nodeShape,
		propertyShape,
		not;
	}

	ConstraintComponent deepClone();

}
