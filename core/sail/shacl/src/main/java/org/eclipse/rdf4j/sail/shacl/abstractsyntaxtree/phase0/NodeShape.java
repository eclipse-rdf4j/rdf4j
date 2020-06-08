package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0;

import java.util.Set;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.AST.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.ConnectionsGroup;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.planNodes.EmptyNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.PlanNode;
import org.eclipse.rdf4j.sail.shacl.planNodes.UnionNode;

public class NodeShape extends Shape implements ConstraintComponent, Identifiable {

	public NodeShape() {
	}

	public NodeShape(NodeShape nodeShape) {
		super(nodeShape);
	}

	public static NodeShape getInstance(ShaclProperties properties,
			RepositoryConnection connection, Cache cache) {

		Shape shape = cache.get(properties.getId());
		if (shape == null) {
			shape = new NodeShape();
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
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		super.toModel(subject, model, exported);
		model.add(getId(), RDF.TYPE, SHACL.NODE_SHAPE);

		if (subject != null) {
			model.add(subject, SHACL.NODE, getId());
		}

		if (exported.contains(getId())) {
			return;
		}
		exported.add(getId());

		constraintComponents.forEach(c -> c.toModel(getId(), model, exported));

	}

	@Override
	public PlanNode generateSparqlValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans) {
		return null;
	}

	@Override
	public PlanNode generateTransactionalValidationPlan(ConnectionsGroup connectionsGroup, boolean logValidationPlans) {

		PlanNode union = new EmptyNode();

		for (ConstraintComponent constraintComponent : constraintComponents) {
			union = new UnionNode(union,
					constraintComponent.generateTransactionalValidationPlan(connectionsGroup, logValidationPlans));
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
}
