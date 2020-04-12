package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.AST.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents.ConstraintComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NodeShape extends Shape implements ConstraintComponent, Identifiable {

	List<ConstraintComponent> constraintComponent = new ArrayList<>();

	public NodeShape() {
	}

	public static NodeShape getInstance(ShaclProperties properties, SailRepositoryConnection connection, Cache cache) {

		Shape shape = cache.get(properties.getId());
		if (shape == null) {
			shape = new NodeShape();
			cache.put(properties.getId(), shape);
			shape.populate(properties, connection, cache);
		}

		return (NodeShape) shape;
	}

	@Override
	public void populate(ShaclProperties properties, SailRepositoryConnection connection, Cache cache) {
		super.populate(properties, connection, cache);

		if (properties.getMinCount() != null)
			throw new IllegalStateException("NodeShapes do not support sh:MinCount in " + getId());
		if (properties.getMaxCount() != null)
			throw new IllegalStateException("NodeShapes do not support sh:MaxCount in " + getId());
		if (properties.isUniqueLang())
			throw new IllegalStateException("NodeShapes do not support sh:uniqueLang in " + getId());
		/*
		 * Also not supported here is: - sh:lessThan - sh:lessThanOrEquals - sh:qualifiedValueShape
		 */

		constraintComponent = getConstraintComponents(properties, connection, cache);

	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {
		super.toModel(subject, model, exported);
		model.add(getId(), RDF.TYPE, SHACL.NODE_SHAPE);

		if (subject != null) {
			model.add(subject, SHACL.NODE, getId());
		}

		if (exported.contains(getId()))
			return;
		exported.add(getId());

		constraintComponent.forEach(c -> c.toModel(getId(), model, exported));

	}
}
