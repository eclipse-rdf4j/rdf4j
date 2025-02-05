package org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents;

import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.DASH;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.ValidationSettings;
import org.eclipse.rdf4j.sail.shacl.ast.SparqlFragment;
import org.eclipse.rdf4j.sail.shacl.ast.StatementMatcher;
import org.eclipse.rdf4j.sail.shacl.ast.Targetable;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.BulkedExternalInnerJoin;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.FilterByPredicateObject;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.GroupByFilter;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeHelper;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.ast.planNodes.ValidationReportNode;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

/**
 * A constraint component that enforces uniqueness of a property value for all resources of a specific class
 * (dash:uniqueValueForClass).
 */
public class DashUniqueValueForClassConstraintComponent extends AbstractConstraintComponent {

	private final IRI uniqueValueForClass;

	public DashUniqueValueForClassConstraintComponent(IRI uniqueValueForClass) {
		this.uniqueValueForClass = uniqueValueForClass;
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {

		return SourceConstraintComponent.UniqueValueForClassConstraintComponent;
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(
			ConnectionsGroup connectionsGroup,
			ValidationSettings validationSettings,
			PlanNodeProvider overrideTargetNode,
			Scope scope
	) {
		// We only support this constraint for property shapes (with a path).
		if (scope != Scope.propertyShape) {
			return EmptyNode.getInstance();
		}

		// 1) Get the base plan node for the shape’s targets
		PlanNode targetSubjects = overrideTargetNode != null
				? overrideTargetNode.getPlanNode()
				: getAllTargetsPlan(
						connectionsGroup,
						validationSettings.getDataGraph(),
						scope,
						new StatementMatcher.StableRandomVariableProvider(),
						validationSettings
				);

		// 2) Filter subjects to only those with rdf:type == uniqueValueForClass
		PlanNode typedSubjects = new FilterByPredicateObject(
				connectionsGroup.getBaseConnection(),
				validationSettings.getDataGraph(),
				RDF.TYPE,
				Set.of(uniqueValueForClass),
				targetSubjects,
				true, // Keep only matches
				FilterByPredicateObject.FilterOn.activeTarget,
				true, // includeInferred
				connectionsGroup
		);

		// 3) Retrieve shape’s path (subject->object) pairs via a bulk-join
		Path path = getTargetChain().getPath()
				.orElseThrow(
						() -> new IllegalStateException(
								"No sh:path found for property shape with dash:uniqueValueForClass!")
				);

		// Prepare the SPARQL pattern for the path: "subject -> object"
		// We'll use the variable "a" for subject, and "c" for object
		SparqlFragment pathQueryFragment = path.getTargetQueryFragment(
				new StatementMatcher.Variable("a"),
				new StatementMatcher.Variable("c"),
				connectionsGroup.getRdfsSubClassOfReasoner(),
				new StatementMatcher.StableRandomVariableProvider(),
				Set.of()
		);

		// Create a BulkedExternalInnerJoin to combine typedSubjects with the path pattern
		PlanNode typedSubjectsAndValues = new BulkedExternalInnerJoin(
				typedSubjects,
				connectionsGroup.getBaseConnection(),
				validationSettings.getDataGraph(),
				pathQueryFragment, // The path SPARQL fragment
				false, // skipBasedOnPreviousConnection
				null, // previousStateConnection
				BulkedExternalInnerJoin.getMapper("a", "c", scope, validationSettings.getDataGraph()),
				connectionsGroup,
				// pass any "StatementMatcher.Variable" list if needed, else empty or default
				java.util.List.of(new StatementMatcher.Variable("a"), new StatementMatcher.Variable("c"))
		);

		// 4) We need to group by the object "c" to detect collisions
		// but first ensure the plan node is sorted if the grouping requires it.
		PlanNode sorted = PlanNodeHelper.handleSorting(true, typedSubjectsAndValues, connectionsGroup);

		// GroupByFilter => keep only collisions (group.size() > 1)
		PlanNode collisions = new GroupByFilter(sorted, group -> (group.size() > 1), connectionsGroup);

		// 5) Convert collisions into validation results
		PlanNode validation = new ValidationReportNode(collisions, tuple -> {
			// Each collision => new ValidationResult
			return new ValidationResult(
					tuple.getActiveTarget(), // the focus node
					tuple.getValue(), // the value node
					this, // shape
					this, // constraint component
					getSeverity(),
					tuple.getScope(),
					tuple.getContexts(),
					getContexts()
			);
		}, connectionsGroup);

		return validation;
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Resource[] dataGraph, Scope scope,
			StatementMatcher.StableRandomVariableProvider stableRandomVariableProvider,
			ValidationSettings validationSettings) {
		// Typically returns a plan node for all shape targets (and possibly shifts node-shapes to property-shapes).
		// We can rely on super if we only do standard logic or override if custom logic is needed.
		return super.getAllTargetsPlan(connectionsGroup, dataGraph, scope, stableRandomVariableProvider,
				validationSettings);
	}

	@Override
	public ConstraintComponent deepClone() {
		return new DashUniqueValueForClassConstraintComponent(uniqueValueForClass);
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> cycleDetection) {
		// Add dash:uniqueValueForClass triple
		model.add(subject, DASH.uniqueValueForClass, uniqueValueForClass);
	}

	@Override
	public List<Literal> getDefaultMessage() {
		// Provide a default or empty message
		return List.of();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof DashUniqueValueForClassConstraintComponent))
			return false;
		DashUniqueValueForClassConstraintComponent that = (DashUniqueValueForClassConstraintComponent) o;
		return uniqueValueForClass.equals(that.uniqueValueForClass);
	}

	@Override
	public int hashCode() {
		return uniqueValueForClass.hashCode()
				^ DashUniqueValueForClassConstraintComponent.class.hashCode();
	}
}
