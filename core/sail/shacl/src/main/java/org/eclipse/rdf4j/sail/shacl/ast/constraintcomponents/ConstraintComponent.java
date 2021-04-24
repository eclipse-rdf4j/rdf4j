package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.RdfsSubClassOfReasoner;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ast.Exportable;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.TargetChainInterface;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationApproach;
import org.eclipse.rdf4j.sail.shacl.ast.ValidationQuery;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;

public interface ConstraintComponent extends Exportable, TargetChainInterface {

	ValidationQuery generateSparqlValidationQuery(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			boolean negatePlan, boolean negateChildren, Scope scope);

	PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, PlanNodeProvider overrideTargetNode,
			Scope scope);

	/**
	 * A constraint component should decide which validation approach is going to be the optimal performance wise based
	 * on the state of the transaction and base sail.
	 */
	ValidationApproach getPreferredValidationApproach(ConnectionsGroup connectionsGroup);

	/**
	 * Should return the fastest validation approach for bulk validation. When aggregating multiple constraint
	 * components the most compatible should be chosen.
	 */
	ValidationApproach getOptimalBulkValidationApproach();

	/**
	 *
	 * @param connectionsGroup
	 * @param scope
	 * @return true if the constraint component should be evaluated, eg. if validation is needed.
	 */
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
