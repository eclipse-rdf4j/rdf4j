package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.constraintcomponents;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.AST.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.ShaclSail;
import org.eclipse.rdf4j.sail.shacl.SourceConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Cache;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.NodeShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.PropertyShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.ShaclUnsupportedException;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.Shape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.DebugPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.GroupByCountFilter;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.LeftOuterJoin;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.NotValuesIn;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PlanNodeProvider;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.PropertyShapeScopeWithValueFilter;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.ShiftToPropertyShape;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnBufferedPlanNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.UnionNode;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.planNodes.Unique;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.targets.TargetChain;

public class QualifiedMinCountConstraintComponent extends AbstractConstraintComponent {
	Shape qualifiedValueShape;
	Boolean qualifiedValueShapesDisjoint;
	Long qualifiedMinCount;

	public QualifiedMinCountConstraintComponent(Resource id, RepositoryConnection connection,
			Cache cache, ShaclSail shaclSail, Boolean qualifiedValueShapesDisjoint, Long qualifiedMinCount) {
		super(id);

		ShaclProperties p = new ShaclProperties(id, connection);

		this.qualifiedValueShapesDisjoint = qualifiedValueShapesDisjoint;
		this.qualifiedMinCount = qualifiedMinCount;

		if (p.getType() == SHACL.NODE_SHAPE) {
			qualifiedValueShape = NodeShape.getInstance(p, connection, cache, false, shaclSail);
		} else if (p.getType() == SHACL.PROPERTY_SHAPE) {
			qualifiedValueShape = PropertyShape.getInstance(p, connection, cache, shaclSail);
		} else {
			throw new IllegalStateException("Unknown shape type for " + p.getId());
		}

	}

	public QualifiedMinCountConstraintComponent(QualifiedMinCountConstraintComponent constraintComponent) {
		super(constraintComponent.getId());
		this.qualifiedValueShape = (Shape) constraintComponent.qualifiedValueShape.deepClone();
		this.qualifiedValueShapesDisjoint = constraintComponent.qualifiedValueShapesDisjoint;
		this.qualifiedMinCount = constraintComponent.qualifiedMinCount;
	}

	@Override
	public void toModel(Resource subject, IRI predicate, Model model, Set<Resource> exported) {
		model.add(subject, SHACL.QUALIFIED_VALUE_SHAPE, getId());
		SimpleValueFactory vf = SimpleValueFactory.getInstance();

		if (qualifiedValueShapesDisjoint != null) {
			model.add(subject, SHACL.QUALIFIED_VALUE_SHAPES_DISJOINT, vf.createLiteral(qualifiedValueShapesDisjoint));
		}

		if (qualifiedMinCount != null) {
			model.add(subject, SHACL.QUALIFIED_MIN_COUNT, vf.createLiteral(qualifiedMinCount));
		}

		qualifiedValueShape.toModel(null, null, model, exported);

	}

	@Override
	public void setTargetChain(TargetChain targetChain) {
		super.setTargetChain(targetChain);
		qualifiedValueShape.setTargetChain(targetChain.setOptimizable(false));
	}

	@Override
	public SourceConstraintComponent getConstraintComponent() {
		return SourceConstraintComponent.QualifiedMinCountConstraintComponent;
	}

	@Override
	public PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			boolean negatePlan, boolean negateChildren, Scope scope) {
		assert scope == Scope.propertyShape;
		throw new ShaclUnsupportedException();
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			PlanNodeProvider overrideTargetNode, Scope scope) {
		assert scope == Scope.propertyShape;




		PlanNode planNode = negated(connectionsGroup, logValidationPlans, overrideTargetNode, scope);

		planNode = new DebugPlanNode(planNode, p -> {
			assert p != null;
		});

		PlanNode target = getTargetChain()
				.getEffectiveTarget("_target", scope, connectionsGroup.getRdfsSubClassOfReasoner())
				.getPlanNode(connectionsGroup, scope, false);
		if (overrideTargetNode != null) {
			target = overrideTargetNode.getPlanNode();
		}

		planNode = new LeftOuterJoin(target, planNode);
		planNode = new DebugPlanNode(planNode, p -> {
			assert p != null;
		});

		return new GroupByCountFilter(planNode, count -> count < qualifiedMinCount);

	}

	public PlanNode negated(ConnectionsGroup connectionsGroup, boolean logValidationPlans,
			PlanNodeProvider overrideTargetNode, Scope scope) {

		// if (scope == Scope.nodeShape) {

		PlanNodeProvider planNodeProvider;
		if (overrideTargetNode != null) {
			planNodeProvider = overrideTargetNode;
		} else {
			planNodeProvider = () -> getAllTargetsPlan(connectionsGroup, scope);
		}

		PlanNode planNode = qualifiedValueShape.generateTransactionalValidationPlan(
				connectionsGroup,
				logValidationPlans,
				planNodeProvider,
				scope
		);

		PlanNode invalid = new Unique(planNode);

		PlanNode allTargetsPlan;
		if (overrideTargetNode != null) {
			if (scope == Scope.propertyShape) {
				allTargetsPlan = getTargetChain()
						.getEffectiveTarget("_target", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
						.extend(planNodeProvider.getPlanNode(), connectionsGroup, Scope.nodeShape,
								EffectiveTarget.Extend.right, false);
				allTargetsPlan = new Unique(new ShiftToPropertyShape(allTargetsPlan));
			} else {
				allTargetsPlan = getTargetChain()
						.getEffectiveTarget("_target", scope, connectionsGroup.getRdfsSubClassOfReasoner())
						.extend(planNodeProvider.getPlanNode(), connectionsGroup, scope, EffectiveTarget.Extend.right,
								false);
			}

		} else {
			allTargetsPlan = planNodeProvider.getPlanNode();
		}

		allTargetsPlan = new DebugPlanNode(allTargetsPlan, p -> {
			assert p != null;
		});
		invalid = new NotValuesIn(allTargetsPlan, invalid);

		invalid = new DebugPlanNode(invalid, p -> {
			assert p != null;
		});

		return invalid;

	}

	@Override
	public PlanNode getAllTargetsPlan(ConnectionsGroup connectionsGroup, Scope scope) {
		assert scope == Scope.propertyShape;
		PlanNode allTargets;

		PlanNode allTargetsPlan = getTargetChain()
				.getEffectiveTarget("target_", Scope.nodeShape, connectionsGroup.getRdfsSubClassOfReasoner())
				.getPlanNode(connectionsGroup, Scope.nodeShape, true);

		allTargets = new Unique(new ShiftToPropertyShape(allTargetsPlan));

		PlanNode notTargets = qualifiedValueShape.getAllTargetsPlan(connectionsGroup, scope);

		notTargets = new PropertyShapeScopeWithValueFilter(notTargets).getTrueNode(UnBufferedPlanNode.class);
		allTargets = new PropertyShapeScopeWithValueFilter(allTargets).getTrueNode(UnBufferedPlanNode.class);

		notTargets = new DebugPlanNode(notTargets, p -> {

			assert p != null;
		});
		return new Unique(new UnionNode(allTargets, notTargets));

	}

	@Override
	public ConstraintComponent deepClone() {
		return new QualifiedMinCountConstraintComponent(this);
	}

	@Override
	public boolean requiresEvaluation(ConnectionsGroup connectionsGroup, Scope scope) {
		return true;
	}
}
