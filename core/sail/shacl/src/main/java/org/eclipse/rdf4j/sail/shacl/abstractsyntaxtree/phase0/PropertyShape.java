package org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.shacl.AST.ShaclProperties;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents.ConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.constraintcomponents.MinCountConstraintComponent;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths.Path;
import org.eclipse.rdf4j.sail.shacl.abstractsyntaxtree.phase0.paths.SimplePath;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class PropertyShape extends Shape implements ConstraintComponent, Identifiable {

	List<ConstraintComponent> constraintComponent = new ArrayList<>();

	String name;
	String description;
	Object defaultValue;
	Object group;

	Path path;

	public static PropertyShape getInstance(ShaclProperties properties, SailRepositoryConnection connection,
			Cache cache) {
		Shape shape = cache.get(properties.getId());
		if (shape == null) {
			shape = new PropertyShape();
			cache.put(properties.getId(), shape);
			shape.populate(properties, connection, cache);
		}

		return (PropertyShape) shape;
	}

	@Override
	public void populate(ShaclProperties properties, SailRepositoryConnection connection, Cache cache) {
		super.populate(properties, connection, cache);

		if (properties.getPath() instanceof IRI) {
			this.path = new SimplePath((IRI) properties.getPath());
		} else if (properties.getPath() == null) {
			throw new IllegalStateException(properties.getId() + " is a sh:PropertyShape without a sh:path!");
		} else {
			throw new UnsupportedOperationException(
					"Path is not supported for " + properties.getPath() + " in " + properties.getId());
		}

		properties.getProperty()
				.stream()
				.map(r -> new ShaclProperties(r, connection))
				.map(p -> PropertyShape.getInstance(p, connection, cache))
				.forEach(constraintComponent::add);

		properties.getNode()
				.stream()
				.map(r -> new ShaclProperties(r, connection))
				.map(p -> NodeShape.getInstance(p, connection, cache))
				.forEach(constraintComponent::add);

		if (properties.getMinCount() != null) {
			constraintComponent.add(new MinCountConstraintComponent(properties.getMinCount()));
		}
	}

	@Override
	public void toModel(Resource subject, Model model, Set<Resource> exported) {

		super.toModel(subject, model, exported);
		model.add(getId(), RDF.TYPE, SHACL.PROPERTY_SHAPE);

		if (subject != null) {
			model.add(subject, SHACL.PROPERTY, getId());
		}

		path.toModel(getId(), model, exported);

		if (exported.contains(getId()))
			return;
		exported.add(getId());

		constraintComponent.forEach(c -> c.toModel(getId(), model, exported));

	}
}
