package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree;

import java.io.StringWriter;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.LinkedHashModelFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.shacl.AST.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.DebugPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ValidationReportNode;
import org.eclipse.rdf4j.sail.shacl.results.ValidationResult;

public class NodeShape extends Shape implements ConstraintComponent, Identifiable {

	protected boolean produceValidationReports;

	public NodeShape(boolean produceValidationReports) {
		this.produceValidationReports = produceValidationReports;
	}

	public NodeShape(NodeShape nodeShape) {
		super(nodeShape);
		this.produceValidationReports = nodeShape.produceValidationReports;
	}

	public static NodeShape getInstance(ShaclProperties properties,
			RepositoryConnection connection, Cache cache, boolean produceValidationReports) {

		Shape shape = cache.get(properties.getId());
		if (shape == null) {
			shape = new NodeShape(produceValidationReports);
			cache.put(properties.getId(), shape);
			shape.populate(properties, connection, cache);
		}

		return (NodeShape) shape;
	}

	@Override
	public void populate(ShaclProperties properties, RepositoryConnection connection,
			Cache cache) {
		super.populate(properties, connection, cache);

		if (properties.getMinCount() != null) {
			throw new IllegalStateException("NodeShapes do not support sh:MinCount in " + getId());
		}
		if (properties.getMaxCount() != null) {
			throw new IllegalStateException("NodeShapes do not support sh:MaxCount in " + getId());
		}
		if (properties.isUniqueLang()) {
			throw new IllegalStateException("NodeShapes do not support sh:uniqueLang in " + getId());
		}
		/*
		 * Also not supported here is: - sh:lessThan - sh:lessThanOrEquals - sh:qualifiedValueShape
		 */

		constraintComponents = getConstraintComponents(properties, connection, cache);

	}

	@Override
	protected NodeShape shallowClone() {
		return new NodeShape(this);
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		super.toModel(subject, predicate, model, exported);
		model.add(getId(), RDF.TYPE, SHACL.NODE_SHAPE);

		if (subject != null) {
			if (predicate == null) {
				model.add(subject, SHACL.NODE, getId());
			} else {
				model.add(subject, predicate, getId());
			}

		}

		if (exported.contains(getId())) {
			return;
		}
		exported.add(getId());

		constraintComponents.forEach(c -> c.toModel(getId(), null, model, exported));

	}

	@Override
	public PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, boolean negatePlan, boolean negateChildren, Scope scope) {
		if (isDeactivated()) {
			return new EmptyNode();
		}

		PlanNode union = new EmptyNode();

		for (ConstraintComponent constraintComponent : constraintComponents) {
			PlanNode validationPlanNode = constraintComponent
					.generateSparqlValidationPlan(connectionsGroup, logValidationPlans, negatePlan, false,
							Scope.nodeShape);
			if (!(constraintComponent instanceof PropertyShape) && produceValidationReports) {
				validationPlanNode = new ValidationReportNode(validationPlanNode, t -> {
					return new ValidationResult(t.getActiveTarget(), t.getActiveTarget(), this,
							constraintComponent.getConstraintComponent(), getSeverity(), t.getScope());
				});
			}
			union = new UnionNode(union,
					validationPlanNode);
		}

		return union;

	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup,
			boolean logValidationPlans, PlanNodeProvider overrideTargetNode, boolean negatePlan,
			boolean negateChildren, Scope scope) {

		if (isDeactivated()) {
			return new EmptyNode();
		}

		PlanNode union = new EmptyNode();

		for (ConstraintComponent constraintComponent : constraintComponents) {
			PlanNode validationPlanNode = constraintComponent
					.generateTransactionalValidationPlan(connectionsGroup, logValidationPlans, overrideTargetNode,
							negatePlan, false,
							Scope.nodeShape);

			validationPlanNode = new DebugPlanNode(validationPlanNode, "", p -> {
				assert p != null;
			});
			if (!(constraintComponent instanceof PropertyShape) && produceValidationReports) {
				validationPlanNode = new ValidationReportNode(validationPlanNode, t -> {
					return new ValidationResult(t.getActiveTarget(), t.getActiveTarget(), this,
							constraintComponent.getConstraintComponent(), getSeverity(), t.getScope());
				});
			}

			if (scope == Scope.propertyShape) {
				validationPlanNode = new ShiftToPropertyShape(validationPlanNode);
			}

			validationPlanNode = new DebugPlanNode(validationPlanNode, "", p -> {
				assert p != null;
			});

			union = new UnionNode(union,
					validationPlanNode);
		}

		return union;
	}

	@Override
	public ValidationApproach getPreferedValidationApproach() {
		return constraintComponents.stream()
				.map(ConstraintComponent::getPreferedValidationApproach)
				.reduce(ValidationApproach::reduce)
				.orElse(ValidationApproach.Transactional);
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.NodeConstraintComponent;
	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, boolean negated, Scope scope) {

		PlanNode planNode = constraintComponents.stream()
				.map(c -> c.getAllTargetsPlan(connectionsGroup, negated, Scope.nodeShape))
				.reduce(UnionNode::new)
				.orElse(new EmptyNode());

		planNode = new UnionNode(planNode,
				getTargetChain()
						.getEffectiveTarget("_target", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
						.getPlanNode(connectionsGroup, Scope.nodeShape, true));

		planNode = new DebugPlanNode(planNode, "NodeShape::getAllTargetsPlan");

		if (scope != Scope.nodeShape) {
			planNode = new ShiftToPropertyShape(planNode);
		}

		planNode = new Unique(planNode);

		return planNode;
	}

	@Override
	public ConstraintComponent deepClone() {
		NodeShape nodeShape = new NodeShape(this);

		constraintComponents.stream()
				.map(ConstraintComponent::deepClone)
				.collect(Collectors.toList());

		nodeShape.constraintComponents = constraintComponents;

		return nodeShape;
	}

	@Override
	public String toString() {
		Model statements = toModel(new DynamicModel(new LinkedHashModelFactory()));
		StringWriter stringWriter = new StringWriter();
		Rio.write(statements, stringWriter, RDFFormat.TURTLE);
		return stringWriter.toString();
	}

}
